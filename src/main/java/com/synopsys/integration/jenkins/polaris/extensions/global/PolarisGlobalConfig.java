/*
 * synopsys-polaris
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.polaris.extensions.global;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.verb.POST;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.synopsys.integration.jenkins.annotations.HelpMarkdown;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.wrapper.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.wrapper.JenkinsWrapper;
import com.synopsys.integration.jenkins.wrapper.SynopsysCredentialsHelper;
import com.synopsys.integration.log.LogLevel;
import com.synopsys.integration.log.PrintStreamIntLogger;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfig;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfigBuilder;
import com.synopsys.integration.rest.client.ConnectionResult;
import com.synopsys.integration.rest.proxy.ProxyInfo;

import hudson.Extension;
import hudson.Functions;
import hudson.Util;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.IOUtils;
import hudson.util.ListBoxModel;
import hudson.util.Messages;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import jenkins.util.xml.XMLUtils;

@Extension
public class PolarisGlobalConfig extends GlobalConfiguration implements Serializable {
    private static final long serialVersionUID = 1903218683598310994L;

    @HelpMarkdown("Provide the URL that lets you access the Polaris Software Integrity Platform.")
    private String polarisUrl;

    @HelpMarkdown("Choose the Access Token from the list to authenticate to the Polaris Software Integrity Platform.  \r\n"
                      + "If the credentials you are looking for are not in the list then you can add them with the Add button")
    private String polarisCredentialsId;

    private int polarisTimeout = 120;

    @DataBoundConstructor
    public PolarisGlobalConfig() {
        load();
    }

    public String getPolarisUrl() {
        return polarisUrl;
    }

    @DataBoundSetter
    public void setPolarisUrl(String polarisUrl) {
        this.polarisUrl = polarisUrl;
        save();
    }

    public String getPolarisCredentialsId() {
        return polarisCredentialsId;
    }

    @DataBoundSetter
    public void setPolarisCredentialsId(String polarisCredentialsId) {
        this.polarisCredentialsId = polarisCredentialsId;
        save();
    }

    public int getPolarisTimeout() {
        return polarisTimeout;
    }

    @DataBoundSetter
    public void setPolarisTimeout(int polarisTimeout) {
        this.polarisTimeout = polarisTimeout;
        save();
    }

    public PolarisServerConfig getPolarisServerConfig(SynopsysCredentialsHelper credentialsHelper, JenkinsProxyHelper jenkinsProxyHelper) throws IllegalArgumentException {
        return getPolarisServerConfigBuilder(credentialsHelper, jenkinsProxyHelper).build();
    }

    public PolarisServerConfigBuilder getPolarisServerConfigBuilder(SynopsysCredentialsHelper credentialsHelper, JenkinsProxyHelper jenkinsProxyHelper) throws IllegalArgumentException {
        return createPolarisServerConfigBuilder(credentialsHelper, jenkinsProxyHelper, polarisUrl, polarisCredentialsId, polarisTimeout);
    }

    public ListBoxModel doFillPolarisCredentialsIdItems() {
        // We don't use JenkinsWrapper here because it grants us no benefit-- we are guaranteed to be running on Jenkins Master and Jenkins should be started when UI-bound methods are run.
        // -- rotte AUG 2020
        Jenkins jenkins = Jenkins.get();
        jenkins.checkPermission(Jenkins.ADMINISTER);

        return new StandardListBoxModel()
                   .includeEmptyValue()
                   .includeMatchingAs(ACL.SYSTEM, jenkins, BaseStandardCredentials.class, Collections.emptyList(), SynopsysCredentialsHelper.API_TOKEN_CREDENTIALS);
    }

    @POST
    public FormValidation doTestPolarisConnection(@QueryParameter("polarisUrl") String polarisUrl, @QueryParameter("polarisCredentialsId") String polarisCredentialsId,
        @QueryParameter("polarisTimeout") String polarisTimeout) {
        JenkinsWrapper jenkinsWrapper = JenkinsWrapper.initializeFromJenkinsJVM();
        if (!jenkinsWrapper.getJenkins().isPresent()) {
            return FormValidation.warning(
                "Connection validation could not be completed: Validation couldn't retrieve the instance of Jenkins from the JVM. This may happen if Jenkins is still starting up or if this code is running on a different JVM than your Jenkins server.");
        }
        jenkinsWrapper.getJenkins().get().checkPermission(Jenkins.ADMINISTER);

        SynopsysCredentialsHelper synopsysCredentialsHelper = jenkinsWrapper.getCredentialsHelper();
        JenkinsProxyHelper jenkinsProxyHelper = jenkinsWrapper.getProxyHelper();

        try {
            PolarisServerConfig polarisServerConfig = createPolarisServerConfigBuilder(synopsysCredentialsHelper, jenkinsProxyHelper, polarisUrl, polarisCredentialsId, Integer.parseInt(polarisTimeout)).build();
            ConnectionResult connectionResult = polarisServerConfig.createPolarisHttpClient(new PrintStreamIntLogger(System.out, LogLevel.DEBUG)).attemptConnection();
            if (connectionResult.isFailure()) {
                int statusCode = connectionResult.getHttpStatusCode();
                String validationMessage;
                try {
                    String statusPhrase = EnglishReasonPhraseCatalog.INSTANCE.getReason(statusCode, Locale.ENGLISH);
                    validationMessage = String.format("ERROR: Connection attempt returned %s %s", statusCode, statusPhrase);
                } catch (IllegalArgumentException ignored) {
                    // EnglishReasonPhraseCatalog throws an IllegalArgumentException if the status code is outside of the 100-600 range --rotte AUG 2020
                    validationMessage = "ERROR: Connection could not be established.";
                }

                // This is how Jenkins constructs an error with an exception stack trace, we're using it here because often a status code and phrase are not enough, but also (especially with proxies) the failure message can be too much.
                // --rotte AUG 2020
                String moreDetailsHtml = connectionResult.getFailureMessage()
                                             .map(Util::escape)
                                             .map(msg -> String.format("<a href='#' class='showDetails'>%s</a><pre style='display:none'>%s</pre>", Messages.FormValidation_Error_Details(), msg))
                                             .orElse(StringUtils.EMPTY);

                return FormValidation.errorWithMarkup(String.join(" ", validationMessage, moreDetailsHtml));
            }
        } catch (IllegalArgumentException e) {
            return FormValidation.error(e.getMessage());
        }

        return FormValidation.ok("Connection successful.");
    }

    // EX: http://localhost:8080/descriptorByName/com.synopsys.integration.jenkins.polaris.extensions.global.PolarisGlobalConfig/config.xml
    @WebMethod(name = "config.xml")
    public void doConfigDotXml(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, ParserConfigurationException {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        boolean changed = false;
        try {
            if (this.getClass().getClassLoader() != originalClassLoader) {
                changed = true;
                Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            }

            Functions.checkPermission(Jenkins.ADMINISTER);
            if (req.getMethod().equals("GET")) {
                // read
                rsp.setContentType("application/xml");
                IOUtils.copy(getConfigFile().getFile(), rsp.getOutputStream());
                return;
            }
            Functions.checkPermission(Jenkins.ADMINISTER);
            if (req.getMethod().equals("POST")) {
                // submission
                updateByXml(new StreamSource(req.getReader()));
                return;
            }
            // huh?
            rsp.sendError(javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
        } finally {
            if (changed) {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    private void updateByXml(Source source) throws IOException {
        Document doc;
        try (StringWriter writer = new StringWriter()) {
            // this allows us to use UTF-8 for storing data,
            // plus it checks any well-formedness issue in the submitted
            // data
            XMLUtils.safeTransform(source, new StreamResult(writer));
            try (StringReader reader = new StringReader(writer.toString())) {
                doc = XMLUtils.parse(reader);
            }
        } catch (TransformerException | SAXException e) {
            throw new IOException("Failed to persist configuration.xml", e);
        }
        String polarisUrl = getNodeValue(doc, "polarisUrl").orElse(StringUtils.EMPTY);
        String polarisCredentialsId = getNodeValue(doc, "polarisCredentialsId").orElse(StringUtils.EMPTY);
        int polarisTimeout = getNodeIntegerValue(doc, "polarisTimeout").orElse(120);

        setPolarisUrl(polarisUrl);
        setPolarisCredentialsId(polarisCredentialsId);
        setPolarisTimeout(polarisTimeout);
        save();
    }

    private Optional<String> getNodeValue(Document doc, String tagName) {
        return Optional.ofNullable(doc.getElementsByTagName(tagName).item(0))
                   .map(Node::getFirstChild)
                   .map(Node::getNodeValue)
                   .map(String::trim);
    }

    private Optional<Integer> getNodeIntegerValue(Document doc, String tagName) {
        try {
            return getNodeValue(doc, tagName).map(Integer::valueOf);
        } catch (NumberFormatException ignored) {
            JenkinsIntLogger logger = JenkinsIntLogger.logToStandardOut();
            logger.warn("Could not parse node " + tagName + ", provided value is not a valid integer. Using default value.");
            return Optional.empty();
        }
    }

    public PolarisServerConfigBuilder createPolarisServerConfigBuilder(SynopsysCredentialsHelper synopsysCredentialsHelper, JenkinsProxyHelper jenkinsProxyHelper, String polarisUrl, String credentialsId, int timeout) {
        PolarisServerConfigBuilder builder = PolarisServerConfig.newBuilder()
                                                 .setUrl(polarisUrl)
                                                 .setTimeoutInSeconds(timeout);

        synopsysCredentialsHelper.getApiTokenByCredentialsId(credentialsId).ifPresent(builder::setAccessToken);

        ProxyInfo proxyInfo = jenkinsProxyHelper.getProxyInfo(polarisUrl);

        proxyInfo.getHost().ifPresent(builder::setProxyHost);

        if (proxyInfo.getPort() != 0) {
            builder.setProxyPort(proxyInfo.getPort());
        }

        proxyInfo.getUsername().ifPresent(builder::setProxyUsername);
        proxyInfo.getPassword().ifPresent(builder::setProxyPassword);
        proxyInfo.getNtlmDomain().ifPresent(builder::setProxyNtlmDomain);
        proxyInfo.getNtlmWorkstation().ifPresent(builder::setProxyNtlmWorkstation);

        return builder;
    }

}

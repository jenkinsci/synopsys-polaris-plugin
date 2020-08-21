/**
 * synopsys-polaris
 *
 * Copyright (c) 2020 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.jenkins.polaris.extensions.global;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
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
import com.synopsys.integration.builder.Buildable;
import com.synopsys.integration.builder.IntegrationBuilder;
import com.synopsys.integration.jenkins.annotations.HelpMarkdown;
import com.synopsys.integration.jenkins.wrapper.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.wrapper.JenkinsWrapper;
import com.synopsys.integration.jenkins.wrapper.SynopsysCredentialsHelper;
import com.synopsys.integration.log.LogLevel;
import com.synopsys.integration.log.PrintStreamIntLogger;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfig;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfigBuilder;
import com.synopsys.integration.rest.client.AuthenticatingIntHttpClient;
import com.synopsys.integration.rest.client.ConnectionResult;
import com.synopsys.integration.rest.proxy.ProxyInfo;

import hudson.Extension;
import hudson.Functions;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.IOUtils;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import jenkins.util.xml.XMLUtils;

@Extension
public class PolarisGlobalConfig extends GlobalConfiguration implements Serializable {
    private static final long serialVersionUID = 1903218683598310994L;
    private transient final Logger logger = Logger.getLogger(PolarisGlobalConfig.class.getName());

    @HelpMarkdown("Provide the URL that lets you access your Polaris server.")
    private String polarisUrl;

    @HelpMarkdown("Choose the Access Token from the list to authenticate to the Polaris server.\r\n"
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
        JenkinsWrapper jenkinsWrapper = JenkinsWrapper.initializeFromJenkinsJVM();
        if (jenkinsWrapper.getJenkins().isPresent())
            Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        return new StandardListBoxModel()
                   .includeEmptyValue()
                   .includeMatchingAs(ACL.SYSTEM, Jenkins.getInstance(), BaseStandardCredentials.class, Collections.emptyList(), SynopsysCredentialsHelper.API_TOKEN_CREDENTIALS);
    }

    @POST
    public FormValidation doTestPolarisConnection(@QueryParameter("polarisUrl") String polarisUrl, @QueryParameter("polarisCredentialsId") String polarisCredentialsId,
        @QueryParameter("polarisTimeout") String polarisTimeout) {
        JenkinsWrapper jenkinsWrapper = JenkinsWrapper.initializeFromJenkinsJVM();
        SynopsysCredentialsHelper credentialsHelper = new SynopsysCredentialsHelper(jenkinsWrapper);
        JenkinsProxyHelper proxyHelper = JenkinsProxyHelper.fromJenkins(jenkinsWrapper);
        PolarisServerConfigBuilder polarisServerConfigBuilder = createPolarisServerConfigBuilder(credentialsHelper, proxyHelper, polarisUrl, polarisCredentialsId, Integer.parseInt(polarisTimeout));
        return validateConnection(polarisServerConfigBuilder, PolarisServerConfig::createPolarisHttpClient);
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

    private <T extends Buildable> FormValidation validateConnection(IntegrationBuilder<T> configBuilder, BiFunction<T, PrintStreamIntLogger, AuthenticatingIntHttpClient> createHttpClientMethod) {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        try {
            T config = configBuilder.build();
            ConnectionResult connectionResult = createHttpClientMethod.apply(config, new PrintStreamIntLogger(System.out, LogLevel.DEBUG)).attemptConnection();
            return connectionResult.getFailureMessage()
                       .map(FormValidation::error)
                       .orElse(FormValidation.ok("Connection successful"));
        } catch (IllegalArgumentException e) {
            return FormValidation.error(e.getMessage());
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
            logger.log(Level.WARNING, "Could not parse node " + tagName + ", provided value is not a valid integer. Using default value.");
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

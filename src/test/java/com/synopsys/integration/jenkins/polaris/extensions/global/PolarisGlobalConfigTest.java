package com.synopsys.integration.jenkins.polaris.extensions.global;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.compression.FilterServletOutputStream;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.synopsys.integration.jenkins.wrapper.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.wrapper.SynopsysCredentialsHelper;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfig;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfigBuilder;
import com.synopsys.integration.polaris.common.rest.AccessTokenPolarisHttpClient;
import com.synopsys.integration.rest.client.ConnectionResult;
import com.synopsys.integration.rest.proxy.ProxyInfo;

import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

@PowerMockIgnore({ "javax.crypto.*", "javax.net.ssl.*" })
@RunWith(PowerMockRunner.class)
@PrepareForTest({ SynopsysCredentialsHelper.class, PolarisServerConfig.class })
public class PolarisGlobalConfigTest {

    public static final String POLARIS_TOKEN = "testToken";
    private static final String POLARIS_URL = "https://polaris.domain.com";
    private static final String INVALID_URL = "polaris.domain.com";
    private static final String POLARIS_CREDENTIALS_ID = "123";
    private static final String POLARIS_TIMEOUT_STRING = "30";
    private static final int POLARIS_TIMEOUT_INT = 30;
    private static final String CONFIG_XML_CONTENTS = "<?xml version='1.1' encoding='UTF-8'?>\n"
                                                          + "<com.synopsys.integration.jenkins.polaris.extensions.global.PolarisGlobalConfig>\n"
                                                          + "  <polarisUrl>https://dev01.dev.polaris.synopsys.com</polarisUrl>\n"
                                                          + "  <polarisCredentialsId>0424ba25-4607-4a81-a809-0220c44d0fc1</polarisCredentialsId>\n"
                                                          + "  <polarisTimeout>120</polarisTimeout>\n"
                                                          + "</com.synopsys.integration.jenkins.polaris.extensions.global.PolarisGlobalConfig>";
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void testInvalidUrl() {

        // This config should be found to be invalid (invalid URL)
        PolarisGlobalConfig detectGlobalConfig = new PolarisGlobalConfig();
        FormValidation formValidation = detectGlobalConfig.doTestPolarisConnection(INVALID_URL, POLARIS_CREDENTIALS_ID, POLARIS_TIMEOUT_STRING);

        assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
        assertTrue(formValidation.getMessage().contains("valid"));
        assertTrue(formValidation.getMessage().contains("URL"));
    }

    @Test
    public void testInvalidCredentialsId() {

        // This config should be found to be invalid (invalid credentials ID)
        PolarisGlobalConfig detectGlobalConfig = new PolarisGlobalConfig();
        FormValidation formValidation = detectGlobalConfig.doTestPolarisConnection(POLARIS_URL, POLARIS_CREDENTIALS_ID, POLARIS_TIMEOUT_STRING);

        assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
        assertTrue(formValidation.getMessage().contains("token"));
    }

    @Test
    public void testValidConfig() {
        PolarisServerConfigBuilder mockedServerConfigBuilder = Mockito.mock(PolarisServerConfigBuilder.class);
        Mockito.when(mockedServerConfigBuilder.setUrl(POLARIS_URL)).thenReturn(mockedServerConfigBuilder);
        Mockito.when(mockedServerConfigBuilder.setTimeoutInSeconds(POLARIS_TIMEOUT_INT)).thenReturn(mockedServerConfigBuilder);
        PolarisServerConfig mockedServerConfig = Mockito.mock(PolarisServerConfig.class);
        Mockito.when(mockedServerConfigBuilder.build()).thenReturn(mockedServerConfig);
        AccessTokenPolarisHttpClient mockedHttpClient = Mockito.mock(AccessTokenPolarisHttpClient.class);
        Mockito.when(mockedServerConfig.createPolarisHttpClient(Mockito.any(IntLogger.class))).thenReturn(mockedHttpClient);
        Mockito.when(mockedHttpClient.attemptConnection()).thenReturn(ConnectionResult.SUCCESS(200));

        // This config should be found to be valid
        PolarisGlobalConfig polarisGlobalConfig = Mockito.mock(PolarisGlobalConfig.class);
        Mockito.when(polarisGlobalConfig.createPolarisServerConfigBuilder(Mockito.any(), Mockito.any(), Mockito.eq(POLARIS_URL), Mockito.eq(POLARIS_CREDENTIALS_ID), Mockito.eq(POLARIS_TIMEOUT_INT))).thenReturn(mockedServerConfigBuilder);
        Mockito.when(polarisGlobalConfig.doTestPolarisConnection(POLARIS_URL, POLARIS_CREDENTIALS_ID, POLARIS_TIMEOUT_STRING)).thenCallRealMethod();

        FormValidation formValidation = polarisGlobalConfig.doTestPolarisConnection(POLARIS_URL, POLARIS_CREDENTIALS_ID, POLARIS_TIMEOUT_STRING);

        assertEquals(FormValidation.Kind.OK, formValidation.kind);
    }

    @Test
    public void testCreatePolarisServerConfigBuilder() {
        SynopsysCredentialsHelper synopsysCredentialsHelper = Mockito.mock(SynopsysCredentialsHelper.class);
        Mockito.when(synopsysCredentialsHelper.getApiTokenByCredentialsId(POLARIS_CREDENTIALS_ID)).thenReturn(Optional.of(POLARIS_TOKEN));

        JenkinsProxyHelper jenkinsProxyHelper = Mockito.mock(JenkinsProxyHelper.class);
        Mockito.when(jenkinsProxyHelper.getProxyInfo(POLARIS_URL)).thenReturn(ProxyInfo.NO_PROXY_INFO);

        PolarisGlobalConfig polarisGlobalConfig = new PolarisGlobalConfig();
        PolarisServerConfigBuilder polarisServerConfigBuilder = polarisGlobalConfig.createPolarisServerConfigBuilder(synopsysCredentialsHelper, jenkinsProxyHelper, POLARIS_URL, POLARIS_CREDENTIALS_ID, POLARIS_TIMEOUT_INT);

        assertEquals(POLARIS_URL, polarisServerConfigBuilder.getUrl());
        assertEquals(POLARIS_TOKEN, polarisServerConfigBuilder.getAccessToken());
        assertEquals(POLARIS_TIMEOUT_INT, polarisServerConfigBuilder.getTimeoutInSeconds());
    }

    @Test
    public void testConfigDotXmlGet() throws ServletException, ParserConfigurationException, IOException {
        PolarisGlobalConfig detectGlobalConfig = new PolarisGlobalConfig();
        StaplerRequest req = Mockito.mock(StaplerRequest.class);
        StaplerResponse rsp = Mockito.mock(StaplerResponse.class);
        Mockito.when(req.getMethod()).thenReturn("GET");

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ServletOutputStream servletOutputStream = new FilterServletOutputStream(byteArrayOutputStream, Mockito.mock(ServletOutputStream.class));
        Mockito.when(rsp.getOutputStream()).thenReturn(servletOutputStream);

        File pluginConfigFile = new File(Jenkins.getInstance().getRootDir(), PolarisGlobalConfig.class.getName() + ".xml");
        FileUtils.write(pluginConfigFile, CONFIG_XML_CONTENTS, StandardCharsets.UTF_8);

        // The config file created above should be written to rsp output stream (byteArrayOutputStream)
        detectGlobalConfig.doConfigDotXml(req, rsp);

        assertEquals(CONFIG_XML_CONTENTS, byteArrayOutputStream.toString());
    }

    @Test
    public void testConfigDotXmlPost() throws ServletException, ParserConfigurationException, IOException {
        PolarisGlobalConfig detectGlobalConfig = new PolarisGlobalConfig();
        StaplerRequest req = Mockito.mock(StaplerRequest.class);
        StaplerResponse rsp = Mockito.mock(StaplerResponse.class);
        Mockito.when(req.getMethod()).thenReturn("POST");

        BufferedReader reader = new BufferedReader(new StringReader(CONFIG_XML_CONTENTS));
        Mockito.when(req.getReader()).thenReturn(reader);

        File pluginConfigFile = new File(Jenkins.getInstance().getRootDir(), PolarisGlobalConfig.class.getName() + ".xml");
        assertFalse(pluginConfigFile.exists());

        // the XML read from the request should get saved to the plugin config file
        detectGlobalConfig.doConfigDotXml(req, rsp);

        assertTrue(pluginConfigFile.exists());
        String pluginConfigFileContents = FileUtils.readFileToString(pluginConfigFile, StandardCharsets.UTF_8);
        assertEquals(CONFIG_XML_CONTENTS, pluginConfigFileContents);
    }

    @Test
    public void testDoFillPolarisCredentialsIdItems() {
        PolarisGlobalConfig detectGlobalConfig = new PolarisGlobalConfig();
        ListBoxModel listBoxModel = detectGlobalConfig.doFillPolarisCredentialsIdItems();
        assertEquals("- none -", listBoxModel.get(0).name);
        assertEquals("", listBoxModel.get(0).value);
        assertEquals(false, listBoxModel.get(0).selected);
    }
}

package com.synopsys.integration.jenkins.polaris;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.polaris.extensions.global.PolarisGlobalConfig;
import com.synopsys.integration.jenkins.polaris.extensions.tools.PolarisCli;
import com.synopsys.integration.jenkins.polaris.service.GetPathToPolarisCli;
import com.synopsys.integration.jenkins.polaris.service.PolarisCliArgumentService;
import com.synopsys.integration.jenkins.polaris.service.PolarisEnvironmentService;
import com.synopsys.integration.jenkins.polaris.service.PolarisPhoneHomeService;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.jenkins.wrapper.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.wrapper.JenkinsVersionHelper;
import com.synopsys.integration.jenkins.wrapper.SynopsysCredentialsHelper;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfigBuilder;
import com.synopsys.integration.polaris.common.exception.PolarisIntegrationException;

public class PolarisCliRunnerTest {
    public static final String SUCCESSFUL_CLI_NAME = "SuccessfulPolarisCLi";
    public static final String SUCCESSFUL_CLI_HOME = "/path/to/polaris/cli/home/";
    public static final String NULL_HOME_CLI_NAME = "PolarisCLiWithNullHome";
    public static final String EMPTY_HOME_CLI_NAME = "PolarisCLiWithEmptyHome";
    public static final String NONEXISTANT_CLI_NAME = "NonexistantPolarisCli";

    public static final String CHANGE_SET_FILE_PATH = "/path/to/changeSetFile.txt";
    public static final String POLARIS_ARGUMENTS = "analyze -w --incremental $CHANGE_SET_FILE_PATH";

    private IntLogger logger;
    private PolarisEnvironmentService polarisEnvironmentService;
    private PolarisCliArgumentService polarisCliArgumentService;
    private PolarisPhoneHomeService polarisPhoneHomeService;
    private JenkinsRemotingService jenkinsRemotingService;
    private JenkinsConfigService jenkinsConfigService;
    private SynopsysCredentialsHelper synopsysCredentialsHelper;
    private JenkinsProxyHelper jenkinsProxyHelper;
    private JenkinsVersionHelper jenkinsVersionHelper;

    @BeforeEach
    public void setUpMocks() {
        try {
            logger = JenkinsIntLogger.logToStandardOut();
            polarisEnvironmentService = new PolarisEnvironmentService(new HashMap<>());
            polarisCliArgumentService = new PolarisCliArgumentService(logger);
            polarisPhoneHomeService = Mockito.mock(PolarisPhoneHomeService.class);
            jenkinsRemotingService = Mockito.mock(JenkinsRemotingService.class);
            Mockito.when(jenkinsRemotingService.tokenizeArgumentString(POLARIS_ARGUMENTS)).thenCallRealMethod();
            Mockito.when(jenkinsRemotingService.resolveEnvironmentVariables(Mockito.any(), Mockito.any())).thenCallRealMethod();
            Mockito.when(jenkinsRemotingService.call(Mockito.any(GetPathToPolarisCli.class))).thenReturn("polaris");
            synopsysCredentialsHelper = Mockito.mock(SynopsysCredentialsHelper.class);
            jenkinsProxyHelper = Mockito.mock(JenkinsProxyHelper.class);
            jenkinsVersionHelper = Mockito.mock(JenkinsVersionHelper.class);

            jenkinsConfigService = Mockito.mock(JenkinsConfigService.class);
            PolarisCli successfulPolarisCli = new PolarisCli(SUCCESSFUL_CLI_NAME, SUCCESSFUL_CLI_HOME, Collections.emptyList());
            PolarisCli nullHomePolarisCli = new PolarisCli(NULL_HOME_CLI_NAME, null, Collections.emptyList());
            PolarisCli emptyHomePolarisCli = new PolarisCli(EMPTY_HOME_CLI_NAME, StringUtils.EMPTY, Collections.emptyList());
            Mockito.when(jenkinsConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, SUCCESSFUL_CLI_NAME)).thenReturn(Optional.of(successfulPolarisCli));
            Mockito.when(jenkinsConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, NULL_HOME_CLI_NAME)).thenReturn(Optional.of(nullHomePolarisCli));
            Mockito.when(jenkinsConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, EMPTY_HOME_CLI_NAME)).thenReturn(Optional.of(emptyHomePolarisCli));

            PolarisGlobalConfig polarisGlobalConfig = Mockito.mock(PolarisGlobalConfig.class);
            PolarisServerConfigBuilder polarisServerConfigBuilder = new PolarisServerConfigBuilder()
                                                                        .setTimeoutInSeconds(120)
                                                                        .setAccessToken("ACCESS-TOKEN")
                                                                        .setUrl("http://example.com/polaris");
            Mockito.when(polarisGlobalConfig.getPolarisServerConfigBuilder(synopsysCredentialsHelper, jenkinsProxyHelper)).thenReturn(polarisServerConfigBuilder);
            Mockito.when(jenkinsConfigService.getGlobalConfiguration(PolarisGlobalConfig.class)).thenReturn(Optional.of(polarisGlobalConfig));
        } catch (Exception e) {
            fail("Unexpected exception occurred when setting up the test code, this test likely needs to be fixed.", e);
        }
    }

    @Test
    public void testRunPolarisCli() {
        PolarisCliRunner polarisCliRunner = new PolarisCliRunner(logger, polarisCliArgumentService, polarisEnvironmentService, polarisPhoneHomeService, jenkinsRemotingService, jenkinsConfigService, synopsysCredentialsHelper,
            jenkinsProxyHelper, jenkinsVersionHelper);

        try {
            polarisCliRunner.runPolarisCli(SUCCESSFUL_CLI_NAME, CHANGE_SET_FILE_PATH, POLARIS_ARGUMENTS);
        } catch (Exception e) {
            fail("Unexpected exception occurred in the test code.", e);
        }
    }

    @Test
    public void testRunPolarisCliEmptyHome() {
        PolarisCliRunner polarisCliRunner = new PolarisCliRunner(logger, polarisCliArgumentService, polarisEnvironmentService, polarisPhoneHomeService, jenkinsRemotingService, jenkinsConfigService, synopsysCredentialsHelper,
            jenkinsProxyHelper, jenkinsVersionHelper);

        assertThrows(PolarisIntegrationException.class, () -> polarisCliRunner.runPolarisCli(EMPTY_HOME_CLI_NAME, CHANGE_SET_FILE_PATH, POLARIS_ARGUMENTS));
    }

    @Test
    public void testRunPolarisCliNullHome() {
        PolarisCliRunner polarisCliRunner = new PolarisCliRunner(logger, polarisCliArgumentService, polarisEnvironmentService, polarisPhoneHomeService, jenkinsRemotingService, jenkinsConfigService, synopsysCredentialsHelper,
            jenkinsProxyHelper, jenkinsVersionHelper);

        assertThrows(PolarisIntegrationException.class, () -> polarisCliRunner.runPolarisCli(NULL_HOME_CLI_NAME, CHANGE_SET_FILE_PATH, POLARIS_ARGUMENTS));
    }

    @Test
    public void testRunPolarisCliNoCliWithName() {
        PolarisCliRunner polarisCliRunner = new PolarisCliRunner(logger, polarisCliArgumentService, polarisEnvironmentService, polarisPhoneHomeService, jenkinsRemotingService, jenkinsConfigService, synopsysCredentialsHelper,
            jenkinsProxyHelper, jenkinsVersionHelper);

        assertThrows(PolarisIntegrationException.class, () -> polarisCliRunner.runPolarisCli(NONEXISTANT_CLI_NAME, CHANGE_SET_FILE_PATH, POLARIS_ARGUMENTS));
    }

}

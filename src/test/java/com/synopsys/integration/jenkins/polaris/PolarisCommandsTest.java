package com.synopsys.integration.jenkins.polaris;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.extensions.ChangeBuildStatusTo;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.polaris.extensions.freestyle.WaitForIssues;
import com.synopsys.integration.jenkins.polaris.extensions.tools.PolarisCli;
import com.synopsys.integration.jenkins.polaris.service.PolarisCliArgumentService;
import com.synopsys.integration.jenkins.polaris.service.PolarisCliIssueCountService;
import com.synopsys.integration.jenkins.polaris.service.PolarisEnvironmentService;
import com.synopsys.integration.jenkins.polaris.service.PolarisJenkinsServicesFactory;
import com.synopsys.integration.jenkins.polaris.service.PolarisPhoneHomeService;
import com.synopsys.integration.jenkins.service.JenkinsBuildService;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.jenkins.service.JenkinsServicesFactory;
import com.synopsys.integration.polaris.common.exception.PolarisIntegrationException;

public class PolarisCommandsTest {
    private static final String POLARIS_CLI_NAME = "polarisCliName";
    private static final String POLARIS_ARGUMENTS = "polarisArguments";
    private static final int STATUS_CODE_SUCCESS = 0;
    private static final int STATUS_CODE_FAILURE = 1;
    private static final int NO_ISSUES = 0;
    private static final int SOME_ISSUES = 1;

    private PolarisJenkinsServicesFactory mockedPolarisJenkinsServicesFactory;
    private JenkinsBuildService mockedBuildService;
    private JenkinsRemotingService mockedRemotingService;
    private JenkinsConfigService mockedConfigService;
    private PolarisCliIssueCountService mockedCliIssueCountService;
    private WaitForIssues mockedWaitForIssues;
    private PolarisCli mockedPolarisCli;

    @BeforeEach
    public void setUpMocks() {
        try {
            mockedPolarisCli = Mockito.mock(PolarisCli.class);
            mockedConfigService = Mockito.mock(JenkinsConfigService.class);

            mockedBuildService = Mockito.mock(JenkinsBuildService.class);
            mockedRemotingService = Mockito.mock(JenkinsRemotingService.class);
            JenkinsServicesFactory mockedJenkinsServicesFactory = Mockito.mock(JenkinsServicesFactory.class);
            Mockito.when(mockedJenkinsServicesFactory.createJenkinsBuildService()).thenReturn(mockedBuildService);
            Mockito.when(mockedJenkinsServicesFactory.createJenkinsRemotingService()).thenReturn(mockedRemotingService);
            Mockito.when(mockedJenkinsServicesFactory.createJenkinsConfigService()).thenReturn(mockedConfigService);

            JenkinsIntLogger mockedLogger = Mockito.mock(JenkinsIntLogger.class);
            PolarisCliArgumentService mockedPolarisCliArgumentService = Mockito.mock(PolarisCliArgumentService.class);
            PolarisEnvironmentService mockedPolarisEnvironmentService = Mockito.mock(PolarisEnvironmentService.class);
            PolarisPhoneHomeService mockedPolarisPhoneHomeService = Mockito.mock(PolarisPhoneHomeService.class);
            mockedCliIssueCountService = Mockito.mock(PolarisCliIssueCountService.class);
            mockedPolarisJenkinsServicesFactory = Mockito.mock(PolarisJenkinsServicesFactory.class);
            Mockito.when(mockedPolarisJenkinsServicesFactory.createJenkinsServiceFactory()).thenReturn(mockedJenkinsServicesFactory);
            Mockito.when(mockedPolarisJenkinsServicesFactory.createPolarisCliArgumentService()).thenReturn(mockedPolarisCliArgumentService);
            Mockito.when(mockedPolarisJenkinsServicesFactory.createPolarisEnvironmentService()).thenReturn(mockedPolarisEnvironmentService);
            Mockito.when(mockedPolarisJenkinsServicesFactory.createPolarisPhoneHomeService()).thenReturn(mockedPolarisPhoneHomeService);
            Mockito.when(mockedPolarisJenkinsServicesFactory.createPolarisCliIssueCountService()).thenReturn(mockedCliIssueCountService);
            Mockito.when(mockedPolarisJenkinsServicesFactory.getOrCreateLogger()).thenReturn(mockedLogger);

            mockedWaitForIssues = Mockito.mock(WaitForIssues.class);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }
    }

    @Test
    public void testPreserveNullTimeout() throws Throwable {
        Mockito.when(mockedConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, POLARIS_CLI_NAME)).thenReturn(Optional.of(mockedPolarisCli));

        WaitForIssues waitForIssues = new WaitForIssues();
        waitForIssues.setBuildStatusForIssues(ChangeBuildStatusTo.SUCCESS);
        waitForIssues.setJobTimeoutInMinutes(null);
        PolarisCommands spiedPolarisCommands = Mockito.spy(new PolarisCommands(mockedPolarisJenkinsServicesFactory));
        spiedPolarisCommands.executePolarisCliFreestyle(POLARIS_CLI_NAME, "polarisArguments", waitForIssues);

        Mockito.verify(spiedPolarisCommands).getPolarisIssueCount(null);
    }

    @Test
    public void testExecutePolarisCliFreestyleSuccess() {
        PolarisCommands polarisCommands = new PolarisCommands(mockedPolarisJenkinsServicesFactory);

        try {
            Mockito.when(mockedConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, POLARIS_CLI_NAME)).thenReturn(Optional.of(mockedPolarisCli));
            Mockito.when(mockedRemotingService.launch(Mockito.any(), Mockito.any())).thenReturn(STATUS_CODE_SUCCESS);
            Mockito.when(mockedCliIssueCountService.getIssueCount(Mockito.anyLong(), Mockito.any())).thenReturn(NO_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        polarisCommands.executePolarisCliFreestyle(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, mockedWaitForIssues);

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(String.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAs(Mockito.any(ChangeBuildStatusTo.class));
    }

    @Test
    public void testExecutePolarisCliFreestyleFailureCli() {
        PolarisCommands polarisCommands = new PolarisCommands(mockedPolarisJenkinsServicesFactory);

        try {
            Mockito.when(mockedConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, POLARIS_CLI_NAME)).thenReturn(Optional.of(mockedPolarisCli));
            Mockito.when(mockedRemotingService.launch(Mockito.any(), Mockito.any())).thenReturn(STATUS_CODE_FAILURE);
            Mockito.when(mockedCliIssueCountService.getIssueCount(Mockito.anyLong(), Mockito.any())).thenReturn(NO_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        polarisCommands.executePolarisCliFreestyle(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, mockedWaitForIssues);

        Mockito.verify(mockedBuildService).markBuildFailed(Mockito.anyString());

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAs(Mockito.any(ChangeBuildStatusTo.class));
    }

    @Test
    public void testExecutePolarisCliFreestyleFailureIssues() {
        PolarisCommands polarisCommands = new PolarisCommands(mockedPolarisJenkinsServicesFactory);

        try {
            Mockito.when(mockedConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, POLARIS_CLI_NAME)).thenReturn(Optional.of(mockedPolarisCli));
            Mockito.when(mockedRemotingService.launch(Mockito.any(), Mockito.any())).thenReturn(STATUS_CODE_SUCCESS);
            Mockito.when(mockedCliIssueCountService.getIssueCount(Mockito.anyLong(), Mockito.any())).thenReturn(SOME_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        polarisCommands.executePolarisCliFreestyle(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, mockedWaitForIssues);

        Mockito.verify(mockedBuildService).markBuildAs(Mockito.any(ChangeBuildStatusTo.class));

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.anyString());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
    }

    @Test
    public void testExecutePolarisCliFreestyleInterrupted() {
        PolarisCommands polarisCommands = new PolarisCommands(mockedPolarisJenkinsServicesFactory);

        try {
            Mockito.when(mockedConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, POLARIS_CLI_NAME)).thenReturn(Optional.of(mockedPolarisCli));
            Mockito.doThrow(new InterruptedException()).when(mockedRemotingService).launch(Mockito.any(), Mockito.any());
            Mockito.when(mockedCliIssueCountService.getIssueCount(Mockito.anyLong(), Mockito.any())).thenReturn(NO_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        polarisCommands.executePolarisCliFreestyle(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, mockedWaitForIssues);

        Mockito.verify(mockedBuildService).markBuildInterrupted();

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.anyString());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAs(Mockito.any(ChangeBuildStatusTo.class));
    }

    @Test
    public void testExecutePolarisCliFreestyleUnexpectedException() {
        PolarisCommands polarisCommands = new PolarisCommands(mockedPolarisJenkinsServicesFactory);

        try {
            Mockito.when(mockedConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, POLARIS_CLI_NAME)).thenReturn(Optional.of(mockedPolarisCli));
            Mockito.doThrow(new IOException()).when(mockedRemotingService).launch(Mockito.any(), Mockito.any());
            Mockito.when(mockedCliIssueCountService.getIssueCount(Mockito.anyLong(), Mockito.any())).thenReturn(NO_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        polarisCommands.executePolarisCliFreestyle(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, mockedWaitForIssues);

        Mockito.verify(mockedBuildService).markBuildUnstable(Mockito.any(IOException.class));

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.anyString());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAs(Mockito.any(ChangeBuildStatusTo.class));
    }

    @Test
    public void testExecutePolarisCliFreestyleIntegrationException() {
        PolarisCommands polarisCommands = new PolarisCommands(mockedPolarisJenkinsServicesFactory);

        try {
            Mockito.when(mockedConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, POLARIS_CLI_NAME)).thenReturn(Optional.empty());
            Mockito.when(mockedRemotingService.launch(Mockito.any(), Mockito.any())).thenReturn(STATUS_CODE_SUCCESS);
            Mockito.when(mockedCliIssueCountService.getIssueCount(Mockito.anyLong(), Mockito.any())).thenReturn(NO_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        polarisCommands.executePolarisCliFreestyle(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, mockedWaitForIssues);

        Mockito.verify(mockedBuildService).markBuildFailed(Mockito.any(IntegrationException.class));

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.anyString());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAs(Mockito.any(ChangeBuildStatusTo.class));
    }

    @Test
    public void testExecutePolarisCliPipelineSuccess() {
        PolarisCommands polarisCommands = new PolarisCommands(mockedPolarisJenkinsServicesFactory);

        try {
            Mockito.when(mockedConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, POLARIS_CLI_NAME)).thenReturn(Optional.of(mockedPolarisCli));
            Mockito.when(mockedRemotingService.launch(Mockito.any(), Mockito.any())).thenReturn(STATUS_CODE_SUCCESS);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        try {
            int actualExitCode = polarisCommands.executePolarisCliPipeline(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, true);

            assertEquals(STATUS_CODE_SUCCESS, actualExitCode);
        } catch (Exception e) {
            fail("An unexpected exception occurred.", e);
        }
    }

    @Test
    public void testExecutePolarisCliPipelineException() {
        PolarisCommands polarisCommands = new PolarisCommands(mockedPolarisJenkinsServicesFactory);

        try {
            Mockito.when(mockedConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, POLARIS_CLI_NAME)).thenReturn(Optional.of(mockedPolarisCli));
            Mockito.doThrow(new IOException()).when(mockedRemotingService).launch(Mockito.any(), Mockito.any());
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        assertThrows(IOException.class, () -> polarisCommands.executePolarisCliPipeline(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, true));
    }

    @Test
    public void testExecutePolarisCliPipelineFailureReturnStatus() {
        PolarisCommands polarisCommands = new PolarisCommands(mockedPolarisJenkinsServicesFactory);

        try {
            Mockito.when(mockedConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, POLARIS_CLI_NAME)).thenReturn(Optional.of(mockedPolarisCli));
            Mockito.when(mockedRemotingService.launch(Mockito.any(), Mockito.any())).thenReturn(STATUS_CODE_FAILURE);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        try {
            int actualExitCode = polarisCommands.executePolarisCliPipeline(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, true);

            assertEquals(STATUS_CODE_FAILURE, actualExitCode);
        } catch (Exception e) {
            fail("An unexpected exception occurred.", e);
        }
    }

    @Test
    public void testExecutePolarisCliPipelineFailureDoNotReturnStatus() {
        PolarisCommands polarisCommands = new PolarisCommands(mockedPolarisJenkinsServicesFactory);

        try {
            Mockito.when(mockedConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, POLARIS_CLI_NAME)).thenReturn(Optional.of(mockedPolarisCli));
            Mockito.when(mockedRemotingService.launch(Mockito.any(), Mockito.any())).thenReturn(STATUS_CODE_FAILURE);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        assertThrows(PolarisIntegrationException.class, () -> polarisCommands.executePolarisCliPipeline(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, false));
    }

    @Test
    public void testCheckForPolarisIssuesSuccess() {
        PolarisCommands polarisCommands = new PolarisCommands(mockedPolarisJenkinsServicesFactory);

        try {
            Mockito.when(mockedCliIssueCountService.getIssueCount(Mockito.anyLong(), Mockito.any())).thenReturn(NO_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        try {
            int actualIssueCount = polarisCommands.checkForPolarisIssues(0, true);

            assertEquals(NO_ISSUES, actualIssueCount);
        } catch (Exception e) {
            fail("An unexpected exception occurred.", e);
        }
    }

    @Test
    public void testCheckForPolarisIssuesFailureReturnIssues() {
        PolarisCommands polarisCommands = new PolarisCommands(mockedPolarisJenkinsServicesFactory);

        try {
            Mockito.when(mockedCliIssueCountService.getIssueCount(Mockito.anyLong(), Mockito.any())).thenReturn(SOME_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        try {
            int actualIssueCount = polarisCommands.checkForPolarisIssues(0, true);

            assertEquals(SOME_ISSUES, actualIssueCount);
        } catch (Exception e) {
            fail("An unexpected exception occurred.", e);
        }
    }

    @Test
    public void testCheckForPolarisIssuesFailureDoNotReturnIssues() {
        PolarisCommands polarisCommands = new PolarisCommands(mockedPolarisJenkinsServicesFactory);

        try {
            Mockito.when(mockedCliIssueCountService.getIssueCount(Mockito.anyLong(), Mockito.any())).thenReturn(SOME_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        assertThrows(PolarisIntegrationException.class, () -> polarisCommands.checkForPolarisIssues(0, false));
    }

}

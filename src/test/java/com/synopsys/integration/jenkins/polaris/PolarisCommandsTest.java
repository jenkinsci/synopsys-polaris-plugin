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
import com.synopsys.integration.jenkins.polaris.service.PolarisCommandsFactory;
import com.synopsys.integration.jenkins.polaris.service.PolarisEnvironmentService;
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

    private PolarisCommandsFactory mockedPolarisCommandsFactory;
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
            mockedPolarisCommandsFactory = Mockito.mock(PolarisCommandsFactory.class);
            Mockito.when(mockedPolarisCommandsFactory.createJenkinsServiceFactory()).thenReturn(mockedJenkinsServicesFactory);
            Mockito.when(mockedPolarisCommandsFactory.createPolarisCliArgumentService()).thenReturn(mockedPolarisCliArgumentService);
            Mockito.when(mockedPolarisCommandsFactory.createPolarisEnvironmentService()).thenReturn(mockedPolarisEnvironmentService);
            Mockito.when(mockedPolarisCommandsFactory.createPolarisPhoneHomeService()).thenReturn(mockedPolarisPhoneHomeService);
            Mockito.when(mockedPolarisCommandsFactory.createPolarisCliIssueCountService()).thenReturn(mockedCliIssueCountService);
            Mockito.when(mockedPolarisCommandsFactory.getOrCreateLogger()).thenReturn(mockedLogger);

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
        PolarisFreestyleCommands spiedPolarisCommands = Mockito.spy(new PolarisFreestyleCommands(mockedPolarisCommandsFactory));
        spiedPolarisCommands.runPolarisCli(POLARIS_CLI_NAME, "polarisArguments", waitForIssues);

        Mockito.verify(spiedPolarisCommands).getPolarisIssueCount(null);
    }

    @Test
    public void testExecutePolarisCliFreestyleSuccess() {
        PolarisFreestyleCommands polarisCommands = new PolarisFreestyleCommands(mockedPolarisCommandsFactory);

        try {
            Mockito.when(mockedConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, POLARIS_CLI_NAME)).thenReturn(Optional.of(mockedPolarisCli));
            Mockito.when(mockedRemotingService.launch(Mockito.any(), Mockito.any())).thenReturn(STATUS_CODE_SUCCESS);
            Mockito.when(mockedCliIssueCountService.getIssueCount(Mockito.anyLong(), Mockito.any())).thenReturn(NO_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        polarisCommands.runPolarisCli(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, mockedWaitForIssues);

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(String.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAs(Mockito.any(ChangeBuildStatusTo.class));
    }

    @Test
    public void testExecutePolarisCliFreestyleFailureCli() {
        PolarisFreestyleCommands polarisCommands = new PolarisFreestyleCommands(mockedPolarisCommandsFactory);

        try {
            Mockito.when(mockedConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, POLARIS_CLI_NAME)).thenReturn(Optional.of(mockedPolarisCli));
            Mockito.when(mockedRemotingService.launch(Mockito.any(), Mockito.any())).thenReturn(STATUS_CODE_FAILURE);
            Mockito.when(mockedCliIssueCountService.getIssueCount(Mockito.anyLong(), Mockito.any())).thenReturn(NO_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        polarisCommands.runPolarisCli(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, mockedWaitForIssues);

        Mockito.verify(mockedBuildService).markBuildFailed(Mockito.anyString());

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAs(Mockito.any(ChangeBuildStatusTo.class));
    }

    @Test
    public void testExecutePolarisCliFreestyleFailureIssues() {
        PolarisFreestyleCommands polarisCommands = new PolarisFreestyleCommands(mockedPolarisCommandsFactory);

        try {
            Mockito.when(mockedConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, POLARIS_CLI_NAME)).thenReturn(Optional.of(mockedPolarisCli));
            Mockito.when(mockedRemotingService.launch(Mockito.any(), Mockito.any())).thenReturn(STATUS_CODE_SUCCESS);
            Mockito.when(mockedCliIssueCountService.getIssueCount(Mockito.anyLong(), Mockito.any())).thenReturn(SOME_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        polarisCommands.runPolarisCli(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, mockedWaitForIssues);

        Mockito.verify(mockedBuildService).markBuildAs(Mockito.any(ChangeBuildStatusTo.class));

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.anyString());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
    }

    @Test
    public void testExecutePolarisCliFreestyleInterrupted() {
        PolarisFreestyleCommands polarisCommands = new PolarisFreestyleCommands(mockedPolarisCommandsFactory);

        try {
            Mockito.when(mockedConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, POLARIS_CLI_NAME)).thenReturn(Optional.of(mockedPolarisCli));
            Mockito.doThrow(new InterruptedException()).when(mockedRemotingService).launch(Mockito.any(), Mockito.any());
            Mockito.when(mockedCliIssueCountService.getIssueCount(Mockito.anyLong(), Mockito.any())).thenReturn(NO_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        polarisCommands.runPolarisCli(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, mockedWaitForIssues);

        Mockito.verify(mockedBuildService).markBuildInterrupted();

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.anyString());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAs(Mockito.any(ChangeBuildStatusTo.class));
    }

    @Test
    public void testExecutePolarisCliFreestyleUnexpectedException() {
        PolarisFreestyleCommands polarisCommands = new PolarisFreestyleCommands(mockedPolarisCommandsFactory);

        try {
            Mockito.when(mockedConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, POLARIS_CLI_NAME)).thenReturn(Optional.of(mockedPolarisCli));
            Mockito.doThrow(new IOException()).when(mockedRemotingService).launch(Mockito.any(), Mockito.any());
            Mockito.when(mockedCliIssueCountService.getIssueCount(Mockito.anyLong(), Mockito.any())).thenReturn(NO_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        polarisCommands.runPolarisCli(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, mockedWaitForIssues);

        Mockito.verify(mockedBuildService).markBuildUnstable(Mockito.any(IOException.class));

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.anyString());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAs(Mockito.any(ChangeBuildStatusTo.class));
    }

    @Test
    public void testExecutePolarisCliFreestyleIntegrationException() {
        PolarisFreestyleCommands polarisCommands = new PolarisFreestyleCommands(mockedPolarisCommandsFactory);

        try {
            Mockito.when(mockedConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, POLARIS_CLI_NAME)).thenReturn(Optional.empty());
            Mockito.when(mockedRemotingService.launch(Mockito.any(), Mockito.any())).thenReturn(STATUS_CODE_SUCCESS);
            Mockito.when(mockedCliIssueCountService.getIssueCount(Mockito.anyLong(), Mockito.any())).thenReturn(NO_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        polarisCommands.runPolarisCli(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, mockedWaitForIssues);

        Mockito.verify(mockedBuildService).markBuildFailed(Mockito.any(IntegrationException.class));

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.anyString());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAs(Mockito.any(ChangeBuildStatusTo.class));
    }

    @Test
    public void testExecutePolarisCliPipelineSuccess() {
        PolarisFreestyleCommands polarisCommands = new PolarisFreestyleCommands(mockedPolarisCommandsFactory);

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
        PolarisFreestyleCommands polarisCommands = new PolarisFreestyleCommands(mockedPolarisCommandsFactory);

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
        PolarisFreestyleCommands polarisCommands = new PolarisFreestyleCommands(mockedPolarisCommandsFactory);

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
        PolarisFreestyleCommands polarisCommands = new PolarisFreestyleCommands(mockedPolarisCommandsFactory);

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
        PolarisFreestyleCommands polarisCommands = new PolarisFreestyleCommands(mockedPolarisCommandsFactory);

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
        PolarisFreestyleCommands polarisCommands = new PolarisFreestyleCommands(mockedPolarisCommandsFactory);

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
        PolarisFreestyleCommands polarisCommands = new PolarisFreestyleCommands(mockedPolarisCommandsFactory);

        try {
            Mockito.when(mockedCliIssueCountService.getIssueCount(Mockito.anyLong(), Mockito.any())).thenReturn(SOME_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        assertThrows(PolarisIntegrationException.class, () -> polarisCommands.checkForPolarisIssues(0, false));
    }

}

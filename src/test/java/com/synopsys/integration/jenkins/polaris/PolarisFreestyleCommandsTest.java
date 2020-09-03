package com.synopsys.integration.jenkins.polaris;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.extensions.ChangeBuildStatusTo;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.polaris.extensions.freestyle.WaitForIssues;
import com.synopsys.integration.jenkins.service.JenkinsBuildService;

public class PolarisFreestyleCommandsTest {
    private static final String POLARIS_CLI_NAME = "polarisCliName";
    private static final String POLARIS_ARGUMENTS = "polarisArguments";
    private static final int STATUS_CODE_SUCCESS = 0;
    private static final int STATUS_CODE_FAILURE = 1;
    private static final int NO_ISSUES = 0;
    private static final int SOME_ISSUES = 1;
    private static final Integer JOB_TIMEOUT_IN_MINUTES = 1;

    private JenkinsIntLogger logger;
    private PolarisCliRunner mockedCliRunner;
    private PolarisIssueChecker mockedIssueChecker;
    private JenkinsBuildService mockedBuildService;
    private WaitForIssues waitForIssues;

    @BeforeEach
    public void setUpMocks() {
        logger = new JenkinsIntLogger(null);
        mockedCliRunner = Mockito.mock(PolarisCliRunner.class);
        mockedIssueChecker = Mockito.mock(PolarisIssueChecker.class);
        mockedBuildService = Mockito.mock(JenkinsBuildService.class);

        waitForIssues = new WaitForIssues();
        waitForIssues.setBuildStatusForIssues(ChangeBuildStatusTo.FAILURE);
        waitForIssues.setJobTimeoutInMinutes(JOB_TIMEOUT_IN_MINUTES);
    }

    @Test
    public void testPreserveNullTimeout() throws Throwable {
        waitForIssues.setJobTimeoutInMinutes(null);

        PolarisFreestyleCommands polarisFreestyleCommands = new PolarisFreestyleCommands(logger, mockedBuildService, mockedCliRunner, mockedIssueChecker);
        polarisFreestyleCommands.runPolarisCliAndCheckForIssues(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, waitForIssues);

        Mockito.verify(mockedIssueChecker).getPolarisIssueCount(null);
    }

    @Test
    public void testRunPolarisCliAndCheckForIssuesSuccess() {
        try {
            Mockito.when(mockedCliRunner.runPolarisCli(POLARIS_CLI_NAME, POLARIS_ARGUMENTS)).thenReturn(STATUS_CODE_SUCCESS);
            Mockito.when(mockedIssueChecker.getPolarisIssueCount(JOB_TIMEOUT_IN_MINUTES)).thenReturn(NO_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        PolarisFreestyleCommands polarisFreestyleCommands = new PolarisFreestyleCommands(logger, mockedBuildService, mockedCliRunner, mockedIssueChecker);
        polarisFreestyleCommands.runPolarisCliAndCheckForIssues(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, waitForIssues);

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(String.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAs(Mockito.any(ChangeBuildStatusTo.class));
    }

    @Test
    public void testRunPolarisCliAndCheckForIssuesCliFailure() {
        try {
            Mockito.when(mockedCliRunner.runPolarisCli(POLARIS_CLI_NAME, POLARIS_ARGUMENTS)).thenReturn(STATUS_CODE_FAILURE);
            Mockito.when(mockedIssueChecker.getPolarisIssueCount(JOB_TIMEOUT_IN_MINUTES)).thenReturn(NO_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        PolarisFreestyleCommands polarisFreestyleCommands = new PolarisFreestyleCommands(logger, mockedBuildService, mockedCliRunner, mockedIssueChecker);
        polarisFreestyleCommands.runPolarisCliAndCheckForIssues(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, waitForIssues);

        Mockito.verify(mockedBuildService).markBuildFailed(Mockito.anyString());

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAs(Mockito.any(ChangeBuildStatusTo.class));
    }

    @Test
    public void testExecutePolarisCliFreestyleFailureIssues() {
        try {
            Mockito.when(mockedCliRunner.runPolarisCli(POLARIS_CLI_NAME, POLARIS_ARGUMENTS)).thenReturn(STATUS_CODE_SUCCESS);
            Mockito.when(mockedIssueChecker.getPolarisIssueCount(JOB_TIMEOUT_IN_MINUTES)).thenReturn(SOME_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        PolarisFreestyleCommands polarisFreestyleCommands = new PolarisFreestyleCommands(logger, mockedBuildService, mockedCliRunner, mockedIssueChecker);
        polarisFreestyleCommands.runPolarisCliAndCheckForIssues(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, waitForIssues);

        Mockito.verify(mockedBuildService).markBuildAs(waitForIssues.getBuildStatusForIssues());

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.anyString());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
    }

    @Test
    public void testExecutePolarisCliFreestyleInterrupted() {
        try {
            Mockito.when(mockedCliRunner.runPolarisCli(POLARIS_CLI_NAME, POLARIS_ARGUMENTS)).thenThrow(new InterruptedException());
            Mockito.when(mockedIssueChecker.getPolarisIssueCount(JOB_TIMEOUT_IN_MINUTES)).thenReturn(NO_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        PolarisFreestyleCommands polarisFreestyleCommands = new PolarisFreestyleCommands(logger, mockedBuildService, mockedCliRunner, mockedIssueChecker);
        polarisFreestyleCommands.runPolarisCliAndCheckForIssues(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, waitForIssues);

        Mockito.verify(mockedBuildService).markBuildInterrupted();

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.anyString());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAs(Mockito.any(ChangeBuildStatusTo.class));
    }

    @Test
    public void testExecutePolarisCliFreestyleUnexpectedException() {
        try {
            Mockito.when(mockedCliRunner.runPolarisCli(POLARIS_CLI_NAME, POLARIS_ARGUMENTS)).thenThrow(new IOException());
            Mockito.when(mockedIssueChecker.getPolarisIssueCount(JOB_TIMEOUT_IN_MINUTES)).thenReturn(NO_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        PolarisFreestyleCommands polarisFreestyleCommands = new PolarisFreestyleCommands(logger, mockedBuildService, mockedCliRunner, mockedIssueChecker);
        polarisFreestyleCommands.runPolarisCliAndCheckForIssues(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, waitForIssues);

        Mockito.verify(mockedBuildService).markBuildUnstable(Mockito.any(IOException.class));

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.anyString());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAs(Mockito.any(ChangeBuildStatusTo.class));
    }

    @Test
    public void testExecutePolarisCliFreestyleIntegrationException() {
        try {
            Mockito.when(mockedCliRunner.runPolarisCli(POLARIS_CLI_NAME, POLARIS_ARGUMENTS)).thenThrow(new IntegrationException());
            Mockito.when(mockedIssueChecker.getPolarisIssueCount(JOB_TIMEOUT_IN_MINUTES)).thenReturn(NO_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        PolarisFreestyleCommands polarisFreestyleCommands = new PolarisFreestyleCommands(logger, mockedBuildService, mockedCliRunner, mockedIssueChecker);
        polarisFreestyleCommands.runPolarisCliAndCheckForIssues(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, waitForIssues);

        Mockito.verify(mockedBuildService).markBuildFailed(Mockito.any(IntegrationException.class));

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.anyString());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAs(Mockito.any(ChangeBuildStatusTo.class));
    }

}

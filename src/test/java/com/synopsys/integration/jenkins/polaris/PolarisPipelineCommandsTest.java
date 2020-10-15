package com.synopsys.integration.jenkins.polaris;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.jenkins.exception.JenkinsUserFriendlyException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.polaris.extensions.pipeline.PipelineCreateChangeSetFile;
import com.synopsys.integration.polaris.common.exception.PolarisIntegrationException;

public class PolarisPipelineCommandsTest {
    private static final String POLARIS_CLI_NAME = "polarisCliName";
    private static final String POLARIS_ARGUMENTS = "polarisArguments";
    private static final String CHANGE_SET_FILE_PATH = "path/to/changeSetFile.txt";
    private static final int STATUS_CODE_SKIP = -1;
    private static final int STATUS_CODE_SUCCESS = 0;
    private static final int STATUS_CODE_FAILURE = 1;
    private static final int NO_ISSUES = 0;
    private static final int SOME_ISSUES = 1;
    private static final int JOB_TIMEOUT_IN_MINUTES = 1;
    private static final String INCLUSION_PATTERNS = "FILES_TO_INCLUDE";
    private static final String EXCLUSION_PATTERNS = "FILES_TO_EXCLUDE";

    private JenkinsIntLogger logger;
    private PolarisCliRunner mockedCliRunner;
    private PolarisIssueChecker mockedIssueChecker;
    private ChangeSetFileCreator mockedChangeSetFileCreator;
    private PipelineCreateChangeSetFile createChangeSetFile;

    @BeforeEach
    public void setUpMocks() {
        logger = JenkinsIntLogger.logToStandardOut();
        mockedCliRunner = Mockito.mock(PolarisCliRunner.class);
        mockedIssueChecker = Mockito.mock(PolarisIssueChecker.class);
        mockedChangeSetFileCreator = Mockito.mock(ChangeSetFileCreator.class);

        createChangeSetFile = new PipelineCreateChangeSetFile();
        createChangeSetFile.setExcluding(EXCLUSION_PATTERNS);
        createChangeSetFile.setIncluding(INCLUSION_PATTERNS);
    }

    @Test
    public void testExecutePolarisCliPipelineSuccess() {
        try {
            Mockito.when(mockedCliRunner.runPolarisCli(POLARIS_CLI_NAME, CHANGE_SET_FILE_PATH, POLARIS_ARGUMENTS)).thenReturn(STATUS_CODE_SUCCESS);
            Mockito.when(mockedChangeSetFileCreator.createChangeSetFile(EXCLUSION_PATTERNS, INCLUSION_PATTERNS)).thenReturn(CHANGE_SET_FILE_PATH);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        try {
            PolarisPipelineCommands polarisPipelineCommands = new PolarisPipelineCommands(logger, mockedChangeSetFileCreator, mockedCliRunner, mockedIssueChecker);
            int actualExitCode = polarisPipelineCommands.runPolarisCli(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, true, createChangeSetFile);

            assertEquals(STATUS_CODE_SUCCESS, actualExitCode);
        } catch (Exception e) {
            fail("An unexpected exception occurred.", e);
        }
    }

    @Test
    public void testExecutePolarisCliPipelineException() {
        try {
            Mockito.when(mockedCliRunner.runPolarisCli(POLARIS_CLI_NAME, CHANGE_SET_FILE_PATH, POLARIS_ARGUMENTS)).thenThrow(new IOException());
            Mockito.when(mockedChangeSetFileCreator.createChangeSetFile(EXCLUSION_PATTERNS, INCLUSION_PATTERNS)).thenReturn(CHANGE_SET_FILE_PATH);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        PolarisPipelineCommands polarisPipelineCommands = new PolarisPipelineCommands(logger, mockedChangeSetFileCreator, mockedCliRunner, mockedIssueChecker);
        assertThrows(IOException.class, () -> polarisPipelineCommands.runPolarisCli(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, true, createChangeSetFile));
    }

    @Test
    public void testExecutePolarisCliPipelineFailureReturnStatus() {
        try {
            Mockito.when(mockedCliRunner.runPolarisCli(POLARIS_CLI_NAME, CHANGE_SET_FILE_PATH, POLARIS_ARGUMENTS)).thenReturn(STATUS_CODE_FAILURE);
            Mockito.when(mockedChangeSetFileCreator.createChangeSetFile(EXCLUSION_PATTERNS, INCLUSION_PATTERNS)).thenReturn(CHANGE_SET_FILE_PATH);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        try {
            PolarisPipelineCommands polarisPipelineCommands = new PolarisPipelineCommands(logger, mockedChangeSetFileCreator, mockedCliRunner, mockedIssueChecker);
            int actualExitCode = polarisPipelineCommands.runPolarisCli(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, true, createChangeSetFile);

            assertEquals(STATUS_CODE_FAILURE, actualExitCode);
        } catch (Exception e) {
            fail("An unexpected exception occurred.", e);
        }
    }

    @Test
    public void testExecutePolarisCliPipelineFailureDoNotReturnStatus() {
        try {
            Mockito.when(mockedCliRunner.runPolarisCli(POLARIS_CLI_NAME, CHANGE_SET_FILE_PATH, POLARIS_ARGUMENTS)).thenReturn(STATUS_CODE_FAILURE);
            Mockito.when(mockedChangeSetFileCreator.createChangeSetFile(EXCLUSION_PATTERNS, INCLUSION_PATTERNS)).thenReturn(CHANGE_SET_FILE_PATH);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        PolarisPipelineCommands polarisPipelineCommands = new PolarisPipelineCommands(logger, mockedChangeSetFileCreator, mockedCliRunner, mockedIssueChecker);
        assertThrows(JenkinsUserFriendlyException.class, () -> polarisPipelineCommands.runPolarisCli(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, false, createChangeSetFile));
    }

    @Test
    public void testExecutePolarisCliPipelineSkipDoNotReturnCode() {
        try {
            Mockito.when(mockedCliRunner.runPolarisCli(POLARIS_CLI_NAME, CHANGE_SET_FILE_PATH, POLARIS_ARGUMENTS)).thenReturn(STATUS_CODE_FAILURE);
            Mockito.when(mockedChangeSetFileCreator.createChangeSetFile(EXCLUSION_PATTERNS, INCLUSION_PATTERNS)).thenReturn(null);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        createChangeSetFile.setReturnSkipCode(Boolean.FALSE);

        PolarisPipelineCommands polarisPipelineCommands = new PolarisPipelineCommands(logger, mockedChangeSetFileCreator, mockedCliRunner, mockedIssueChecker);
        assertThrows(JenkinsUserFriendlyException.class, () -> polarisPipelineCommands.runPolarisCli(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, false, createChangeSetFile));
    }

    @Test
    public void testExecutePolarisCliPipelineSkipReturnCode() {
        try {
            Mockito.when(mockedCliRunner.runPolarisCli(POLARIS_CLI_NAME, CHANGE_SET_FILE_PATH, POLARIS_ARGUMENTS)).thenReturn(STATUS_CODE_FAILURE);
            Mockito.when(mockedChangeSetFileCreator.createChangeSetFile(EXCLUSION_PATTERNS, INCLUSION_PATTERNS)).thenReturn(null);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        createChangeSetFile.setReturnSkipCode(Boolean.TRUE);

        try {
            PolarisPipelineCommands polarisPipelineCommands = new PolarisPipelineCommands(logger, mockedChangeSetFileCreator, mockedCliRunner, mockedIssueChecker);
            int actualExitCode = polarisPipelineCommands.runPolarisCli(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, false, createChangeSetFile);

            assertEquals(STATUS_CODE_SKIP, actualExitCode);
        } catch (Exception e) {
            fail("An unexpected exception occurred.", e);
        }
    }

    @Test
    public void testExecutePolarisCliPipelineSkipDefaultBehavior() {
        try {
            Mockito.when(mockedCliRunner.runPolarisCli(POLARIS_CLI_NAME, CHANGE_SET_FILE_PATH, POLARIS_ARGUMENTS)).thenReturn(STATUS_CODE_FAILURE);
            Mockito.when(mockedChangeSetFileCreator.createChangeSetFile(EXCLUSION_PATTERNS, INCLUSION_PATTERNS)).thenReturn(null);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        try {
            PolarisPipelineCommands polarisPipelineCommands = new PolarisPipelineCommands(logger, mockedChangeSetFileCreator, mockedCliRunner, mockedIssueChecker);
            int actualExitCode = polarisPipelineCommands.runPolarisCli(POLARIS_CLI_NAME, POLARIS_ARGUMENTS, false, createChangeSetFile);

            assertEquals(STATUS_CODE_SKIP, actualExitCode);
        } catch (Exception e) {
            fail("An unexpected exception occurred.", e);
        }
    }

    @Test
    public void testCheckForPolarisIssuesSuccess() {
        try {
            Mockito.when(mockedIssueChecker.getPolarisIssueCount(JOB_TIMEOUT_IN_MINUTES)).thenReturn(NO_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        try {
            PolarisPipelineCommands polarisPipelineCommands = new PolarisPipelineCommands(logger, mockedChangeSetFileCreator, mockedCliRunner, mockedIssueChecker);
            int actualIssueCount = polarisPipelineCommands.checkForIssues(JOB_TIMEOUT_IN_MINUTES, true);

            assertEquals(NO_ISSUES, actualIssueCount);
        } catch (Exception e) {
            fail("An unexpected exception occurred.", e);
        }
    }

    @Test
    public void testCheckForPolarisIssuesFailureReturnIssues() {
        try {
            Mockito.when(mockedIssueChecker.getPolarisIssueCount(JOB_TIMEOUT_IN_MINUTES)).thenReturn(SOME_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        try {
            PolarisPipelineCommands polarisPipelineCommands = new PolarisPipelineCommands(logger, mockedChangeSetFileCreator, mockedCliRunner, mockedIssueChecker);
            int actualIssueCount = polarisPipelineCommands.checkForIssues(JOB_TIMEOUT_IN_MINUTES, true);

            assertEquals(SOME_ISSUES, actualIssueCount);
        } catch (Exception e) {
            fail("An unexpected exception occurred.", e);
        }
    }

    @Test
    public void testCheckForPolarisIssuesFailureDoNotReturnIssues() {
        try {
            Mockito.when(mockedIssueChecker.getPolarisIssueCount(JOB_TIMEOUT_IN_MINUTES)).thenReturn(SOME_ISSUES);
        } catch (Exception e) {
            fail("An unexpected exception occurred when preparing the test for setup. Please correct the test code.", e);
        }

        PolarisPipelineCommands polarisPipelineCommands = new PolarisPipelineCommands(logger, mockedChangeSetFileCreator, mockedCliRunner, mockedIssueChecker);
        assertThrows(PolarisIntegrationException.class, () -> polarisPipelineCommands.checkForIssues(JOB_TIMEOUT_IN_MINUTES, false));
    }

}

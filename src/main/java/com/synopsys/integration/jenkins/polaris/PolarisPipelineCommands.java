/*
 * synopsys-polaris
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.polaris;

import java.io.IOException;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.exception.JenkinsUserFriendlyException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.polaris.extensions.pipeline.PipelineCreateChangeSetFile;
import com.synopsys.integration.polaris.common.exception.PolarisIntegrationException;

public class PolarisPipelineCommands {
    private final JenkinsIntLogger logger;
    private final ChangeSetFileCreator changeSetFileCreator;
    private final PolarisCliRunner polarisCliRunner;
    private final PolarisIssueChecker polarisIssueCounter;

    public PolarisPipelineCommands(JenkinsIntLogger jenkinsIntLogger, ChangeSetFileCreator changeSetFileCreator, PolarisCliRunner polarisCliRunner, PolarisIssueChecker polarisIssueCounter) {
        this.logger = jenkinsIntLogger;
        this.changeSetFileCreator = changeSetFileCreator;
        this.polarisCliRunner = polarisCliRunner;
        this.polarisIssueCounter = polarisIssueCounter;
    }

    public int runPolarisCli(String polarisCliName, String polarisCliArgumentString, Boolean returnStatus, PipelineCreateChangeSetFile createChangeSetFile) throws IntegrationException, InterruptedException, IOException {
        String changeSetFilePath = null;
        if (createChangeSetFile != null) {
            changeSetFilePath = changeSetFileCreator.createChangeSetFile(createChangeSetFile.getExcluding(), createChangeSetFile.getIncluding());
            if (changeSetFilePath == null) {
                String skipMessage = "The changeset contained no files to analyze. Skipping Polaris Software Integrity Platform static analysis.";
                if (Boolean.FALSE.equals(createChangeSetFile.getReturnSkipCode())) {
                    throw new JenkinsUserFriendlyException(skipMessage);
                } else {
                    logger.info(skipMessage);
                    return -1;
                }
            }
        }

        int exitCode = polarisCliRunner.runPolarisCli(polarisCliName, changeSetFilePath, polarisCliArgumentString);

        if (exitCode > 0) {
            String errorMsg = "Polaris Software Integrity Platform failed with exit code: " + exitCode;
            if (Boolean.TRUE.equals(returnStatus)) {
                logger.error(errorMsg);
            } else {
                throw new JenkinsUserFriendlyException(errorMsg);
            }
        }

        return exitCode;
    }

    public int checkForIssues(Integer jobTimeoutInMinutes, Boolean returnIssueCount) throws InterruptedException, IntegrationException, IOException {
        int issueCount = polarisIssueCounter.getPolarisIssueCount(jobTimeoutInMinutes);

        String defectMessage = String.format("[Polaris] Found %s total issues.", issueCount);
        if (issueCount > 0) {
            if (Boolean.TRUE.equals(returnIssueCount)) {
                logger.error(defectMessage);
            } else {
                throw new PolarisIntegrationException(defectMessage);
            }
        } else {
            logger.alwaysLog(defectMessage);
        }

        return issueCount;
    }

}

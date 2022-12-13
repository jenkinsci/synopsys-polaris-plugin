/*
 * synopsys-polaris
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.polaris;

import java.util.Optional;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.exception.JenkinsUserFriendlyException;
import com.synopsys.integration.jenkins.extensions.ChangeBuildStatusTo;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.polaris.extensions.freestyle.FreestyleCreateChangeSetFile;
import com.synopsys.integration.jenkins.polaris.extensions.freestyle.WaitForIssues;
import com.synopsys.integration.jenkins.service.JenkinsBuildService;

public class PolarisFreestyleCommands {
    private final JenkinsIntLogger logger;
    private final JenkinsBuildService jenkinsBuildService;
    private final ChangeSetFileCreator changeSetFileCreator;
    private final PolarisCliRunner polarisCliRunner;
    private final PolarisIssueChecker polarisIssueCounter;

    public PolarisFreestyleCommands(JenkinsIntLogger jenkinsIntLogger, JenkinsBuildService jenkinsBuildService, ChangeSetFileCreator changeSetFileCreator, PolarisCliRunner polarisCliRunner, PolarisIssueChecker polarisIssueCounter) {
        this.logger = jenkinsIntLogger;
        this.jenkinsBuildService = jenkinsBuildService;
        this.changeSetFileCreator = changeSetFileCreator;
        this.polarisCliRunner = polarisCliRunner;
        this.polarisIssueCounter = polarisIssueCounter;
    }

    public void runPolarisCliAndCheckForIssues(String polarisCliName, String polarisArgumentString, FreestyleCreateChangeSetFile createChangeSetFile, WaitForIssues waitForIssues) {
        try {
            String changeSetFilePath = null;
            if (createChangeSetFile != null) {
                changeSetFilePath = changeSetFileCreator.createChangeSetFile(createChangeSetFile.getChangeSetExclusionPatterns(), createChangeSetFile.getChangeSetInclusionPatterns());
                if (changeSetFilePath == null) {
                    ChangeBuildStatusTo changeBuildStatusTo = createChangeSetFile.getBuildStatusOnSkip() != null ? createChangeSetFile.getBuildStatusOnSkip() : createChangeSetFile.getDescriptor().getDefaultBuildStatusOnSkip();
                    logger.warn("The changeset contained no files to analyze. Skipping Polaris Software Integrity Platform static analysis.");
                    logger.warn("Performing configured skip action: " + changeBuildStatusTo.getDisplayName());
                    jenkinsBuildService.markBuildAs(changeBuildStatusTo);
                    return;
                }
            }

            int exitCode = polarisCliRunner.runPolarisCli(polarisCliName, changeSetFilePath, polarisArgumentString);
            if (exitCode > 0) {
                jenkinsBuildService.markBuildFailed("Polaris CLI failed with exit code: " + exitCode);
            }

            if (waitForIssues != null) {
                ChangeBuildStatusTo buildStatusToSet = Optional.ofNullable(waitForIssues.getBuildStatusForIssues())
                                                           .orElse(ChangeBuildStatusTo.SUCCESS);

                int issueCount = polarisIssueCounter.getPolarisIssueCount(waitForIssues.getJobTimeoutInMinutes());

                logger.alwaysLog("Polaris Software Integrity Platform Issue Check");
                logger.alwaysLog("Build state for issues: " + buildStatusToSet.getDisplayName());
                logger.alwaysLog(String.format("Found %s issues", issueCount));
                if (issueCount > 0) {
                    jenkinsBuildService.markBuildAs(buildStatusToSet);
                }

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            jenkinsBuildService.markBuildInterrupted();
        } catch (JenkinsUserFriendlyException e) {
            jenkinsBuildService.markBuildFailed(e.getMessage());
        } catch (IntegrationException e) {
            jenkinsBuildService.markBuildFailed(e);
        } catch (Exception e) {
            jenkinsBuildService.markBuildUnstable(e);
        }
    }
}

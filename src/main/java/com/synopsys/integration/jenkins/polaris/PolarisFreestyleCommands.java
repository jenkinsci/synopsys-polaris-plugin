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
package com.synopsys.integration.jenkins.polaris;

import java.util.Optional;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.extensions.ChangeBuildStatusTo;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.polaris.extensions.CreateChangeSetFile;
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

    public void runPolarisCliAndCheckForIssues(String polarisCliName, String polarisArgumentString, CreateChangeSetFile createChangeSetFile, WaitForIssues waitForIssues) {
        try {
            String changeSetFilePath = null;
            if (createChangeSetFile != null) {
                changeSetFilePath = changeSetFileCreator.createChangeSetFile(createChangeSetFile.getChangeSetExclusionPatterns(), createChangeSetFile.getChangeSetInclusionPatterns());
            }

            int exitCode = polarisCliRunner.runPolarisCli(polarisCliName, changeSetFilePath, polarisArgumentString);
            if (exitCode > 0) {
                jenkinsBuildService.markBuildFailed("Polaris failed with exit code: " + exitCode);
            }

            if (waitForIssues != null) {
                ChangeBuildStatusTo buildStatusToSet = Optional.ofNullable(waitForIssues.getBuildStatusForIssues())
                                                           .orElse(ChangeBuildStatusTo.SUCCESS);

                int issueCount = polarisIssueCounter.getPolarisIssueCount(waitForIssues.getJobTimeoutInMinutes());

                logger.alwaysLog("Polaris Issue Check");
                logger.alwaysLog("Build state for issues: " + buildStatusToSet.getDisplayName());
                logger.alwaysLog(String.format("Found %s issues", issueCount));
                if (issueCount > 0) {
                    jenkinsBuildService.markBuildAs(buildStatusToSet);
                }

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            jenkinsBuildService.markBuildInterrupted();
        } catch (IntegrationException e) {
            jenkinsBuildService.markBuildFailed(e);
        } catch (Exception e) {
            jenkinsBuildService.markBuildUnstable(e);
        }
    }
}

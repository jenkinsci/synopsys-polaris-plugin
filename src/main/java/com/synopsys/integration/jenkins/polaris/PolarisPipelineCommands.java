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

import java.io.IOException;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.common.IntegrationAbortException;
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
                    throw new IntegrationAbortException(skipMessage);
                } else {
                    logger.info(skipMessage);
                    return -1;
                }
            }
        }

        int exitCode = polarisCliRunner.runPolarisCli(polarisCliName, changeSetFilePath, polarisCliArgumentString);

        if (exitCode > 0) {
            String errorMsg = "Polaris failed with exit code: " + exitCode;
            if (Boolean.TRUE.equals(returnStatus)) {
                logger.error(errorMsg);
            } else {
                throw new PolarisIntegrationException(errorMsg);
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

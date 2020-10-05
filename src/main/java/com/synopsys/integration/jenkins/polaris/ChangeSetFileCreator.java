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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.jenkins.ChangeSetFilter;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.polaris.service.PolarisEnvironmentService;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.jenkins.service.JenkinsScmService;
import com.synopsys.integration.util.IntEnvironmentVariables;

import jenkins.security.MasterToSlaveCallable;

public class ChangeSetFileCreator {
    private final JenkinsIntLogger logger;
    private final JenkinsRemotingService jenkinsRemotingService;
    private final JenkinsScmService jenkinsScmService;
    private final PolarisEnvironmentService polarisEnvironmentService;

    public ChangeSetFileCreator(JenkinsIntLogger logger, JenkinsRemotingService jenkinsRemotingService, JenkinsScmService jenkinsScmService, PolarisEnvironmentService polarisEnvironmentService) {
        this.logger = logger;
        this.jenkinsRemotingService = jenkinsRemotingService;
        this.jenkinsScmService = jenkinsScmService;
        this.polarisEnvironmentService = polarisEnvironmentService;
    }

    public String createChangeSetFile(String exclusionPatterns, String inclusionPatterns) throws IOException, InterruptedException {
        ChangeSetFilter changeSetFilter = jenkinsScmService.newChangeSetFilter().excludeMatching(exclusionPatterns).includeMatching(inclusionPatterns);

        // ArrayLists are serializable, Lists are not. -- rotte SEP 2020
        ArrayList<String> changedFiles = new ArrayList<>();
        try {
            changedFiles.addAll(jenkinsScmService.getFilePathsFromChangeSet(changeSetFilter));
        } catch (Exception e) {
            logger.error("Could not get the Jenkins change set: " + e.getMessage());
        }

        String remoteWorkspacePath = jenkinsRemotingService.getRemoteWorkspacePath();

        String changeSetFilePath;
        if (changedFiles.size() == 0) {
            logger.info("The change set file could not be created because the Jenkins change set was empty.");
            changeSetFilePath = null;
        } else {
            IntEnvironmentVariables environment = polarisEnvironmentService.getInitialEnvironment();
            String valueOfChangeSetFilePath = environment.getValue(PolarisJenkinsEnvironmentVariable.CHANGE_SET_FILE_PATH.stringValue());

            changeSetFilePath = jenkinsRemotingService.call(new CreateChangeSetFileAndGetRemotePath(valueOfChangeSetFilePath, remoteWorkspacePath, changedFiles));
        }

        return changeSetFilePath;
    }

    private static class CreateChangeSetFileAndGetRemotePath extends MasterToSlaveCallable<String, IOException> {
        private static final long serialVersionUID = -8708849449533708805L;
        private final ArrayList<String> changedFiles;
        private final String changeSetFilePath;
        private final String valueOfChangeSetFilePath;

        public CreateChangeSetFileAndGetRemotePath(String valueOfChangeSetFilePath, String remoteWorkspacePath, ArrayList<String> changedFiles) {
            this.valueOfChangeSetFilePath = valueOfChangeSetFilePath;
            this.changeSetFilePath = remoteWorkspacePath;
            this.changedFiles = changedFiles;
        }

        @Override
        public String call() throws IOException {
            Path changeSetFile;
            if (StringUtils.isNotBlank(valueOfChangeSetFilePath)) {
                changeSetFile = Paths.get(valueOfChangeSetFilePath);
            } else {
                changeSetFile = Paths.get(changeSetFilePath)
                                    .resolve(".synopsys")
                                    .resolve("polaris")
                                    .resolve("changeSetFiles.txt");
            }
            Files.createDirectories(changeSetFile.getParent());
            Files.write(changeSetFile, changedFiles);

            return changeSetFile.toRealPath().toString();
        }
    }
}

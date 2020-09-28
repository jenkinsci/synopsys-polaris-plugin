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

import com.synopsys.integration.jenkins.ChangeSetFilter;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.jenkins.service.JenkinsScmService;

import jenkins.security.MasterToSlaveCallable;

public class ChangeSetFileCreator {
    private final JenkinsIntLogger logger;
    private final JenkinsRemotingService jenkinsRemotingService;
    private final JenkinsScmService jenkinsScmService;

    public ChangeSetFileCreator(JenkinsIntLogger logger, JenkinsRemotingService jenkinsRemotingService, JenkinsScmService jenkinsScmService) {
        this.logger = logger;
        this.jenkinsRemotingService = jenkinsRemotingService;
        this.jenkinsScmService = jenkinsScmService;
    }

    public String createChangeSetFile(String exclusionPatterns, String inclusionPatterns) throws IOException, InterruptedException {
        ChangeSetFilter changeSetFilter = jenkinsScmService.newChangeSetFilter().excludeMatching(exclusionPatterns).includeMatching(inclusionPatterns);

        // ArrayLists are serializable, Lists are not. -- rotte SEP 2020
        ArrayList<String> changedFiles = new ArrayList<>();
        try {
            changedFiles.addAll(jenkinsScmService.getFilePathsFromChangeSet(changeSetFilter));
        } catch (Exception e) {
            logger.error("Could not get the jenkins change set: " + e.getMessage());
        }

        String remoteWorkspacePath = jenkinsRemotingService.getRemoteWorkspacePath();

        String changeSetFilePath;
        if (changedFiles.size() == 0) {
            logger.info("The change set file could not be created because the jenkins change set was empty.");
            changeSetFilePath = null;
        } else {
            changeSetFilePath = jenkinsRemotingService.call(new CreateChangeSetFileAndGetRemotePath(remoteWorkspacePath, changedFiles));
        }

        return changeSetFilePath;
    }

    private static class CreateChangeSetFileAndGetRemotePath extends MasterToSlaveCallable<String, IOException> {
        private static final long serialVersionUID = -8708849449533708805L;
        private final ArrayList<String> changedFiles;
        private final String remoteWorkspacePath;

        public CreateChangeSetFileAndGetRemotePath(String remoteWorkspacePath, ArrayList<String> changedFiles) {
            this.remoteWorkspacePath = remoteWorkspacePath;
            this.changedFiles = changedFiles;
        }

        @Override
        public String call() throws IOException {
            Path changeSetFile = Paths.get(remoteWorkspacePath).resolve("changeSetFiles.txt");
            Files.write(changeSetFile, changedFiles);

            return changeSetFile.toRealPath().toString();
        }
    }
}

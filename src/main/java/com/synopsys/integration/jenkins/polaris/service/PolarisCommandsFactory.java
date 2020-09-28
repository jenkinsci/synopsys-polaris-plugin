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
package com.synopsys.integration.jenkins.polaris.service;

import java.io.IOException;
import java.util.function.Supplier;

import com.synopsys.integration.function.ThrowingSupplier;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.polaris.ChangeSetFileCreator;
import com.synopsys.integration.jenkins.polaris.PolarisCliRunner;
import com.synopsys.integration.jenkins.polaris.PolarisFreestyleCommands;
import com.synopsys.integration.jenkins.polaris.PolarisIssueChecker;
import com.synopsys.integration.jenkins.polaris.PolarisPipelineCommands;
import com.synopsys.integration.jenkins.polaris.extensions.global.PolarisGlobalConfig;
import com.synopsys.integration.jenkins.service.JenkinsBuildService;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.jenkins.service.JenkinsFreestyleServicesFactory;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.jenkins.service.JenkinsScmService;
import com.synopsys.integration.jenkins.service.JenkinsServicesFactory;
import com.synopsys.integration.jenkins.wrapper.JenkinsWrapper;
import com.synopsys.integration.polaris.common.cli.PolarisCliResponseUtility;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfig;
import com.synopsys.integration.polaris.common.service.ContextsService;
import com.synopsys.integration.polaris.common.service.CountService;
import com.synopsys.integration.polaris.common.service.JobService;
import com.synopsys.integration.polaris.common.service.PolarisServicesFactory;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;

public class PolarisCommandsFactory {
    private final EnvVars envVars;
    private final TaskListener listener;
    private final ThrowingSupplier<JenkinsWrapper, AbortException> validatedJenkinsWrapper;
    // These fields are lazily initialized; within this class use the suppliers instead of referencing the fields directly
    private JenkinsIntLogger _logger = null;
    private final Supplier<JenkinsIntLogger> initializedLogger = this::getOrCreateLogger;

    private PolarisCommandsFactory(JenkinsWrapper jenkinsWrapper, EnvVars envVars, TaskListener listener) {
        this.validatedJenkinsWrapper = () -> validateJenkinsWrapper(jenkinsWrapper);
        this.envVars = envVars;
        this.listener = listener;
    }

    public static PolarisFreestyleCommands fromPostBuild(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        PolarisCommandsFactory polarisCommandsFactory = new PolarisCommandsFactory(JenkinsWrapper.initializeFromJenkinsJVM(), build.getEnvironment(listener), listener);
        JenkinsFreestyleServicesFactory jenkinsServicesFactory = new JenkinsFreestyleServicesFactory(polarisCommandsFactory.getOrCreateLogger(), build, build.getEnvironment(listener), launcher, listener, build.getBuiltOn(),
            build.getWorkspace());

        JenkinsRemotingService jenkinsRemotingService = jenkinsServicesFactory.createJenkinsRemotingService();
        JenkinsConfigService jenkinsConfigService = jenkinsServicesFactory.createJenkinsConfigService();
        JenkinsBuildService jenkinsBuildService = jenkinsServicesFactory.createJenkinsBuildService();
        JenkinsScmService jenkinsScmService = jenkinsServicesFactory.createJenkinsScmService();

        ChangeSetFileCreator changeSetFileCreator = polarisCommandsFactory.createChangeSetFileCreator(jenkinsRemotingService, jenkinsScmService);
        PolarisCliRunner polarisCliRunner = polarisCommandsFactory.createPolarisCliRunner(jenkinsConfigService, jenkinsRemotingService);
        PolarisIssueChecker polarisIssueCounter = polarisCommandsFactory.createPolarisIssueCounter(jenkinsConfigService, jenkinsRemotingService);

        return new PolarisFreestyleCommands(polarisCommandsFactory.getOrCreateLogger(), jenkinsBuildService, changeSetFileCreator, polarisCliRunner, polarisIssueCounter);
    }

    public static PolarisPipelineCommands fromPipeline(TaskListener listener, EnvVars envVars, Launcher launcher, Node node, Run<?, ?> run, FilePath workspace) throws AbortException {
        PolarisCommandsFactory polarisCommandsFactory = new PolarisCommandsFactory(JenkinsWrapper.initializeFromJenkinsJVM(), envVars, listener);
        JenkinsServicesFactory jenkinsServicesFactory = new JenkinsServicesFactory(polarisCommandsFactory.getOrCreateLogger(), envVars, launcher, listener, node, run, workspace);

        JenkinsRemotingService jenkinsRemotingService = jenkinsServicesFactory.createJenkinsRemotingService();
        JenkinsConfigService jenkinsConfigService = jenkinsServicesFactory.createJenkinsConfigService();
        JenkinsScmService jenkinsScmService = jenkinsServicesFactory.createJenkinsScmService();

        ChangeSetFileCreator changeSetFileCreator = polarisCommandsFactory.createChangeSetFileCreator(jenkinsRemotingService, jenkinsScmService);
        PolarisCliRunner polarisCliRunner = polarisCommandsFactory.createPolarisCliRunner(jenkinsConfigService, jenkinsRemotingService);
        PolarisIssueChecker polarisIssueCounter = polarisCommandsFactory.createPolarisIssueCounter(jenkinsConfigService, jenkinsRemotingService);

        return new PolarisPipelineCommands(polarisCommandsFactory.getOrCreateLogger(), changeSetFileCreator, polarisCliRunner, polarisIssueCounter);
    }

    public PolarisIssueChecker createPolarisIssueCounter(JenkinsConfigService jenkinsConfigService, JenkinsRemotingService jenkinsRemotingService) throws AbortException {
        return new PolarisIssueChecker(initializedLogger.get(), createPolarisCliIssueCountService(jenkinsConfigService), jenkinsRemotingService, validatedJenkinsWrapper.get().getVersionHelper());
    }

    public PolarisCliRunner createPolarisCliRunner(JenkinsConfigService jenkinsConfigService, JenkinsRemotingService jenkinsRemotingService) throws AbortException {
        JenkinsWrapper jenkinsWrapper = validatedJenkinsWrapper.get();
        return new PolarisCliRunner(initializedLogger.get(),
            createPolarisCliArgumentService(),
            createPolarisEnvironmentService(),
            createPolarisPhoneHomeService(jenkinsConfigService),
            jenkinsRemotingService,
            jenkinsConfigService,
            jenkinsWrapper.getCredentialsHelper(),
            jenkinsWrapper.getProxyHelper(),
            jenkinsWrapper.getVersionHelper());
    }

    public ChangeSetFileCreator createChangeSetFileCreator(JenkinsRemotingService jenkinsRemotingService, JenkinsScmService jenkinsScmService) {
        return new ChangeSetFileCreator(initializedLogger.get(), jenkinsRemotingService, jenkinsScmService);
    }

    private PolarisEnvironmentService createPolarisEnvironmentService() {
        return new PolarisEnvironmentService(envVars);
    }

    private PolarisCliArgumentService createPolarisCliArgumentService() {
        return new PolarisCliArgumentService(initializedLogger.get());
    }

    private PolarisCliIssueCountService createPolarisCliIssueCountService(JenkinsConfigService jenkinsConfigService) throws AbortException {
        PolarisServicesFactory polarisServicesFactory = createPolarisServicesFactory(jenkinsConfigService);
        JobService jobService = polarisServicesFactory.createJobService();
        CountService countService = polarisServicesFactory.createCountService();
        PolarisCliResponseUtility polarisCliResponseUtility = PolarisCliResponseUtility.defaultUtility(initializedLogger.get());

        return new PolarisCliIssueCountService(initializedLogger.get(), countService, jobService, polarisCliResponseUtility);
    }

    private PolarisPhoneHomeService createPolarisPhoneHomeService(JenkinsConfigService jenkinsConfigService) throws AbortException {
        PolarisServicesFactory polarisServicesFactory = createPolarisServicesFactory(jenkinsConfigService);
        ContextsService contextsService = polarisServicesFactory.createContextsService();
        JenkinsWrapper jenkinsWrapper = validatedJenkinsWrapper.get();

        return new PolarisPhoneHomeService(initializedLogger.get(), jenkinsWrapper.getVersionHelper(), contextsService, polarisServicesFactory.getHttpClient());
    }

    private JenkinsIntLogger getOrCreateLogger() {
        if (_logger == null) {
            _logger = JenkinsIntLogger.logToListener(listener);
        }
        return _logger;
    }

    private PolarisServicesFactory createPolarisServicesFactory(JenkinsConfigService jenkinsConfigService) throws AbortException {
        PolarisGlobalConfig polarisGlobalConfig = jenkinsConfigService.getGlobalConfiguration(PolarisGlobalConfig.class)
                                                      .orElseThrow(() -> new AbortException("Polaris cannot be executed: No Polaris global configuration detected in the Jenkins system configuration."));

        JenkinsIntLogger jenkinsIntLogger = getOrCreateLogger();
        JenkinsWrapper jenkinsWrapper = validatedJenkinsWrapper.get();
        PolarisServerConfig polarisServerConfig = polarisGlobalConfig.getPolarisServerConfig(jenkinsWrapper.getCredentialsHelper(), jenkinsWrapper.getProxyHelper());
        return polarisServerConfig.createPolarisServicesFactory(jenkinsIntLogger);
    }

    private JenkinsWrapper validateJenkinsWrapper(JenkinsWrapper jenkinsWrapper) throws AbortException {
        if (jenkinsWrapper.getJenkins().isPresent()) {
            return jenkinsWrapper;
        }

        throw new AbortException("Polaris cannot be executed: The Jenkins instance was not started, was already shut down, or is not reachable from this JVM.");
    }

}

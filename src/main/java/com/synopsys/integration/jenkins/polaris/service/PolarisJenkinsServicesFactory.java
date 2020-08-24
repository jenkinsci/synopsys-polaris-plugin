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
import com.synopsys.integration.jenkins.polaris.extensions.global.PolarisGlobalConfig;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.jenkins.service.JenkinsServicesFactory;
import com.synopsys.integration.jenkins.wrapper.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.wrapper.JenkinsVersionHelper;
import com.synopsys.integration.jenkins.wrapper.JenkinsWrapper;
import com.synopsys.integration.jenkins.wrapper.SynopsysCredentialsHelper;
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
import hudson.model.TaskListener;

public class PolarisJenkinsServicesFactory {
    private final AbstractBuild<?, ?> build;
    private final EnvVars envVars;
    private final Launcher launcher;
    private final TaskListener listener;
    private final FilePath workspace;
    private final ThrowingSupplier<JenkinsWrapper, AbortException> validatedJenkinsWrapper;
    private final Node node;
    // These fields are lazily initialized; within this class use the suppliers instead of referencing the fields directly
    private JenkinsIntLogger _logger = null;
    private final Supplier<JenkinsIntLogger> initializedLogger = this::getOrCreateLogger;
    private PolarisServicesFactory _polarisServicesFactory = null;
    private final ThrowingSupplier<PolarisServicesFactory, AbortException> initializedPolarisServicesFactory = this::getOrCreatePolarisServicesFactory;
    private JenkinsVersionHelper _jenkinsVersionHelper = null;
    private final ThrowingSupplier<JenkinsVersionHelper, AbortException> initializedJenkinsVersionHelper = this::getOrCreateJenkinsVersionHelper;

    public PolarisJenkinsServicesFactory(JenkinsWrapper jenkinsWrapper, AbstractBuild<?, ?> build, EnvVars envVars, Launcher launcher, TaskListener listener, Node node, FilePath workspace) {
        this.build = build;
        this.validatedJenkinsWrapper = () -> validateJenkinsWrapper(jenkinsWrapper);
        this.envVars = envVars;
        this.launcher = launcher;
        this.listener = listener;
        this.node = node;
        this.workspace = workspace;
    }

    public static PolarisJenkinsServicesFactory fromPostBuild(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        return new PolarisJenkinsServicesFactory(JenkinsWrapper.initializeFromJenkinsJVM(), build, build.getEnvironment(listener), launcher, listener, build.getBuiltOn(), build.getWorkspace());
    }

    public static PolarisJenkinsServicesFactory fromPipeline(TaskListener listener, EnvVars envVars, Launcher launcher, Node node, FilePath workspace) {
        return new PolarisJenkinsServicesFactory(JenkinsWrapper.initializeFromJenkinsJVM(), null, envVars, launcher, listener, node, workspace);
    }

    public JenkinsServicesFactory createJenkinsServiceFactory() {
        return new JenkinsServicesFactory(getOrCreateLogger(), build, envVars, launcher, listener, node, workspace);
    }

    public PolarisEnvironmentService createPolarisEnvironmentService() throws AbortException {
        JenkinsWrapper jenkinsWrapper = validatedJenkinsWrapper.get();
        SynopsysCredentialsHelper synopsysCredentialsHelper = new SynopsysCredentialsHelper(jenkinsWrapper);
        JenkinsProxyHelper jenkinsProxyHelper = JenkinsProxyHelper.fromJenkins(jenkinsWrapper);
        return new PolarisEnvironmentService(initializedLogger.get(), initializedJenkinsVersionHelper.get(), synopsysCredentialsHelper, jenkinsProxyHelper, envVars);
    }

    public PolarisCliArgumentService createPolarisCliArgumentService() {
        return new PolarisCliArgumentService(initializedLogger.get());
    }

    public PolarisCliIssueCountService createPolarisCliIssueCountService() throws AbortException {
        PolarisServicesFactory polarisServicesFactory = initializedPolarisServicesFactory.get();
        JobService jobService = polarisServicesFactory.createJobService();
        CountService countService = polarisServicesFactory.createCountService();
        PolarisCliResponseUtility polarisCliResponseUtility = PolarisCliResponseUtility.defaultUtility(initializedLogger.get());

        return new PolarisCliIssueCountService(initializedLogger.get(), countService, jobService, polarisCliResponseUtility);
    }

    public PolarisPhoneHomeService createPolarisPhoneHomeService() throws AbortException {
        PolarisServicesFactory polarisServicesFactory = initializedPolarisServicesFactory.get();
        ContextsService contextsService = polarisServicesFactory.createContextsService();

        return new PolarisPhoneHomeService(initializedLogger.get(), initializedJenkinsVersionHelper.get(), contextsService, polarisServicesFactory.getHttpClient());
    }

    public JenkinsIntLogger getOrCreateLogger() {
        if (_logger == null) {
            _logger = new JenkinsIntLogger(listener);
        }
        return _logger;
    }

    public PolarisServicesFactory getOrCreatePolarisServicesFactory() throws AbortException {
        if (_polarisServicesFactory == null) {
            JenkinsServicesFactory jenkinsServicesFactory = createJenkinsServiceFactory();
            JenkinsConfigService jenkinsConfigService = jenkinsServicesFactory.createJenkinsConfigService();
            PolarisGlobalConfig polarisGlobalConfig = jenkinsConfigService.getGlobalConfiguration(PolarisGlobalConfig.class)
                                                          .orElseThrow(() -> new AbortException("Polaris cannot be executed: No Polaris global configuration detected in the Jenkins system configuration."));

            JenkinsIntLogger jenkinsIntLogger = getOrCreateLogger();
            JenkinsWrapper jenkinsWrapper = validatedJenkinsWrapper.get();
            SynopsysCredentialsHelper synopsysCredentialsHelper = new SynopsysCredentialsHelper(jenkinsWrapper);
            JenkinsProxyHelper jenkinsProxyHelper = JenkinsProxyHelper.fromJenkins(jenkinsWrapper);
            PolarisServerConfig polarisServerConfig = polarisGlobalConfig.getPolarisServerConfig(synopsysCredentialsHelper, jenkinsProxyHelper);
            _polarisServicesFactory = polarisServerConfig.createPolarisServicesFactory(jenkinsIntLogger);
        }
        return _polarisServicesFactory;
    }

    public JenkinsVersionHelper getOrCreateJenkinsVersionHelper() throws AbortException {
        if (_jenkinsVersionHelper == null) {
            JenkinsWrapper jenkinsWrapper = validatedJenkinsWrapper.get();
            _jenkinsVersionHelper = new JenkinsVersionHelper(jenkinsWrapper);
        }

        return _jenkinsVersionHelper;
    }

    private JenkinsWrapper validateJenkinsWrapper(JenkinsWrapper jenkinsWrapper) throws AbortException {
        if (jenkinsWrapper.getJenkins().isPresent()) {
            return jenkinsWrapper;
        }

        throw new AbortException("Polaris cannot be executed: The Jenkins instance was not started, was already shut down, or is not reachable from this JVM.");
    }

}

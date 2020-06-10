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
package com.synopsys.integration.jenkins.polaris.workflow;

import java.util.Optional;
import java.util.function.Supplier;

import com.synopsys.integration.function.ThrowingConsumer;
import com.synopsys.integration.function.ThrowingSupplier;
import com.synopsys.integration.jenkins.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.JenkinsVersionHelper;
import com.synopsys.integration.jenkins.SynopsysCredentialsHelper;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.polaris.extensions.global.PolarisGlobalConfig;
import com.synopsys.integration.jenkins.polaris.extensions.tools.PolarisCli;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfig;
import com.synopsys.integration.polaris.common.service.CountService;
import com.synopsys.integration.polaris.common.service.JobService;
import com.synopsys.integration.polaris.common.service.PolarisServicesFactory;
import com.synopsys.integration.stepworkflow.SubStep;
import com.synopsys.integration.stepworkflow.jenkins.RemoteSubStep;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

public class PolarisWorkflowStepFactory {
    private final EnvVars envVars;
    private final Launcher launcher;
    private final TaskListener listener;
    private final ThrowingSupplier<Node, AbortException> validatedNode;
    private final ThrowingSupplier<FilePath, AbortException> validatedWorkspace;
    private final ThrowingSupplier<Jenkins, AbortException> validatedJenkins;
    // These fields are lazily initialized; within this class use the suppliers instead of referencing the fields directly
    private IntEnvironmentVariables _intEnvironmentVariables = null;
    private final Supplier<IntEnvironmentVariables> initializedIntEnvironmentVariables = this::getOrCreateEnvironmentVariables;
    private JenkinsIntLogger _logger = null;
    private final Supplier<JenkinsIntLogger> initializedLogger = this::getOrCreateLogger;
    private PolarisServicesFactory _polarisServicesFactory = null;
    private final ThrowingSupplier<PolarisServicesFactory, AbortException> initializedPolarisServicesFactory = this::getOrCreatePolarisServicesFactory;
    private JenkinsVersionHelper _jenkinsVersionHelper = null;
    private final ThrowingSupplier<JenkinsVersionHelper, AbortException> initializedJenkinsVersionHelper = this::getOrCreateJenkinsVersionHelper;

    public PolarisWorkflowStepFactory(Jenkins jenkins, Node node, FilePath workspace, EnvVars envVars, Launcher launcher, TaskListener listener) {
        this.validatedJenkins = () -> validateJenkins(jenkins);
        this.validatedNode = () -> validateNode(node);
        this.validatedWorkspace = () -> validateWorkspace(workspace);
        this.envVars = envVars;
        this.launcher = launcher;
        this.listener = listener;
    }

    public CreatePolarisEnvironment createStepCreatePolarisEnvironment() throws AbortException {
        Jenkins jenkins = validatedJenkins.get();
        SynopsysCredentialsHelper synopsysCredentialsHelper = new SynopsysCredentialsHelper(jenkins);
        JenkinsProxyHelper jenkinsProxyHelper = JenkinsProxyHelper.fromJenkins(jenkins);
        return new CreatePolarisEnvironment(initializedLogger.get(), initializedJenkinsVersionHelper.get(), synopsysCredentialsHelper, jenkinsProxyHelper, initializedIntEnvironmentVariables.get());
    }

    public FindPolarisCli createStepFindPolarisCli(String polarisCliName) throws AbortException {
        if (!PolarisCli.installationsExist()) {
            throw new AbortException("Polaris cannot be executed: No Polaris CLI installations could be found in the Global Tool Configuration. Please configure a Polaris CLI installation.");
        }

        PolarisCli polarisCli = PolarisCli.findInstallationWithName(polarisCliName)
                                    .orElseThrow(() -> new AbortException(
                                        String.format("Polaris cannot be executed: No Polaris CLI installation with the name %s could be found in the Global Tool Configuration.", polarisCliName)));

        return new FindPolarisCli(launcher.getChannel(), polarisCli, validatedNode.get(), listener, envVars);
    }

    public ExecutePolarisCli createStepExecutePolarisCli(String polarisArguments) throws AbortException {
        return new ExecutePolarisCli(initializedLogger.get(), launcher, initializedIntEnvironmentVariables.get(), validatedWorkspace.get(), listener, polarisArguments);
    }

    public RemoteSubStep<String> createStepGetPolarisCliResponseContent() throws AbortException {
        GetPolarisCliResponseContent getPolarisCliResponseContent = new GetPolarisCliResponseContent(validatedWorkspace.get().getRemote());
        return RemoteSubStep.of(launcher.getChannel(), getPolarisCliResponseContent);
    }

    public GetTotalIssueCount createStepGetTotalIssueCount(Integer jobTimeoutInMinutes) throws AbortException {
        PolarisServicesFactory polarisServicesFactory = initializedPolarisServicesFactory.get();
        JobService jobService = polarisServicesFactory.createJobService();
        CountService countService = polarisServicesFactory.createCountService();
        Long jobTimeoutInSeconds = Optional.ofNullable(jobTimeoutInMinutes)
                                       .map(value -> value * 60L)
                                       .orElse(JobService.DEFAULT_TIMEOUT);

        return new GetTotalIssueCount(initializedLogger.get(), countService, jobService, jobTimeoutInSeconds);
    }

    public SubStep<Integer, Object> createStepWithConsumer(ThrowingConsumer<Integer, RuntimeException> consumer) {
        return SubStep.ofConsumer(consumer);
    }

    public JenkinsIntLogger getOrCreateLogger() {
        if (_logger == null) {
            _logger = new JenkinsIntLogger(listener);
            _logger.setLogLevel(initializedIntEnvironmentVariables.get());
        }
        return _logger;
    }

    public PolarisServicesFactory getOrCreatePolarisServicesFactory() throws AbortException {
        JenkinsIntLogger jenkinsIntLogger = getOrCreateLogger();
        if (_polarisServicesFactory == null) {
            PolarisGlobalConfig polarisGlobalConfig = GlobalConfiguration.all().get(PolarisGlobalConfig.class);
            if (polarisGlobalConfig == null) {
                throw new AbortException("Polaris cannot be executed: No Polaris global configuration detected in the Jenkins system configuration.");
            }
            Jenkins jenkins = validatedJenkins.get();
            SynopsysCredentialsHelper synopsysCredentialsHelper = new SynopsysCredentialsHelper(jenkins);
            JenkinsProxyHelper jenkinsProxyHelper = JenkinsProxyHelper.fromJenkins(jenkins);
            PolarisServerConfig polarisServerConfig = polarisGlobalConfig.getPolarisServerConfig(synopsysCredentialsHelper, jenkinsProxyHelper);
            _polarisServicesFactory = polarisServerConfig.createPolarisServicesFactory(jenkinsIntLogger);
        }
        return _polarisServicesFactory;
    }

    public IntEnvironmentVariables getOrCreateEnvironmentVariables() {
        if (_intEnvironmentVariables == null) {
            _intEnvironmentVariables = new IntEnvironmentVariables(false);
            _intEnvironmentVariables.putAll(envVars);
        }
        return _intEnvironmentVariables;
    }

    public JenkinsVersionHelper getOrCreateJenkinsVersionHelper() throws AbortException {
        if (_jenkinsVersionHelper == null) {
            Jenkins jenkins = validatedJenkins.get();
            _jenkinsVersionHelper = new JenkinsVersionHelper(jenkins);
        }

        return _jenkinsVersionHelper;
    }

    private Node validateNode(Node node) throws AbortException {
        if (node == null) {
            throw new AbortException("Polaris cannot be executed: The node that it was executed on no longer exists.");
        }

        return node;
    }

    private FilePath validateWorkspace(FilePath workspace) throws AbortException {
        if (workspace == null) {
            throw new AbortException("Polaris cannot be executed: The workspace could not be determined.");
        }

        return workspace;
    }

    private Jenkins validateJenkins(Jenkins jenkins) throws AbortException {
        if (jenkins == null) {
            throw new AbortException("Polaris cannot be executed: The Jenkins instance was not started, was already shut down, or is not reachable from this JVM.");
        }

        return jenkins;
    }
}

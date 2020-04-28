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

public class PolarisWorkflowStepFactory {
    private final EnvVars envVars;
    private final Launcher launcher;
    private final TaskListener listener;
    private final ThrowingSupplier<Node, AbortException> validatedNode;
    private final ThrowingSupplier<FilePath, AbortException> validatedWorkspace;

    // These fields are lazily initialized; within this class use the suppliers instead of referencing the fields directly
    private IntEnvironmentVariables _intEnvironmentVariables = null;
    private final Supplier<IntEnvironmentVariables> initializedIntEnvironmentVariables = this::getOrCreateEnvironmentVariables;
    private JenkinsIntLogger _logger = null;
    private final Supplier<JenkinsIntLogger> initializedLogger = this::getOrCreateLogger;
    private PolarisServicesFactory _polarisServicesFactory = null;
    private final ThrowingSupplier<PolarisServicesFactory, AbortException> initializedPolarisServicesFactory = this::getOrCreatePolarisServicesFactory;

    public PolarisWorkflowStepFactory(final Node node, final FilePath workspace, final EnvVars envVars, final Launcher launcher, final TaskListener listener) {
        this.validatedNode = () -> validateNode(node);
        this.validatedWorkspace = () -> validateWorkspace(workspace);
        this.envVars = envVars;
        this.launcher = launcher;
        this.listener = listener;
    }

    public CreatePolarisEnvironment createStepCreatePolarisEnvironment() {
        return new CreatePolarisEnvironment(initializedLogger.get(), initializedIntEnvironmentVariables.get());
    }

    public FindPolarisCli createStepFindPolarisCli(final String polarisCliName) throws AbortException {
        if (!PolarisCli.installationsExist()) {
            throw new AbortException("Polaris cannot be executed: No Polaris CLI installations could be found in the Global Tool Configuration. Please configure a Polaris CLI installation.");
        }

        final PolarisCli polarisCli = PolarisCli.findInstallationWithName(polarisCliName)
                                          .orElseThrow(() -> new AbortException(
                                              String.format("Polaris cannot be executed: No Polaris CLI installation with the name %s could be found in the Global Tool Configuration.", polarisCliName)));

        return new FindPolarisCli(launcher.getChannel(), polarisCli, validatedNode.get(), listener, envVars);
    }

    public ExecutePolarisCli createStepExecutePolarisCli(final String polarisArguments) throws AbortException {
        return new ExecutePolarisCli(initializedLogger.get(), launcher, initializedIntEnvironmentVariables.get(), validatedWorkspace.get(), listener, polarisArguments);
    }

    public RemoteSubStep<String> createStepGetPolarisCliResponseContent() throws AbortException {
        final GetPolarisCliResponseContent getPolarisCliResponseContent = new GetPolarisCliResponseContent(validatedWorkspace.get().getRemote());
        return RemoteSubStep.of(launcher.getChannel(), getPolarisCliResponseContent);
    }

    public GetTotalIssueCount createStepGetTotalIssueCount(final Integer jobTimeoutInMinutes) throws AbortException {
        final PolarisServicesFactory polarisServicesFactory = initializedPolarisServicesFactory.get();
        final JobService jobService = polarisServicesFactory.createJobService();
        final CountService countService = polarisServicesFactory.createCountService();
        final Long jobTimeoutInSeconds = Optional.ofNullable(jobTimeoutInMinutes)
                                             .map(value -> value * 60L)
                                             .orElse(JobService.DEFAULT_TIMEOUT);

        return new GetTotalIssueCount(initializedLogger.get(), countService, jobService, jobTimeoutInSeconds);
    }

    public SubStep<Integer, Object> createStepWithConsumer(final ThrowingConsumer<Integer, RuntimeException> consumer) {
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
        final JenkinsIntLogger jenkinsIntLogger = getOrCreateLogger();
        if (_polarisServicesFactory == null) {
            final PolarisGlobalConfig polarisGlobalConfig = GlobalConfiguration.all().get(PolarisGlobalConfig.class);
            if (polarisGlobalConfig == null) {
                throw new AbortException("Polaris cannot be executed: No Polaris global configuration detected in the Jenkins system configuration.");
            }
            final PolarisServerConfig polarisServerConfig = polarisGlobalConfig.getPolarisServerConfig();
            _polarisServicesFactory = polarisServerConfig.createPolarisServicesFactory(jenkinsIntLogger);
        }
        return _polarisServicesFactory;
    }

    private IntEnvironmentVariables getOrCreateEnvironmentVariables() {
        if (_intEnvironmentVariables == null) {
            _intEnvironmentVariables = new IntEnvironmentVariables(false);
            _intEnvironmentVariables.putAll(envVars);
        }
        return _intEnvironmentVariables;
    }

    private Node validateNode(final Node node) throws AbortException {
        if (node == null) {
            throw new AbortException("Polaris cannot be executed: The node that it was executed on no longer exists.");
        }

        return node;
    }

    private FilePath validateWorkspace(final FilePath workspace) throws AbortException {
        if (workspace == null) {
            throw new AbortException("Polaris cannot be executed: The workspace could not be determined.");
        }

        return workspace;
    }

}

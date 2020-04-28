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
package com.synopsys.integration.jenkins.polaris.extensions.freestyle;

import java.io.IOException;
import java.io.Serializable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.synopsys.integration.jenkins.annotations.HelpMarkdown;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.polaris.extensions.tools.PolarisCli;
import com.synopsys.integration.jenkins.polaris.workflow.PolarisWorkflowStepFactory;
import com.synopsys.integration.polaris.common.service.PolarisServicesFactory;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tools.ToolInstallation;
import hudson.util.ListBoxModel;

public class PolarisBuildStep extends Builder {
    public static final String DISPLAY_NAME = "Synopsys Polaris";

    @Nullable
    @HelpMarkdown("The Polaris CLI installation to execute")
    private String polarisCliName;

    @Nullable
    @HelpMarkdown("The command line arguments to pass to the Synopsys Polaris CLI")
    private String polarisArguments;

    @Nullable
    @HelpMarkdown("Check this box to wait for Polaris CLI jobs to complete and set the build status based on issues discovered")
    private WaitForIssues waitForIssues;

    @DataBoundConstructor
    public PolarisBuildStep() {
        // Nothing to do-- we generally want to only use DataBoundSetters if we can avoid it, but having no DataBoundConstructor can cause issues.
        // -- rotte FEB 2020
    }

    @Nullable
    public String getPolarisArguments() {
        return polarisArguments;
    }

    @DataBoundSetter
    public void setPolarisArguments(final String polarisArguments) {
        this.polarisArguments = polarisArguments;
    }

    @Nullable
    public String getPolarisCliName() {
        return polarisCliName;
    }

    @DataBoundSetter
    public void setPolarisCliName(final String polarisCliName) {
        this.polarisCliName = polarisCliName;
    }

    @Nullable
    public WaitForIssues getWaitForIssues() {
        return waitForIssues;
    }

    @DataBoundSetter
    public void setWaitForIssues(final WaitForIssues waitForIssues) {
        this.waitForIssues = waitForIssues;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) throws InterruptedException, IOException {
        final PolarisWorkflowStepFactory polarisWorkflowStepFactory = new PolarisWorkflowStepFactory(build.getBuiltOn(), build.getWorkspace(), build.getEnvironment(listener), launcher, listener);
        final JenkinsIntLogger logger = polarisWorkflowStepFactory.getOrCreateLogger();
        final PolarisServicesFactory polarisServicesFactory = polarisWorkflowStepFactory.getOrCreatePolarisServicesFactory();
        final PolarisBuildStepWorkflow polarisBuildStepWorkflow = new PolarisBuildStepWorkflow(polarisWorkflowStepFactory, logger, polarisServicesFactory, polarisCliName, polarisArguments, waitForIssues, build);
        return polarisBuildStepWorkflow.perform()
                   .handleResponse(polarisBuildStepWorkflow::handleResponse);
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> implements Serializable {
        private static final long serialVersionUID = -3800519788262007744L;

        public DescriptorImpl() {
            super(PolarisBuildStep.class);
            load();
        }

        public ListBoxModel doFillPolarisCliNameItems() {
            final PolarisCli.DescriptorImpl polarisCliToolInstallationDescriptor = ToolInstallation.all().get(PolarisCli.DescriptorImpl.class);

            if (polarisCliToolInstallationDescriptor == null) {
                return new ListBoxModel();
            }

            return Stream.of(polarisCliToolInstallationDescriptor.getInstallations())
                       .map(PolarisCli::getName)
                       .map(ListBoxModel.Option::new)
                       .collect(Collectors.toCollection(ListBoxModel::new));
        }

        @Override
        public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}

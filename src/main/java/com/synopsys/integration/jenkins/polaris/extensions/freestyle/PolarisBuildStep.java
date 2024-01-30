/*
 * synopsys-polaris
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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
import com.synopsys.integration.jenkins.polaris.extensions.tools.PolarisCli;
import com.synopsys.integration.jenkins.polaris.service.PolarisCommandsFactory;

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
    public static final String DISPLAY_NAME = "Coverity on Polaris";

    @Nullable
    @HelpMarkdown("The CLI installation to execute")
    private String polarisCliName;

    @Nullable
    @HelpMarkdown("The command line arguments to pass to the CLI")
    private String polarisArguments;

    @Nullable
    @HelpMarkdown("Creates a file at $CHANGE_SET_FILE_PATH (by default, the workspace directory) containing a list of files generated from the Jenkins-provided scm changeset.  \r\n"
                      + "Used for Incremental analysis (--incremental) as the file containing the list of changed files for analysis.")
    private FreestyleCreateChangeSetFile createChangeSetFile;

    @Nullable
    @HelpMarkdown("Check this box to wait for CLI jobs to complete and set the build status based on issues discovered")
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
    public void setPolarisArguments(String polarisArguments) {
        this.polarisArguments = polarisArguments;
    }

    @Nullable
    public String getPolarisCliName() {
        return polarisCliName;
    }

    @DataBoundSetter
    public void setPolarisCliName(String polarisCliName) {
        this.polarisCliName = polarisCliName;
    }

    @Nullable
    public WaitForIssues getWaitForIssues() {
        return waitForIssues;
    }

    @DataBoundSetter
    public void setWaitForIssues(WaitForIssues waitForIssues) {
        this.waitForIssues = waitForIssues;
    }

    @Nullable
    public FreestyleCreateChangeSetFile getCreateChangeSetFile() {
        return createChangeSetFile;
    }

    @DataBoundSetter
    public void setCreateChangeSetFile(@Nullable FreestyleCreateChangeSetFile createChangeSetFile) {
        this.createChangeSetFile = createChangeSetFile;
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
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        PolarisCommandsFactory.fromPostBuild(build, launcher, listener)
            .runPolarisCliAndCheckForIssues(polarisCliName, polarisArguments, createChangeSetFile, waitForIssues);

        return true;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> implements Serializable {
        private static final long serialVersionUID = -3800519788262007744L;

        public DescriptorImpl() {
            super(PolarisBuildStep.class);
            load();
        }

        public ListBoxModel doFillPolarisCliNameItems() {
            PolarisCli.DescriptorImpl polarisCliToolInstallationDescriptor = ToolInstallation.all().get(PolarisCli.DescriptorImpl.class);

            if (polarisCliToolInstallationDescriptor == null) {
                return new ListBoxModel();
            }

            return Stream.of(polarisCliToolInstallationDescriptor.getInstallations())
                       .map(PolarisCli::getName)
                       .map(ListBoxModel.Option::new)
                       .collect(Collectors.toCollection(ListBoxModel::new));
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}

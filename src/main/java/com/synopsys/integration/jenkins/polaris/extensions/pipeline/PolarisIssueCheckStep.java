/*
 * synopsys-polaris
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.polaris.extensions.pipeline;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.synopsys.integration.jenkins.annotations.HelpMarkdown;
import com.synopsys.integration.jenkins.polaris.service.PolarisCommandsFactory;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;

public class PolarisIssueCheckStep extends Step implements Serializable {
    public static final String DISPLAY_NAME = "Check for issues in the Coverity on Polaris found by a previous execution of the CLI";
    public static final String PIPELINE_NAME = "polarisIssueCheck";
    private static final long serialVersionUID = -2698425344634481146L;

    @Nullable
    @HelpMarkdown("Check this box to return the issue count as an integer instead of throwing an exception")
    private Boolean returnIssueCount;

    @Nullable
    @HelpMarkdown("The maximum number of minutes to wait for jobs started by the Coverity on Polaris CLI to complete when executed without -w (nonblocking mode). Must be a positive integer, defaults to 30 minutes.")
    private Integer jobTimeoutInMinutes;

    @DataBoundConstructor
    public PolarisIssueCheckStep() {
        // Nothing to do-- we generally want to only use DataBoundSetters if we can, but having no DataBoundConstructor can cause issues.
        // -- rotte FEB 2020
    }

    @Nullable
    public Integer getJobTimeoutInMinutes() {
        return jobTimeoutInMinutes;
    }

    @DataBoundSetter
    public void setJobTimeoutInMinutes(Integer jobTimeoutInMinutes) {
        this.jobTimeoutInMinutes = jobTimeoutInMinutes;
    }

    @Nullable
    public Boolean getReturnIssueCount() {
        if (!Boolean.TRUE.equals(returnIssueCount)) {
            return null;
        }
        return returnIssueCount;
    }

    @DataBoundSetter
    public void setReturnIssueCount(Boolean returnIssueCount) {
        this.returnIssueCount = returnIssueCount;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(context);
    }

    @Symbol(PIPELINE_NAME)
    @Extension(optional = true)
    public static final class DescriptorImpl extends StepDescriptor {
        public DescriptorImpl() {
            // Nothing to do here, but we must provide an explicit default constructor or else some versions of the Pipeline syntax generator will break
            // -rotte FEB 2020
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return new HashSet<>(Arrays.asList(TaskListener.class, EnvVars.class, FilePath.class, Launcher.class, Run.class, Node.class));
        }

        @Override
        public String getFunctionName() {
            return PIPELINE_NAME;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }

    }

    public class Execution extends SynchronousNonBlockingStepExecution<Integer> {
        private static final long serialVersionUID = -3799159740768688972L;
        private final transient TaskListener listener;
        private final transient EnvVars envVars;
        private final transient FilePath workspace;
        private final transient Launcher launcher;
        private final transient Node node;
        private final transient Run<?, ?> run;

        protected Execution(@Nonnull StepContext context) throws InterruptedException, IOException {
            super(context);
            listener = context.get(TaskListener.class);
            envVars = context.get(EnvVars.class);
            workspace = context.get(FilePath.class);
            launcher = context.get(Launcher.class);
            node = context.get(Node.class);
            run = context.get(Run.class);
        }

        @Override
        protected Integer run() throws Exception {
            return PolarisCommandsFactory.fromPipeline(listener, envVars, launcher, node, run, workspace)
                       .checkForIssues(jobTimeoutInMinutes, returnIssueCount);
        }
    }
}

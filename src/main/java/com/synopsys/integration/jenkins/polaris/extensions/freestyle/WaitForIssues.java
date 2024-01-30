/*
 * synopsys-polaris
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.polaris.extensions.freestyle;

import javax.annotation.Nullable;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.synopsys.integration.jenkins.annotations.HelpMarkdown;
import com.synopsys.integration.jenkins.extensions.ChangeBuildStatusTo;
import com.synopsys.integration.jenkins.extensions.JenkinsSelectBoxEnum;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;

public class WaitForIssues extends AbstractDescribableImpl<WaitForIssues> {
    @Nullable
    @HelpMarkdown("The build status to set the project to if there are issues")
    private ChangeBuildStatusTo buildStatusForIssues;

    // jobTimeoutInMinutes must be a Number to guarantee identical functionality between Freestyle and Pipeline StepWorkflows that use GetTotalIssueCount.
    // -- rotte APR 2020
    @Nullable
    @HelpMarkdown("The maximum number of minutes to wait for jobs started by the Coverity on Polaris CLI to complete when the Coverity on Polaris CLI is executed without -w (nonblocking mode). Must be a positive integer, defaults to 30 minutes.")
    private Integer jobTimeoutInMinutes;

    @DataBoundConstructor
    public WaitForIssues() {
        // Nothing to do-- we generally want to only use DataBoundSetters if we can avoid it, but having no DataBoundConstructor can cause issues.
        // -- rotte FEB 2020
    }

    public ChangeBuildStatusTo getBuildStatusForIssues() {
        return buildStatusForIssues;
    }

    @DataBoundSetter
    public void setBuildStatusForIssues(final ChangeBuildStatusTo buildStatusForIssues) {
        this.buildStatusForIssues = buildStatusForIssues;
    }

    public Integer getJobTimeoutInMinutes() {
        return jobTimeoutInMinutes;
    }

    @DataBoundSetter
    public void setJobTimeoutInMinutes(final Integer jobTimeoutInMinutes) {
        this.jobTimeoutInMinutes = jobTimeoutInMinutes;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<WaitForIssues> {
        public ListBoxModel doFillBuildStatusForIssuesItems() {
            return JenkinsSelectBoxEnum.toListBoxModel(ChangeBuildStatusTo.values());
        }
    }
}

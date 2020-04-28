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
    @HelpMarkdown("The maximum number of minutes to wait for jobs started by the Polaris CLI to complete when the Polaris CLI is executed without -w (nonblocking mode). Must be a positive integer, defaults to 30 minutes.")
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

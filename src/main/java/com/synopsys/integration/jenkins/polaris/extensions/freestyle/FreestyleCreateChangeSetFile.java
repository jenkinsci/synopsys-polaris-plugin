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
import org.kohsuke.stapler.StaplerRequest;

import com.synopsys.integration.jenkins.annotations.HelpMarkdown;
import com.synopsys.integration.jenkins.extensions.ChangeBuildStatusTo;
import com.synopsys.integration.jenkins.extensions.JenkinsSelectBoxEnum;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;

public class FreestyleCreateChangeSetFile extends AbstractDescribableImpl<FreestyleCreateChangeSetFile> {
    @Nullable
    @HelpMarkdown("Specify a comma separated list of filename patterns that you would like to explicitly excluded from the Jenkins-provided SCM changeset.  \r\n"
                      + "The pattern is applied to determine which files will be populated in the changeset file, stored at $CHANGE_SET_FILE_PATH.  \r\n"
                      + "If blank, will exclude none.  \r\n"
                      + "Examples:\r\n"
                      + "\r\n"
                      + "| File Name | Pattern    | Will be excluded |\r\n"
                      + "| --------- | ---------- | ---------------- |\r\n"
                      + "| test.java | *.java     | Yes              |\r\n"
                      + "| test.java | *.jpg      | No               |\r\n"
                      + "| test.java | test.*     | Yes              |\r\n"
                      + "| test.java | test.????  | Yes              |\r\n"
                      + "| test.java | test.????? | No               |")
    private String changeSetExclusionPatterns;

    @Nullable
    @HelpMarkdown("Specify a comma separated list of filename patterns that you would like to explicitly included Jenkins-provided SCM changeset.  \r\n"
                      + "The pattern is applied to determine which files will be populated in the changeset file, stored at $CHANGE_SET_FILE_PATH.  \r\n"
                      + "If blank, will include all. \r\n"
                      + "Examples:\r\n"
                      + "\r\n"
                      + "| File Name | Pattern    | Will be included |\r\n"
                      + "| --------- | ---------- | ---------------- |\r\n"
                      + "| test.java | *.java     | Yes              |\r\n"
                      + "| test.java | *.jpg      | No               |\r\n"
                      + "| test.java | test.*     | Yes              |\r\n"
                      + "| test.java | test.????  | Yes              |\r\n"
                      + "| test.java | test.????? | No               |")
    private String changeSetInclusionPatterns;

    @Nullable
    @HelpMarkdown("The action to take when static analysis is skipped because the changeset contained no files to analyze. Defaults to \"Mark the build as Unstable\".")
    private ChangeBuildStatusTo buildStatusOnSkip;

    @DataBoundConstructor
    public FreestyleCreateChangeSetFile() {
        // do nothing
    }

    @Nullable
    public ChangeBuildStatusTo getBuildStatusOnSkip() {
        return buildStatusOnSkip;
    }

    @DataBoundSetter
    public void setBuildStatusOnSkip(ChangeBuildStatusTo buildStatusOnSkip) {
        this.buildStatusOnSkip = buildStatusOnSkip;
    }

    @Nullable
    public String getChangeSetExclusionPatterns() {
        return changeSetExclusionPatterns;
    }

    @DataBoundSetter
    public void setChangeSetExclusionPatterns(@Nullable String changeSetExclusionPatterns) {
        this.changeSetExclusionPatterns = changeSetExclusionPatterns;
    }

    @Nullable
    public String getChangeSetInclusionPatterns() {
        return changeSetInclusionPatterns;
    }

    @DataBoundSetter
    public void setChangeSetInclusionPatterns(@Nullable String changeSetInclusionPatterns) {
        this.changeSetInclusionPatterns = changeSetInclusionPatterns;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<FreestyleCreateChangeSetFile> {
        public DescriptorImpl() {
            super(FreestyleCreateChangeSetFile.class);
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }

        public ChangeBuildStatusTo getDefaultBuildStatusOnSkip() {
            return ChangeBuildStatusTo.UNSTABLE;
        }

        public ListBoxModel doFillBuildStatusOnSkipItems() {
            return JenkinsSelectBoxEnum.toListBoxModel(ChangeBuildStatusTo.values());
        }

    }

}
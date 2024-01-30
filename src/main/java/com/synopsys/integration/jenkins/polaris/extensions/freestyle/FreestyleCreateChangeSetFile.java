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

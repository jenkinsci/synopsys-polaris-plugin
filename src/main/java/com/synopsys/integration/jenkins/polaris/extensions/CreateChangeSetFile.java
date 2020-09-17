/**
 * synopsys-coverity
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
package com.synopsys.integration.jenkins.polaris.extensions;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.synopsys.integration.jenkins.ChangeSetFilter;
import com.synopsys.integration.jenkins.annotations.HelpMarkdown;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import net.sf.json.JSONObject;

public class CreateChangeSetFile extends AbstractDescribableImpl<CreateChangeSetFile> {
    @HelpMarkdown("Specify a comma separated list of filename patterns that you would like to explicitly excluded from the Jenkins change set.  \r\n"
                      + "The pattern is applied to determine which files will be populated in the change set file, stored at $CHANGE_SET_FILE_PATH.  \r\n"
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
    private final String changeSetExclusionPatterns;

    @HelpMarkdown("Specify a comma separated list of filename patterns that you would like to explicitly included from the Jenkins change set.  \r\n"
                      + "The pattern is applied to determine which files will be populated in the change set file, stored at $CHANGE_SET_FILE_PATH.  \r\n"
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
    private final String changeSetInclusionPatterns;

    @DataBoundConstructor
    public CreateChangeSetFile(String changeSetExclusionPatterns, String changeSetInclusionPatterns) {
        this.changeSetExclusionPatterns = changeSetExclusionPatterns;
        this.changeSetInclusionPatterns = changeSetInclusionPatterns;
    }

    public String getChangeSetInclusionPatterns() {
        return changeSetInclusionPatterns;
    }

    public String getChangeSetExclusionPatterns() {
        return changeSetExclusionPatterns;
    }

    public ChangeSetFilter createChangeSetFilter() {
        return new ChangeSetFilter(new JenkinsIntLogger(null), changeSetExclusionPatterns, changeSetInclusionPatterns);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<CreateChangeSetFile> {
        public DescriptorImpl() {
            super(CreateChangeSetFile.class);
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }

    }

}

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
package com.synopsys.integration.jenkins.polaris.extensions.tools;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.polaris.extensions.global.PolarisGlobalConfig;
import com.synopsys.integration.polaris.common.rest.AccessTokenPolarisHttpClient;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import jenkins.model.GlobalConfiguration;

public class PolarisCliInstaller extends ToolInstaller {
    @DataBoundConstructor
    public PolarisCliInstaller(final String label) {
        super(label);
    }

    @Override
    public FilePath performInstallation(final ToolInstallation tool, final Node node, final TaskListener log) throws IOException, InterruptedException {
        final JenkinsIntLogger jenkinsIntLogger = new JenkinsIntLogger(log);
        final PolarisGlobalConfig polarisGlobalConfig = GlobalConfiguration.all().get(PolarisGlobalConfig.class);

        if (polarisGlobalConfig == null) {
            throw new AbortException("Cannot install Polaris CLI Installation" + tool.getName() + " because no Polaris global config was found. Please check your system config.");
        }

        final VirtualChannel virtualChannel = node.getChannel();

        if (virtualChannel == null) {
            throw new AbortException("Cannot install Polaris CLI Installation" + tool.getName() + " because node " + node.getDisplayName() + " is not connected or offline");
        }

        final FilePath installLocation = preferredLocation(tool, node);
        installLocation.mkdirs();

        final AccessTokenPolarisHttpClient polarisHttpClient = polarisGlobalConfig.getPolarisServerConfig().createPolarisHttpClient(jenkinsIntLogger);
        final FindOrInstallPolarisCli findOrInstallPolarisCli = FindOrInstallPolarisCli.getConnectionDetailsFromHttpClient(jenkinsIntLogger, polarisHttpClient, installLocation.getRemote());

        try {
            final String polarisCliRemotePath = virtualChannel.call(findOrInstallPolarisCli);
            return new FilePath(virtualChannel, polarisCliRemotePath);
        } catch (final IntegrationException ex) {
            throw new IOException("Polaris CLI was not correctly installed.", ex);
        }
    }

    @Extension
    public static final class DescriptorImpl extends ToolInstallerDescriptor<PolarisCliInstaller> {
        @Override
        public String getDisplayName() {
            return "Install from Polaris";
        }

        @Override
        public boolean isApplicable(final Class<? extends ToolInstallation> toolType) {
            return toolType == PolarisCli.class;
        }
    }

}

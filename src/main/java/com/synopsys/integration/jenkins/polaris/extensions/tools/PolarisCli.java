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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.ArrayUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;

public class PolarisCli extends ToolInstallation implements NodeSpecific<PolarisCli>, EnvironmentSpecific<PolarisCli> {
    private static final long serialVersionUID = -3838254855454518440L;

    @DataBoundConstructor
    public PolarisCli(final String name, final String home, final List<? extends ToolProperty<?>> properties) {
        super(Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(home), properties);
    }

    public static boolean installationsExist() {
        return Optional.ofNullable(ToolInstallation.all().get(PolarisCli.DescriptorImpl.class))
                   .map(PolarisCli.DescriptorImpl::getInstallations)
                   .filter(ArrayUtils::isNotEmpty)
                   .isPresent();
    }

    public static Optional<PolarisCli> findInstallationWithName(final String polarisCliName) {
        final ToolDescriptor<PolarisCli> toolDescriptor = ToolInstallation.all().get(PolarisCli.DescriptorImpl.class);

        if (toolDescriptor == null) {
            return Optional.empty();
        }

        return Stream.of(toolDescriptor.getInstallations())
                   .filter(installation -> installation.getName().equals(polarisCliName))
                   .findFirst();
    }

    @Override
    public PolarisCli forNode(@Nonnull final Node node, final TaskListener log) throws IOException, InterruptedException {
        return new PolarisCli(getName(), translateFor(node, log), getProperties().toList());
    }

    @Override
    public PolarisCli forEnvironment(final EnvVars environment) {
        return new PolarisCli(getName(), environment.expand(getHome()), getProperties().toList());
    }

    @Override
    public void buildEnvVars(final EnvVars env) {
        // This is what the gradle plugin does, so it's probably good enough for us --rotte (JAN 2020)
        env.putIfNotNull("PATH+POLARIS", getHome() + "/bin");
    }

    @Extension
    @Symbol("polarisCli")
    public static final class DescriptorImpl extends ToolDescriptor<PolarisCli> {
        @Override
        public String getDisplayName() {
            return "Polaris CLI";
        }

        @Override
        public PolarisCli[] getInstallations() {
            load();
            return super.getInstallations();
        }

        @Override
        public void setInstallations(final PolarisCli... installations) {
            super.setInstallations(installations);
            save();
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new PolarisCliInstaller(null));
        }
    }

}
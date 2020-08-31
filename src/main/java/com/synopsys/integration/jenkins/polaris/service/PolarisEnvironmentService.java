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
package com.synopsys.integration.jenkins.polaris.service;

import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.polaris.extensions.global.PolarisGlobalConfig;
import com.synopsys.integration.jenkins.wrapper.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.wrapper.JenkinsVersionHelper;
import com.synopsys.integration.jenkins.wrapper.SynopsysCredentialsHelper;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfigBuilder;
import com.synopsys.integration.polaris.common.exception.PolarisIntegrationException;
import com.synopsys.integration.util.IntEnvironmentVariables;

import jenkins.model.GlobalConfiguration;

public class PolarisEnvironmentService {
    private final JenkinsIntLogger logger;
    private final SynopsysCredentialsHelper credentialsHelper;
    private final JenkinsProxyHelper proxyHelper;
    private final Map<String, String> environmentVariables;
    private final JenkinsVersionHelper versionHelper;

    public PolarisEnvironmentService(JenkinsIntLogger logger, JenkinsVersionHelper versionHelper, SynopsysCredentialsHelper credentialsHelper, JenkinsProxyHelper proxyHelper, Map<String, String> environmentVariables) {
        this.logger = logger;
        this.versionHelper = versionHelper;
        this.credentialsHelper = credentialsHelper;
        this.proxyHelper = proxyHelper;
        this.environmentVariables = environmentVariables;
    }

    public IntEnvironmentVariables createPolarisEnvironment() throws PolarisIntegrationException {
        IntEnvironmentVariables intEnvironmentVariables = IntEnvironmentVariables.empty();
        intEnvironmentVariables.putAll(environmentVariables);

        PolarisGlobalConfig polarisGlobalConfig = GlobalConfiguration.all().get(PolarisGlobalConfig.class);
        if (polarisGlobalConfig == null) {
            throw new PolarisIntegrationException("No Polaris system configuration could be found, please check your system configuration.");
        }

        PolarisServerConfigBuilder polarisServerConfigBuilder = polarisGlobalConfig.getPolarisServerConfigBuilder(credentialsHelper, proxyHelper);

        polarisServerConfigBuilder.getProperties()
            .forEach((builderPropertyKey, propertyValue) -> acceptIfNotNull(intEnvironmentVariables::put, builderPropertyKey.getKey(), propertyValue));

        try {
            polarisServerConfigBuilder.build().populateEnvironmentVariables(intEnvironmentVariables::put);
        } catch (IllegalArgumentException ex) {
            throw new PolarisIntegrationException("There is a problem with your Polaris system configuration", ex);
        }

        String logMessage = versionHelper.getPluginVersion("synopsys-polaris")
                                .map(version -> String.format("Running Synopsys Polaris for Jenkins version %s", version))
                                .orElse("Running Synopsys Polaris for Jenkins");
        logger.info(logMessage);

        return intEnvironmentVariables;
    }

    private void acceptIfNotNull(BiConsumer<String, String> environmentPutter, String key, String value) {
        if (StringUtils.isNoneBlank(key, value)) {
            environmentPutter.accept(key, value);
        }
    }
}

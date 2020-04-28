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
package com.synopsys.integration.jenkins.polaris.workflow;

import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.jenkins.JenkinsVersionHelper;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.polaris.extensions.global.PolarisGlobalConfig;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfigBuilder;
import com.synopsys.integration.polaris.common.exception.PolarisIntegrationException;
import com.synopsys.integration.stepworkflow.AbstractExecutingSubStep;
import com.synopsys.integration.stepworkflow.SubStepResponse;
import com.synopsys.integration.util.IntEnvironmentVariables;

import jenkins.model.GlobalConfiguration;

public class CreatePolarisEnvironment extends AbstractExecutingSubStep {
    private final JenkinsIntLogger logger;
    private final IntEnvironmentVariables intEnvironmentVariables;

    public CreatePolarisEnvironment(final JenkinsIntLogger logger, final IntEnvironmentVariables intEnvironmentVariables) {
        this.logger = logger;
        this.intEnvironmentVariables = intEnvironmentVariables;
    }

    @Override
    public SubStepResponse<Object> run() {
        final PolarisGlobalConfig polarisGlobalConfig = GlobalConfiguration.all().get(PolarisGlobalConfig.class);
        if (polarisGlobalConfig == null) {
            return SubStepResponse.FAILURE(new PolarisIntegrationException("No Polaris system configuration could be found, please check your system configuration."));
        }

        final PolarisServerConfigBuilder polarisServerConfigBuilder = polarisGlobalConfig.getPolarisServerConfigBuilder();

        polarisServerConfigBuilder.getProperties()
            .forEach((builderPropertyKey, propertyValue) -> acceptIfNotNull(intEnvironmentVariables::put, builderPropertyKey.getKey(), propertyValue));

        try {
            polarisServerConfigBuilder.build().populateEnvironmentVariables(intEnvironmentVariables::put);
        } catch (final IllegalArgumentException ex) {
            return SubStepResponse.FAILURE(new PolarisIntegrationException("There is a problem with your Polaris system configuration", ex));
        }

        final String logMessage = JenkinsVersionHelper.getPluginVersion("synopsys-polaris")
                                      .map(version -> String.format("Running Synopsys Polaris for Jenkins version %s", version))
                                      .orElse("Running Synopsys Polaris for Jenkins");
        logger.info(logMessage);

        return SubStepResponse.SUCCESS();
    }

    private void acceptIfNotNull(final BiConsumer<String, String> environmentPutter, final String key, final String value) {
        if (StringUtils.isNoneBlank(key, value)) {
            environmentPutter.accept(key, value);
        }
    }
}

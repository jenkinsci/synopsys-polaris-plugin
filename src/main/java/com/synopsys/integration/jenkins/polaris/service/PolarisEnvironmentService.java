/*
 * synopsys-polaris
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.polaris.service;

import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.jenkins.polaris.PolarisJenkinsEnvironmentVariable;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfigBuilder;
import com.synopsys.integration.polaris.common.exception.PolarisIntegrationException;
import com.synopsys.integration.util.IntEnvironmentVariables;

public class PolarisEnvironmentService {
    private final Map<String, String> environmentVariables;

    public PolarisEnvironmentService(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public IntEnvironmentVariables createPolarisEnvironment(String changeSetFileRemotePath, PolarisServerConfigBuilder polarisServerConfigBuilder) throws PolarisIntegrationException {
        IntEnvironmentVariables intEnvironmentVariables = IntEnvironmentVariables.empty();
        intEnvironmentVariables.putAll(environmentVariables);

        if (StringUtils.isNotBlank(changeSetFileRemotePath)) {
            intEnvironmentVariables.put(PolarisJenkinsEnvironmentVariable.CHANGE_SET_FILE_PATH.stringValue(), changeSetFileRemotePath);
        }

        polarisServerConfigBuilder.getProperties()
            .forEach((builderPropertyKey, propertyValue) -> acceptIfNotNull(intEnvironmentVariables::put, builderPropertyKey.getKey(), propertyValue));

        try {
            polarisServerConfigBuilder.build().populateEnvironmentVariables(intEnvironmentVariables::put);
        } catch (IllegalArgumentException ex) {
            throw new PolarisIntegrationException("There is a problem with your Polaris system configuration", ex);
        }

        return intEnvironmentVariables;
    }

    public IntEnvironmentVariables getInitialEnvironment() {
        IntEnvironmentVariables intEnvironmentVariables = IntEnvironmentVariables.empty();

        intEnvironmentVariables.putAll(environmentVariables);

        return intEnvironmentVariables;
    }

    private void acceptIfNotNull(BiConsumer<String, String> environmentPutter, String key, String value) {
        if (StringUtils.isNoneBlank(key, value)) {
            environmentPutter.accept(key, value);
        }
    }
}

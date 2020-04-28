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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.polaris.common.cli.PolarisDownloadUtility;
import com.synopsys.integration.polaris.common.exception.PolarisIntegrationException;
import com.synopsys.integration.polaris.common.rest.AccessTokenPolarisHttpClient;
import com.synopsys.integration.rest.client.IntHttpClient;
import com.synopsys.integration.rest.credentials.CredentialsBuilder;
import com.synopsys.integration.rest.proxy.ProxyInfo;
import com.synopsys.integration.rest.proxy.ProxyInfoBuilder;
import com.synopsys.integration.util.CleanupZipExpander;
import com.synopsys.integration.util.OperatingSystemType;

import jenkins.security.MasterToSlaveCallable;

public class FindOrInstallPolarisCli extends MasterToSlaveCallable<String, IntegrationException> {
    private static final long serialVersionUID = 6457474109970149144L;
    private final JenkinsIntLogger jenkinsIntLogger;
    private final String polarisServerUrl;
    private final int timeout;
    private final String proxyHost;
    private final int proxyPort;
    private final String proxyUsername;
    private final String proxyPassword;
    private final String proxyNtlmDomain;
    private final String proxyNtlmWorkstation;
    private final String installationLocation;

    public FindOrInstallPolarisCli(final JenkinsIntLogger jenkinsIntLogger, final String polarisServerUrl, final int timeout, final String proxyHost, final int proxyPort,
        final String proxyUsername, final String proxyPassword, final String proxyNtlmDomain, final String proxyNtlmWorkstation, final String installationLocation) {
        this.jenkinsIntLogger = jenkinsIntLogger;
        this.polarisServerUrl = polarisServerUrl;
        this.timeout = timeout;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUsername = proxyUsername;
        this.proxyPassword = proxyPassword;
        this.proxyNtlmDomain = proxyNtlmDomain;
        this.proxyNtlmWorkstation = proxyNtlmWorkstation;
        this.installationLocation = installationLocation;
    }

    // We'd love to just pass the httpclient through but it's not serializable and it's probably not worthwhile to make a serializable class just for this -- rotte DEC 2019
    public static FindOrInstallPolarisCli getConnectionDetailsFromHttpClient(final JenkinsIntLogger jenkinsIntLogger, final AccessTokenPolarisHttpClient accessTokenPolarisHttpClient, final String installationLocation) {
        final ProxyInfo proxyInfo = accessTokenPolarisHttpClient.getProxyInfo();
        return new FindOrInstallPolarisCli(
            jenkinsIntLogger,
            accessTokenPolarisHttpClient.getPolarisServerUrl(),
            accessTokenPolarisHttpClient.getTimeoutInSeconds(),
            proxyInfo.getHost().orElse(null),
            proxyInfo.getPort(),
            proxyInfo.getUsername().orElse(null),
            proxyInfo.getPassword().orElse(null),
            proxyInfo.getNtlmDomain().orElse(null),
            proxyInfo.getNtlmWorkstation().orElse(null),
            installationLocation);
    }

    @Override
    public String call() throws IntegrationException {
        try {
            final File installLocation = new File(installationLocation);
            final OperatingSystemType operatingSystemType = OperatingSystemType.determineFromSystem();
            final CredentialsBuilder credentialsBuilder = new CredentialsBuilder();
            credentialsBuilder.setUsernameAndPassword(proxyUsername, proxyPassword);

            final ProxyInfoBuilder proxyInfoBuilder = new ProxyInfoBuilder();
            proxyInfoBuilder.setCredentials(credentialsBuilder.build());
            proxyInfoBuilder.setHost(proxyHost);
            proxyInfoBuilder.setPort(proxyPort);
            proxyInfoBuilder.setNtlmDomain(proxyNtlmDomain);
            proxyInfoBuilder.setNtlmDomain(proxyNtlmWorkstation);

            final IntHttpClient intHttpClient = new IntHttpClient(jenkinsIntLogger, timeout, false, proxyInfoBuilder.build());
            final CleanupZipExpander cleanupZipExpander = new CleanupZipExpander(jenkinsIntLogger);

            Files.createDirectories(installLocation.toPath());

            final PolarisDownloadUtility polarisDownloadUtility = new PolarisDownloadUtility(jenkinsIntLogger, operatingSystemType, intHttpClient, cleanupZipExpander, polarisServerUrl, installLocation);

            return polarisDownloadUtility.getOrDownloadPolarisCliHome().orElseThrow(() -> new PolarisIntegrationException("The Polaris CLI could not be found or installed correctly."));
        } catch (final IOException | IllegalArgumentException ex) {
            throw new PolarisIntegrationException(ex);
        }
    }

}

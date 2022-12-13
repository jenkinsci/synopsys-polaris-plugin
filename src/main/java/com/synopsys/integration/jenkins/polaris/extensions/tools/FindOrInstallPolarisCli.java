/*
 * synopsys-polaris
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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
import com.synopsys.integration.rest.HttpUrl;
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

    public FindOrInstallPolarisCli(JenkinsIntLogger jenkinsIntLogger, String polarisServerUrl, int timeout, String proxyHost, int proxyPort,
        String proxyUsername, String proxyPassword, String proxyNtlmDomain, String proxyNtlmWorkstation, String installationLocation) {
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
    public static FindOrInstallPolarisCli getConnectionDetailsFromHttpClient(JenkinsIntLogger jenkinsIntLogger, AccessTokenPolarisHttpClient accessTokenPolarisHttpClient, String installationLocation) {
        ProxyInfo proxyInfo = accessTokenPolarisHttpClient.getProxyInfo();
        return new FindOrInstallPolarisCli(
            jenkinsIntLogger,
            accessTokenPolarisHttpClient.getPolarisServerUrl().string(),
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
            File installLocation = new File(installationLocation);
            OperatingSystemType operatingSystemType = OperatingSystemType.determineFromSystem();
            CredentialsBuilder credentialsBuilder = new CredentialsBuilder();
            credentialsBuilder.setUsernameAndPassword(proxyUsername, proxyPassword);

            ProxyInfoBuilder proxyInfoBuilder = new ProxyInfoBuilder();
            proxyInfoBuilder.setCredentials(credentialsBuilder.build());
            proxyInfoBuilder.setHost(proxyHost);
            proxyInfoBuilder.setPort(proxyPort);
            proxyInfoBuilder.setNtlmDomain(proxyNtlmDomain);
            proxyInfoBuilder.setNtlmDomain(proxyNtlmWorkstation);

            IntHttpClient intHttpClient = new IntHttpClient(jenkinsIntLogger, timeout, false, proxyInfoBuilder.build());
            CleanupZipExpander cleanupZipExpander = new CleanupZipExpander(jenkinsIntLogger);

            Files.createDirectories(installLocation.toPath());

            PolarisDownloadUtility polarisDownloadUtility = new PolarisDownloadUtility(jenkinsIntLogger, operatingSystemType, intHttpClient, cleanupZipExpander, new HttpUrl(polarisServerUrl), installLocation);

            return polarisDownloadUtility.getOrDownloadPolarisCliHome().orElseThrow(() -> new PolarisIntegrationException("The Polaris CLI could not be found or installed correctly."));
        } catch (IOException | IllegalArgumentException ex) {
            throw new PolarisIntegrationException(ex);
        }
    }

}

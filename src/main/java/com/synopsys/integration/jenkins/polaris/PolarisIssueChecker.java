/*
 * synopsys-polaris
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.polaris;

import java.io.IOException;
import java.util.Optional;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.polaris.service.GetPolarisCliResponseContent;
import com.synopsys.integration.jenkins.polaris.service.PolarisCliIssueCountService;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.jenkins.wrapper.JenkinsVersionHelper;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.polaris.common.service.JobService;

public class PolarisIssueChecker {
    private final IntLogger logger;
    private final PolarisCliIssueCountService polarisCliIssueCountService;
    private final JenkinsRemotingService jenkinsRemotingService;
    private final JenkinsVersionHelper versionHelper;

    public PolarisIssueChecker(IntLogger logger, PolarisCliIssueCountService polarisCliIssueCountService, JenkinsRemotingService jenkinsRemotingService, JenkinsVersionHelper versionHelper) {
        this.logger = logger;
        this.polarisCliIssueCountService = polarisCliIssueCountService;
        this.jenkinsRemotingService = jenkinsRemotingService;
        this.versionHelper = versionHelper;
    }

    public int getPolarisIssueCount(Integer jobTimeoutInMinutes) throws IOException, InterruptedException, IntegrationException {
        String logMessage = versionHelper.getPluginVersion("synopsys-polaris")
                                .map(version -> String.format("Running Polaris Software Integrity Platform for Jenkins version %s", version))
                                .orElse("Running Polaris Software Integrity Platform for Jenkins");
        logger.info(logMessage);

        Long jobTimeoutInSeconds = Optional.ofNullable(jobTimeoutInMinutes)
                                       .map(value -> value * 60L)
                                       .orElse(JobService.DEFAULT_TIMEOUT);

        String cliCommonResponseModelJson = jenkinsRemotingService.call(new GetPolarisCliResponseContent(jenkinsRemotingService.getRemoteWorkspacePath()));

        return polarisCliIssueCountService.getIssueCount(jobTimeoutInSeconds, cliCommonResponseModelJson);
    }
}

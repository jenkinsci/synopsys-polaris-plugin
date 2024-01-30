/*
 * synopsys-polaris
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.polaris.service;

import java.util.Optional;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.exception.JenkinsUserFriendlyException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.polaris.common.cli.PolarisCliResponseUtility;
import com.synopsys.integration.polaris.common.cli.model.CliCommonResponseModel;
import com.synopsys.integration.polaris.common.cli.model.CommonIssueSummary;
import com.synopsys.integration.polaris.common.cli.model.CommonScanInfo;
import com.synopsys.integration.polaris.common.cli.model.CommonToolInfo;
import com.synopsys.integration.polaris.common.service.CountService;
import com.synopsys.integration.polaris.common.service.JobService;
import com.synopsys.integration.rest.HttpUrl;

public class PolarisCliIssueCountService {
    public static final String STEP_EXCEPTION_PREFIX = "Issue count for most recent Polaris Software Integrity Platform analysis could not be determined: ";
    private final JenkinsIntLogger logger;
    private final CountService countService;
    private final JobService jobService;
    private final PolarisCliResponseUtility polarisCliResponseUtility;

    public PolarisCliIssueCountService(JenkinsIntLogger logger, CountService countService, JobService jobService, PolarisCliResponseUtility polarisCliResponseUtility) {
        this.logger = logger;
        this.countService = countService;
        this.jobService = jobService;
        this.polarisCliResponseUtility = polarisCliResponseUtility;
    }

    public Integer getIssueCount(long jobTimeoutInSeconds, String cliCommonResponseModelJson) throws IntegrationException, JenkinsUserFriendlyException, InterruptedException {
        CliCommonResponseModel polarisCliResponseModel = polarisCliResponseUtility.getPolarisCliResponseModelFromString(cliCommonResponseModelJson);

        Optional<CommonIssueSummary> issueSummary = polarisCliResponseModel.getIssueSummary();
        CommonScanInfo scanInfo = polarisCliResponseModel.getScanInfo();

        if (issueSummary.isPresent()) {
            logger.debug("Found total issue count in cli-scan.json, scan must have been run with -w");
            return issueSummary.get().getTotalIssueCount();
        }

        if (jobTimeoutInSeconds < 1) {
            throw new JenkinsUserFriendlyException(STEP_EXCEPTION_PREFIX + "Job timeout must be a positive integer if the Polaris CLI is being run without -w");
        }

        HttpUrl issueApiUrl = Optional.ofNullable(scanInfo)
                                  .map(CommonScanInfo::getIssueApiUrl)
                                  .orElseThrow(() -> new JenkinsUserFriendlyException(
                                      "Polaris Software Integrity Platform for Jenkins cannot find the total issue count or issue api url in the cli-scan.json. Please ensure that you are using a supported version of the Polaris CLI."
                                  ));

        logger.debug("Found issue api url, polling for job status");

        for (CommonToolInfo tool : polarisCliResponseModel.getTools()) {
            HttpUrl jobStatusUrl = tool.getJobStatusUrl();
            if (jobStatusUrl == null) {
                throw new JenkinsUserFriendlyException(STEP_EXCEPTION_PREFIX + "tool with name " + tool.getToolName() + " has no jobStatusUrl");
            }
            jobService.waitForJobStateIsCompletedOrDieByUrl(jobStatusUrl, jobTimeoutInSeconds, JobService.DEFAULT_WAIT_INTERVAL);
        }

        return countService.getTotalIssueCountFromIssueApiUrl(issueApiUrl);
    }

}

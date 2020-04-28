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

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.polaris.common.cli.PolarisCliResponseUtility;
import com.synopsys.integration.polaris.common.cli.model.CliCommonResponseModel;
import com.synopsys.integration.polaris.common.cli.model.CommonIssueSummary;
import com.synopsys.integration.polaris.common.cli.model.CommonScanInfo;
import com.synopsys.integration.polaris.common.cli.model.CommonToolInfo;
import com.synopsys.integration.polaris.common.exception.PolarisIntegrationException;
import com.synopsys.integration.polaris.common.service.CountService;
import com.synopsys.integration.polaris.common.service.JobService;
import com.synopsys.integration.stepworkflow.SubStep;
import com.synopsys.integration.stepworkflow.SubStepResponse;

public class GetTotalIssueCount implements SubStep<String, Integer> {
    public static final String STEP_EXCEPTION_PREFIX = "Issue count for most recent Polaris Analysis could not be determined: ";
    private final JenkinsIntLogger logger;
    private final CountService countService;
    private final JobService jobService;
    private final long jobTimeoutInSeconds;

    public GetTotalIssueCount(final JenkinsIntLogger logger, final CountService countService, final JobService jobService, final long jobTimeoutInSeconds) {
        this.logger = logger;
        this.countService = countService;
        this.jobService = jobService;
        this.jobTimeoutInSeconds = jobTimeoutInSeconds;
    }

    @Override
    public SubStepResponse<Integer> run(final SubStepResponse<? extends String> previousResponse) {
        if (previousResponse.isFailure() || !previousResponse.hasData()) {
            return SubStepResponse.FAILURE(previousResponse);
        }

        final PolarisCliResponseUtility polarisCliResponseUtility = PolarisCliResponseUtility.defaultUtility(logger);
        final String rawJson = previousResponse.getData();
        try {
            final CliCommonResponseModel polarisCliResponseModel = polarisCliResponseUtility.getPolarisCliResponseModelFromString(rawJson);

            final Optional<CommonIssueSummary> issueSummary = polarisCliResponseModel.getIssueSummary();
            final CommonScanInfo scanInfo = polarisCliResponseModel.getScanInfo();

            if (issueSummary.isPresent()) {
                logger.debug("Found total issue count in cli-scan.json, scan must have been run with -w");
                return SubStepResponse.SUCCESS(issueSummary.get().getTotalIssueCount());
            }

            if (jobTimeoutInSeconds < 1) {
                throw new PolarisIntegrationException(STEP_EXCEPTION_PREFIX + "Job timeout must be a positive integer if the Polaris CLI is being run without -w");
            }

            final String issueApiUrl = Optional.ofNullable(scanInfo)
                                           .map(CommonScanInfo::getIssueApiUrl)
                                           .filter(StringUtils::isNotBlank)
                                           .orElseThrow(() -> new PolarisIntegrationException(
                                               "Synopsys Polaris for Jenkins cannot find the total issue count or issue api url in the cli-scan.json. Please ensure that you are using a supported version of the Polaris CLI."
                                           ));

            logger.debug("Found issue api url, polling for job status");

            for (final CommonToolInfo tool : polarisCliResponseModel.getTools()) {
                final String jobStatusUrl = tool.getJobStatusUrl();
                if (jobStatusUrl == null) {
                    throw new PolarisIntegrationException(STEP_EXCEPTION_PREFIX + "tool with name " + tool.getToolName() + " has no jobStatusUrl");
                }
                jobService.waitForJobStateIsCompletedOrDieByUrl(jobStatusUrl, jobTimeoutInSeconds, JobService.DEFAULT_WAIT_INTERVAL);
            }

            final Integer totalIssues = countService.getTotalIssueCountFromIssueApiUrl(issueApiUrl);

            return SubStepResponse.SUCCESS(totalIssues);

        } catch (final InterruptedException | IntegrationException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return SubStepResponse.FAILURE(e);
        }
    }

}

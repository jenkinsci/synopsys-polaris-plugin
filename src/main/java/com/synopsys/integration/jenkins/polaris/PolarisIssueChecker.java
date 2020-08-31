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
package com.synopsys.integration.jenkins.polaris;

import java.io.IOException;
import java.util.Optional;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.polaris.service.GetPolarisCliResponseContent;
import com.synopsys.integration.jenkins.polaris.service.PolarisCliIssueCountService;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.polaris.common.service.JobService;

public class PolarisIssueChecker {
    private final PolarisCliIssueCountService polarisCliIssueCountService;
    private final JenkinsRemotingService jenkinsRemotingService;

    public PolarisIssueChecker(PolarisCliIssueCountService polarisCliIssueCountService, JenkinsRemotingService jenkinsRemotingService) {
        this.polarisCliIssueCountService = polarisCliIssueCountService;
        this.jenkinsRemotingService = jenkinsRemotingService;
    }

    public int getPolarisIssueCount(Integer jobTimeoutInMinutes) throws IOException, InterruptedException, IntegrationException {
        Long jobTimeoutInSeconds = Optional.ofNullable(jobTimeoutInMinutes)
                                       .map(value -> value * 60L)
                                       .orElse(JobService.DEFAULT_TIMEOUT);

        String cliCommonResponseModelJson = jenkinsRemotingService.call(new GetPolarisCliResponseContent(jenkinsRemotingService.getRemoteWorkspacePath()));

        return polarisCliIssueCountService.getIssueCount(jobTimeoutInSeconds, cliCommonResponseModelJson);
    }
}

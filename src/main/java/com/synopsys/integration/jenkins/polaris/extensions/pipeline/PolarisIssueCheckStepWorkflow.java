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
package com.synopsys.integration.jenkins.polaris.extensions.pipeline;

import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.polaris.workflow.PolarisJenkinsStepWorkflow;
import com.synopsys.integration.jenkins.polaris.workflow.PolarisWorkflowStepFactory;
import com.synopsys.integration.polaris.common.exception.PolarisIntegrationException;
import com.synopsys.integration.polaris.common.service.PolarisServicesFactory;
import com.synopsys.integration.stepworkflow.StepWorkflow;
import com.synopsys.integration.stepworkflow.StepWorkflowResponse;

import hudson.AbortException;

public class PolarisIssueCheckStepWorkflow extends PolarisJenkinsStepWorkflow<Integer> {
    private final Integer jobTimeoutInMinutes;
    private final Boolean returnIssueCount;
    private final PolarisWorkflowStepFactory polarisWorkflowStepFactory;

    public PolarisIssueCheckStepWorkflow(final PolarisWorkflowStepFactory polarisWorkflowStepFactory, final JenkinsIntLogger logger, final PolarisServicesFactory polarisServicesFactory, final Integer jobTimeoutInMinutes,
        final Boolean returnIssueCount) {
        super(logger, polarisServicesFactory);
        this.jobTimeoutInMinutes = jobTimeoutInMinutes;
        this.returnIssueCount = returnIssueCount;
        this.polarisWorkflowStepFactory = polarisWorkflowStepFactory;
    }

    @Override
    public StepWorkflow<Integer> buildWorkflow() throws AbortException {
        return StepWorkflow.first(polarisWorkflowStepFactory.createStepGetPolarisCliResponseContent())
                   .then(polarisWorkflowStepFactory.createStepGetTotalIssueCount(jobTimeoutInMinutes))
                   .build();
    }

    public Integer handleResponse(final JenkinsIntLogger logger, final StepWorkflowResponse<Integer> stepWorkflowResponse) throws Exception {
        final Integer numberOfIssues = stepWorkflowResponse.getDataOrThrowException();
        if (numberOfIssues > 0) {
            final String defectMessage = String.format("[Polaris] Found %s total issues.", numberOfIssues);
            if (Boolean.TRUE.equals(returnIssueCount)) {
                logger.error(defectMessage);
            } else {
                throw new PolarisIntegrationException(defectMessage);
            }
        }

        return numberOfIssues;
    }

    @Override
    protected void validate() throws AbortException {
        // nothing to validate
    }
}

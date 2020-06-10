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

import com.synopsys.integration.jenkins.JenkinsVersionHelper;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.phonehome.request.PhoneHomeRequestBody;
import com.synopsys.integration.phonehome.request.PhoneHomeRequestBodyBuilder;
import com.synopsys.integration.phonehome.request.PolarisPhoneHomeRequestFactory;
import com.synopsys.integration.polaris.common.api.auth.model.Context;
import com.synopsys.integration.polaris.common.api.auth.model.ContextAttributes;
import com.synopsys.integration.polaris.common.rest.AccessTokenPolarisHttpClient;
import com.synopsys.integration.polaris.common.service.ContextsService;
import com.synopsys.integration.polaris.common.service.PolarisServicesFactory;
import com.synopsys.integration.stepworkflow.jenkins.JenkinsStepWorkflow;

public abstract class PolarisJenkinsStepWorkflow<T> extends JenkinsStepWorkflow<T> {
    protected final PolarisServicesFactory polarisServicesFactory;

    public PolarisJenkinsStepWorkflow(JenkinsIntLogger jenkinsIntLogger, JenkinsVersionHelper jenkinsVersionHelper, PolarisServicesFactory polarisServicesFactory) {
        super(jenkinsIntLogger, jenkinsVersionHelper);
        this.polarisServicesFactory = polarisServicesFactory;
    }

    protected PhoneHomeRequestBodyBuilder createPhoneHomeBuilder() {
        PolarisPhoneHomeRequestFactory polarisPhoneHomeRequestFactory = new PolarisPhoneHomeRequestFactory("synopsys-polaris-plugin");
        AccessTokenPolarisHttpClient accessTokenPolarisHttpClient = polarisServicesFactory.getHttpClient();
        ContextsService contextsService = polarisServicesFactory.createContextsService();
        String organizationName;
        try {
            organizationName = contextsService.getCurrentContext()
                                   .map(Context::getAttributes)
                                   .map(ContextAttributes::getOrganizationname)
                                   .orElse(PhoneHomeRequestBody.UNKNOWN_FIELD_VALUE);
        } catch (Exception ex) {
            organizationName = PhoneHomeRequestBody.UNKNOWN_FIELD_VALUE;
        }
        return polarisPhoneHomeRequestFactory.create(organizationName, accessTokenPolarisHttpClient.getPolarisServerUrl(), () -> jenkinsVersionHelper.getPluginVersion("synopsys-polaris"), Optional::empty);
    }

}

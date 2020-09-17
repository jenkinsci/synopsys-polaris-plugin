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
package com.synopsys.integration.jenkins.polaris.service;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.impl.client.HttpClientBuilder;

import com.google.gson.Gson;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.wrapper.JenkinsVersionHelper;
import com.synopsys.integration.phonehome.PhoneHomeClient;
import com.synopsys.integration.phonehome.PhoneHomeResponse;
import com.synopsys.integration.phonehome.PhoneHomeService;
import com.synopsys.integration.phonehome.request.PhoneHomeRequestBody;
import com.synopsys.integration.phonehome.request.PhoneHomeRequestBodyBuilder;
import com.synopsys.integration.polaris.common.api.PolarisResource;
import com.synopsys.integration.polaris.common.api.model.ContextAttributes;
import com.synopsys.integration.polaris.common.rest.AccessTokenPolarisHttpClient;
import com.synopsys.integration.polaris.common.service.ContextsService;

public class PolarisPhoneHomeService {
    private final JenkinsIntLogger logger;
    private final JenkinsVersionHelper jenkinsVersionHelper;
    private final ContextsService contextsService;
    private final AccessTokenPolarisHttpClient accessTokenPolarisHttpClient;

    public PolarisPhoneHomeService(JenkinsIntLogger logger, JenkinsVersionHelper jenkinsVersionHelper, ContextsService contextsService,
        AccessTokenPolarisHttpClient accessTokenPolarisHttpClient) {
        this.logger = logger;
        this.jenkinsVersionHelper = jenkinsVersionHelper;
        this.contextsService = contextsService;
        this.accessTokenPolarisHttpClient = accessTokenPolarisHttpClient;
    }

    public Optional<PhoneHomeResponse> phoneHome() {
        try {
            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
            Gson gson = new Gson();
            PhoneHomeClient phoneHomeClient = new PhoneHomeClient(logger, httpClientBuilder, gson);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            PhoneHomeService phoneHomeService = PhoneHomeService.createAsynchronousPhoneHomeService(logger, phoneHomeClient, executor);

            PhoneHomeRequestBody phoneHomeRequestBody = buildPhoneHomeRequest();

            return Optional.ofNullable(phoneHomeService.phoneHome(phoneHomeRequestBody));
        } catch (Exception e) {
            logger.trace("Phone home failed due to an unexpected exception:", e);
        }

        return Optional.empty();
    }

    private PhoneHomeRequestBody buildPhoneHomeRequest() {
        String organizationName;
        try {
            organizationName = contextsService.getCurrentContext()
                                   .map(PolarisResource::getAttributes)
                                   .map(ContextAttributes::getOrganizationname)
                                   .orElse(PhoneHomeRequestBody.UNKNOWN_FIELD_VALUE);
        } catch (Exception ex) {
            organizationName = PhoneHomeRequestBody.UNKNOWN_FIELD_VALUE;
        }

        PhoneHomeRequestBodyBuilder phoneHomeRequestBodyBuilder = PhoneHomeRequestBodyBuilder.createForPolaris("synopsys-polaris-plugin",
            organizationName,
            accessTokenPolarisHttpClient.getPolarisServerUrl().string(),
            jenkinsVersionHelper.getPluginVersion("synopsys-polaris").orElse(PhoneHomeRequestBody.UNKNOWN_FIELD_VALUE),
            PhoneHomeRequestBody.UNKNOWN_FIELD_VALUE);

        jenkinsVersionHelper.getJenkinsVersion()
            .ifPresent(jenkinsVersionString -> phoneHomeRequestBodyBuilder.addToMetaData("jenkins.version", jenkinsVersionString));

        return phoneHomeRequestBodyBuilder.build();
    }

}

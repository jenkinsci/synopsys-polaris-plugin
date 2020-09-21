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
package com.synopsys.integration.polaris.common.service;

import com.google.gson.Gson;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.polaris.common.request.PolarisRequestFactory;
import com.synopsys.integration.polaris.common.rest.AccessTokenPolarisHttpClient;

public class PolarisServicesFactory {
    private final IntLogger logger;
    private final AccessTokenPolarisHttpClient httpClient;
    private final Gson gson;
    private final PolarisJsonTransformer polarisJsonTransformer;
    private int defaultPageSize;

    public PolarisServicesFactory(IntLogger logger, AccessTokenPolarisHttpClient httpClient, Gson gson) {
        this.logger = logger;
        this.httpClient = httpClient;
        this.gson = gson;
        this.polarisJsonTransformer = new PolarisJsonTransformer(gson, logger);
        this.defaultPageSize = PolarisRequestFactory.DEFAULT_LIMIT;
    }

    public PolarisService createPolarisService() {
        return new PolarisService(httpClient, polarisJsonTransformer, defaultPageSize);
    }

    public JobService createJobService() {
        return new JobService(logger, createPolarisService());
    }

    public CountService createCountService() {
        return new CountService(createPolarisService());
    }

    public ContextsService createContextsService() {
        return new ContextsService(createPolarisService(), httpClient.getPolarisServerUrl());
    }

    public IntLogger getLogger() {
        return logger;
    }

    public Gson getGson() {
        return gson;
    }

    public AccessTokenPolarisHttpClient getHttpClient() {
        return httpClient;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        if (defaultPageSize >= 0) {
            this.defaultPageSize = defaultPageSize;
        }
    }

}

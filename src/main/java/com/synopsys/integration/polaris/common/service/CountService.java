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

import java.util.List;
import java.util.Objects;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.polaris.common.api.PolarisResource;
import com.synopsys.integration.polaris.common.api.model.CountV0Attributes;
import com.synopsys.integration.rest.HttpUrl;

public class CountService {
    private final PolarisService polarisService;

    public CountService(PolarisService polarisService) {
        this.polarisService = polarisService;
    }

    public List<PolarisResource<CountV0Attributes>> getCountV0ResourcesFromIssueApiUrl(HttpUrl issueApiUrl) throws IntegrationException {
        return polarisService.getAll(issueApiUrl, CountV0Attributes.class);
    }

    public Integer getTotalIssueCountFromIssueApiUrl(HttpUrl issueApiUrl) throws IntegrationException {
        return getCountV0ResourcesFromIssueApiUrl(issueApiUrl).stream()
                   .map(PolarisResource::getAttributes)
                   .map(CountV0Attributes::getValue)
                   .filter(Objects::nonNull)
                   .reduce(0, Integer::sum);
    }
}

/*
 * synopsys-polaris
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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

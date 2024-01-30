/*
 * synopsys-polaris
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.polaris.common.service;

import java.util.List;
import java.util.Optional;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.polaris.common.api.PolarisResource;
import com.synopsys.integration.polaris.common.api.model.ContextAttributes;
import com.synopsys.integration.rest.HttpUrl;

public class ContextsService {
    private final PolarisService polarisService;
    private final HttpUrl polarisServerUrl;

    public ContextsService(PolarisService polarisService, HttpUrl polarisServerUrl) {
        this.polarisService = polarisService;
        this.polarisServerUrl = polarisServerUrl;
    }

    public List<PolarisResource<ContextAttributes>> getAllContexts() throws IntegrationException {
        HttpUrl httpUrl = polarisServerUrl.appendRelativeUrl("/api/auth/contexts");
        return polarisService.getAll(httpUrl, ContextAttributes.class);
    }

    public Optional<PolarisResource<ContextAttributes>> getCurrentContext() throws IntegrationException {
        return getAllContexts().stream()
                   .filter(this::isCurrentContext)
                   .findFirst();
    }

    private Boolean isCurrentContext(PolarisResource<ContextAttributes> context) {
        return Optional.ofNullable(context)
                   .map(PolarisResource::getAttributes)
                   .map(ContextAttributes::getCurrent)
                   .orElse(Boolean.FALSE);
    }

}

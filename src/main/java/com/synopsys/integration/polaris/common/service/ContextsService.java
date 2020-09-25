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

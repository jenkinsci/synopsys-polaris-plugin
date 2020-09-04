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

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.polaris.common.api.AttributelessPolarisResource;
import com.synopsys.integration.polaris.common.api.PolarisAttributes;
import com.synopsys.integration.polaris.common.api.PolarisPagedResourceResponse;
import com.synopsys.integration.polaris.common.api.PolarisPaginationMeta;
import com.synopsys.integration.polaris.common.api.PolarisResource;
import com.synopsys.integration.polaris.common.api.PolarisResponse;
import com.synopsys.integration.polaris.common.request.PolarisPagedRequestWrapper;
import com.synopsys.integration.polaris.common.rest.AccessTokenPolarisHttpClient;
import com.synopsys.integration.rest.request.Request;
import com.synopsys.integration.rest.response.Response;

public class PolarisService {
    public static final String QUERY_API_SPEC = "/api/query/v0";
    public static final String ISSUES_API_SPEC = QUERY_API_SPEC + "/issues";
    private final AccessTokenPolarisHttpClient polarisHttpClient;
    private final PolarisJsonTransformer polarisJsonTransformer;
    private final int defaultPageSize;

    public PolarisService(AccessTokenPolarisHttpClient polarisHttpClient, PolarisJsonTransformer polarisJsonTransformer, int defaultPageSize) {
        this.polarisHttpClient = polarisHttpClient;
        this.polarisJsonTransformer = polarisJsonTransformer;
        this.defaultPageSize = defaultPageSize;
    }

    public static final String GET_ISSUE_API_SPEC(String issueKey) {
        return ISSUES_API_SPEC + "/" + issueKey;
    }

    public <A extends PolarisAttributes, R extends PolarisResource<A>> Optional<R> getResourceFromPopulated(PolarisPagedResourceResponse<R> populatedResources, AttributelessPolarisResource sparseResourceData, Class<R> resourceClass) {
        String id = StringUtils.defaultString(sparseResourceData.getId());
        String type = StringUtils.defaultString(sparseResourceData.getType());
        for (AttributelessPolarisResource includedResource : populatedResources.getIncluded()) {
            if (type.equals(includedResource.getType()) && id.equals(includedResource.getId())) {
                try {
                    R fullyTypedResource = polarisJsonTransformer.getResponseAs(includedResource.getJson(), resourceClass);
                    return Optional.of(fullyTypedResource);
                } catch (IntegrationException e) {
                    break;
                }
            }
        }
        return Optional.empty();
    }

    public <R extends PolarisResponse> R get(Type returnType, Request request) throws IntegrationException {
        try (Response response = polarisHttpClient.execute(request)) {
            response.throwExceptionForError();

            return polarisJsonTransformer.getResponse(response, returnType);
        } catch (IOException e) {
            throw new IntegrationException(e);
        }
    }

    /* TODO: Refactor this implementation. The following should compile, but doesn't. --rotte APR 2020
public <R extends PolarisResource> Optional<R> getFirstResponse2(final Request request, final Type resourcesType) throws IntegrationException {
    return getAllResponses(request, resourcesType)
               .stream()
               .findFirst();
}
 */
    public <A extends PolarisAttributes, R extends PolarisResource<A>> Optional<R> getFirstResponse(Request request, Type resourcesType) throws IntegrationException {
        try (Response response = polarisHttpClient.execute(request)) {
            response.throwExceptionForError();
            PolarisPagedResourceResponse<R> wrappedResponse = polarisJsonTransformer.getResponse(response, resourcesType);
            if (wrappedResponse != null) {
                List<R> data = wrappedResponse.getData();
                if (null != data && !data.isEmpty()) {
                    return Optional.ofNullable(data.get(0));
                }
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new IntegrationException(e);
        }
    }

    public <A extends PolarisAttributes, R extends PolarisResource<A>> List<R> getAllResponses(Request request, Type resourcesType) throws IntegrationException {
        try (Response response = polarisHttpClient.execute(request)) {
            response.throwExceptionForError();
            PolarisPagedResourceResponse<R> wrappedResponse = polarisJsonTransformer.getResponse(response, resourcesType);
            if (wrappedResponse != null && wrappedResponse.getData() != null) {
                return wrappedResponse.getData();
            }
            return Collections.emptyList();
        } catch (IOException e) {
            throw new IntegrationException(e);
        }
    }

    public <A extends PolarisAttributes, R extends PolarisResource<A>> List<R> getAllResponses(PolarisPagedRequestWrapper polarisPagedRequestWrapper) throws IntegrationException {
        return getAllResponses(polarisPagedRequestWrapper, defaultPageSize);
    }

    public <A extends PolarisAttributes, R extends PolarisResource<A>, W extends PolarisPagedResourceResponse<R>> List<R> getAllResponses(PolarisPagedRequestWrapper polarisPagedRequestWrapper, int pageSize) throws IntegrationException {
        W populatedResponse = getPopulatedResponse(polarisPagedRequestWrapper, pageSize);

        if (populatedResponse == null) {
            return Collections.emptyList();
        }

        return populatedResponse.getData();
    }

    public <A extends PolarisAttributes, R extends PolarisResource<A>, W extends PolarisPagedResourceResponse<R>> W getPopulatedResponse(PolarisPagedRequestWrapper polarisPagedRequestWrapper) throws IntegrationException {
        return getPopulatedResponse(polarisPagedRequestWrapper, defaultPageSize);
    }

    // TODO: Cognitive complexity should be reduced even more here --rotte APR 2020
    public <A extends PolarisAttributes, R extends PolarisResource<A>, W extends PolarisPagedResourceResponse<R>> W getPopulatedResponse(PolarisPagedRequestWrapper polarisPagedRequestWrapper, int pageSize) throws IntegrationException {
        W populatedResources = null;
        List<R> allData = new ArrayList<>();
        List<AttributelessPolarisResource> allIncluded = new ArrayList<>();

        Integer totalExpected = null;
        int offset = 0;
        boolean totalExpectedHasNotBeenSet = true;
        boolean thisPageHadData;
        boolean isMoreData = true;
        do {
            W wrappedResponse = executePagedRequest(polarisPagedRequestWrapper, offset, pageSize);
            if (wrappedResponse == null) {
                break;
            }

            if (null == populatedResources) {
                populatedResources = wrappedResponse;
            }

            if (totalExpectedHasNotBeenSet) {
                PolarisPaginationMeta meta = wrappedResponse.getMeta();
                totalExpected = Optional.ofNullable(meta)
                                    .map(PolarisPaginationMeta::getTotal)
                                    .map(BigDecimal::intValue)
                                    .orElse(null);
                totalExpectedHasNotBeenSet = false;
            }

            List<R> data = Optional.ofNullable(wrappedResponse.getData()).orElse(Collections.emptyList());
            allData.addAll(data);

            List<AttributelessPolarisResource> included = Optional.ofNullable(wrappedResponse.getIncluded()).orElse(Collections.emptyList());
            allIncluded.addAll(included);

            if (totalExpected != null) {
                isMoreData = totalExpected > allData.size();
            }
            thisPageHadData = !data.isEmpty();
            offset += pageSize;
        } while (isMoreData && thisPageHadData);

        // If wrappedResponse is null, populatedResources could be null -- rotte APR 2020
        if (populatedResources != null) {
            populatedResources.setData(new ArrayList<>(allData));
            populatedResources.setIncluded(new ArrayList<>(allIncluded));
        }
        return populatedResources;
    }

    private <A extends PolarisAttributes, R extends PolarisResource<A>, W extends PolarisPagedResourceResponse<R>> W executePagedRequest(PolarisPagedRequestWrapper polarisPagedRequestWrapper, int offset, int limit)
        throws IntegrationException {
        Request pagedRequest = polarisPagedRequestWrapper.getRequestCreator().apply(limit, offset);
        try (Response response = polarisHttpClient.execute(pagedRequest)) {
            response.throwExceptionForError();
            return polarisJsonTransformer.getResponse(response, polarisPagedRequestWrapper.getResponseType());
        } catch (IOException e) {
            throw new IntegrationException("Problem handling request", e);
        }
    }

}

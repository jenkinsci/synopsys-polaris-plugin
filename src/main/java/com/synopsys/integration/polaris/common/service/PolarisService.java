/*
 * synopsys-polaris
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.polaris.common.service;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.gson.reflect.TypeToken;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.polaris.common.api.PolarisAttributes;
import com.synopsys.integration.polaris.common.api.PolarisPagedResourceResponse;
import com.synopsys.integration.polaris.common.api.PolarisPaginationMeta;
import com.synopsys.integration.polaris.common.api.PolarisResource;
import com.synopsys.integration.polaris.common.api.PolarisResponse;
import com.synopsys.integration.polaris.common.api.PolarisSingleResourceResponse;
import com.synopsys.integration.polaris.common.request.PolarisRequestFactory;
import com.synopsys.integration.polaris.common.rest.AccessTokenPolarisHttpClient;
import com.synopsys.integration.rest.HttpUrl;
import com.synopsys.integration.rest.request.Request;
import com.synopsys.integration.rest.response.Response;

public class PolarisService {
    private final AccessTokenPolarisHttpClient polarisHttpClient;
    private final PolarisJsonTransformer polarisJsonTransformer;
    private final int defaultPageSize;

    public PolarisService(AccessTokenPolarisHttpClient polarisHttpClient, PolarisJsonTransformer polarisJsonTransformer, int defaultPageSize) {
        this.polarisHttpClient = polarisHttpClient;
        this.polarisJsonTransformer = polarisJsonTransformer;
        this.defaultPageSize = defaultPageSize;
    }

    public <R extends PolarisResponse> R get(Request request, Type returnType) throws IntegrationException {
        try (Response response = polarisHttpClient.execute(request)) {
            response.throwExceptionForError();

            return polarisJsonTransformer.getResponse(response, returnType);
        } catch (IOException e) {
            throw new IntegrationException(e);
        }
    }

    public <A extends PolarisAttributes> PolarisResource<A> get(HttpUrl apiUrl, Class<A> attributeType) throws IntegrationException {
        Type resourceType = TypeToken.getParameterized(PolarisResource.class, attributeType).getType();
        Type responseType = TypeToken.getParameterized(PolarisSingleResourceResponse.class, resourceType).getType();

        Request request = PolarisRequestFactory.createDefaultGetRequest(apiUrl);
        PolarisSingleResourceResponse<PolarisResource<A>> polarisSingleResourceResponse = get(request, responseType);
        return polarisSingleResourceResponse.getData();
    }

    public <A extends PolarisAttributes> List<PolarisResource<A>> getAll(HttpUrl apiUrl, Class<A> attributeType) throws IntegrationException {
        return getAll(apiUrl, attributeType, defaultPageSize);
    }

    public <A extends PolarisAttributes> List<PolarisResource<A>> getAll(HttpUrl apiUrl, Class<A> attributeType, int pageSize) throws IntegrationException {
        return collectAllResources(apiUrl, attributeType, pageSize);
    }

    public <A extends PolarisAttributes> List<PolarisResource<A>> collectAllResources(HttpUrl apiUrl, Class<A> attributeType, int pageSize) throws IntegrationException {
        List<PolarisResource<A>> allResources = new ArrayList<>();

        Integer totalExpected = null;
        int offset = 0;
        boolean totalExpectedHasNotBeenSet = true;
        boolean thisPageHadData;
        boolean isMoreData;
        do {
            PolarisPagedResourceResponse<PolarisResource<A>> pageOfResources = executePagedRequest(apiUrl, attributeType, offset, pageSize);
            if (pageOfResources == null) {
                break;
            }

            if (totalExpectedHasNotBeenSet) {
                PolarisPaginationMeta meta = pageOfResources.getMeta();
                totalExpected = Optional.ofNullable(meta)
                                    .map(PolarisPaginationMeta::getTotal)
                                    .map(BigDecimal::intValue)
                                    .orElse(null);
                totalExpectedHasNotBeenSet = false;
            }

            List<PolarisResource<A>> pageResources = Optional.ofNullable(pageOfResources.getData()).orElse(Collections.emptyList());
            allResources.addAll(pageResources);

            // Pagination meta does not include a total if it only has one page of results to give. -- rotte SEP 2020
            isMoreData = totalExpected != null && totalExpected > allResources.size();
            thisPageHadData = !pageResources.isEmpty();
            offset += pageSize;
        } while (isMoreData && thisPageHadData);

        return allResources;
    }

    protected <A extends PolarisAttributes> PolarisPagedResourceResponse<PolarisResource<A>> executePagedRequest(HttpUrl apiUrl, Class<A> attributeType, int offset, int limit) throws IntegrationException {
        Type resourceType = TypeToken.getParameterized(PolarisResource.class, attributeType).getType();
        Type responseType = TypeToken.getParameterized(PolarisPagedResourceResponse.class, resourceType).getType();

        Request pagedRequest = PolarisRequestFactory.createDefaultPagedGetRequest(apiUrl, limit, offset);
        try (Response response = polarisHttpClient.execute(pagedRequest)) {
            response.throwExceptionForError();
            return polarisJsonTransformer.getResponse(response, responseType);
        } catch (IOException e) {
            throw new IntegrationException("Problem handling request", e);
        }
    }

}

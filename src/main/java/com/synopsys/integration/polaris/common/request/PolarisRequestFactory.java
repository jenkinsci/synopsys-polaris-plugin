/*
 * synopsys-polaris
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.polaris.common.request;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.synopsys.integration.rest.HttpMethod;
import com.synopsys.integration.rest.HttpUrl;
import com.synopsys.integration.rest.request.Request;

public class PolarisRequestFactory {
    public static final String DEFAULT_MIME_TYPE = "application/vnd.api+json";

    public static final String LIMIT_PARAMETER = "page[limit]";
    public static final String OFFSET_PARAMETER = "page[offset]";

    public static final int DEFAULT_LIMIT = 25;
    public static final int DEFAULT_OFFSET = 0;

    public static Request createDefaultGetRequest(HttpUrl requestHttpUrl) {
        return createDefaultBuilder()
                   .url(requestHttpUrl)
                   .build();
    }

    public static Request createDefaultPagedGetRequest(HttpUrl requestHttpUrl) {
        return createDefaultPagedGetRequest(requestHttpUrl, DEFAULT_LIMIT);
    }

    public static Request createDefaultPagedGetRequest(HttpUrl requestHttpUrl, int limit) {
        return createDefaultPagedGetRequest(requestHttpUrl, limit, DEFAULT_OFFSET);
    }

    public static Request createDefaultPagedGetRequest(HttpUrl requestHttpUrl, int limit, int offset) {
        return createDefaultPagedRequestBuilder(limit, offset)
                   .url(requestHttpUrl)
                   .build();
    }

    public static Request.Builder createDefaultPagedRequestBuilder(int limit, int offset) {
        return populatePagedRequestBuilder(createDefaultBuilder(), limit, offset);
    }

    public static Request.Builder populatePagedRequestBuilder(Request.Builder requestBuilder, int limit, int offset) {
        Map<String, Set<String>> queryParameters = requestBuilder.getQueryParameters();
        if (null == queryParameters) {
            requestBuilder.queryParameters(new HashMap<>());
            queryParameters = requestBuilder.getQueryParameters();
        }
        queryParameters.put(LIMIT_PARAMETER, Collections.singleton(Integer.toString(limit)));
        queryParameters.put(OFFSET_PARAMETER, Collections.singleton(Integer.toString(offset)));
        return requestBuilder;
    }

    public static Request.Builder createDefaultBuilder() {
        return new Request.Builder()
                   .acceptMimeType(DEFAULT_MIME_TYPE)
                   .method(HttpMethod.GET);
    }

}

/*
 * synopsys-polaris
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.polaris.common.rest;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;

import com.google.gson.Gson;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.rest.HttpMethod;
import com.synopsys.integration.rest.HttpUrl;
import com.synopsys.integration.rest.client.AuthenticatingIntHttpClient;
import com.synopsys.integration.rest.proxy.ProxyInfo;
import com.synopsys.integration.rest.response.Response;
import com.synopsys.integration.rest.support.AuthenticationSupport;

public class AccessTokenPolarisHttpClient extends AuthenticatingIntHttpClient {
    private static final String AUTHENTICATION_SPEC = "api/auth/authenticate";
    private static final String AUTHENTICATION_RESPONSE_KEY = "jwt";

    private static final String ACCESS_TOKEN_REQUEST_KEY = "accesstoken";
    private static final String ACCESS_TOKEN_REQUEST_CONTENT_TYPE = "application/x-www-form-urlencoded";

    private final Gson gson;
    private final AuthenticationSupport authenticationSupport;
    private final HttpUrl baseUrl;
    private final String accessToken;

    public AccessTokenPolarisHttpClient(IntLogger logger, int timeout, ProxyInfo proxyInfo, HttpUrl baseUrl, String accessToken, Gson gson, AuthenticationSupport authenticationSupport) {
        super(logger, timeout, false, proxyInfo);
        this.baseUrl = baseUrl;
        this.accessToken = accessToken;
        this.gson = gson;
        this.authenticationSupport = authenticationSupport;

        if (StringUtils.isBlank(accessToken)) {
            throw new IllegalArgumentException("No access token was found.");
        }
    }

    @Override
    public void handleErrorResponse(HttpUriRequest request, Response response) {
        super.handleErrorResponse(request, response);

        authenticationSupport.handleTokenErrorResponse(this, request, response);
    }

    @Override
    public boolean isAlreadyAuthenticated(HttpUriRequest request) {
        return authenticationSupport.isTokenAlreadyAuthenticated(request);
    }

    @Override
    protected void completeAuthenticationRequest(HttpUriRequest request, Response response) {
        authenticationSupport.completeTokenAuthenticationRequest(request, response, logger, gson, this, AccessTokenPolarisHttpClient.AUTHENTICATION_RESPONSE_KEY);
    }

    @Override
    public final Response attemptAuthentication() throws IntegrationException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", AccessTokenPolarisHttpClient.ACCESS_TOKEN_REQUEST_CONTENT_TYPE);

        String httpBody = String.format("%s=%s", AccessTokenPolarisHttpClient.ACCESS_TOKEN_REQUEST_KEY, accessToken);
        HttpEntity httpEntity = new StringEntity(httpBody, StandardCharsets.UTF_8);

        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.POST, headers);
        requestBuilder.setEntity(httpEntity);

        HttpUrl authenticationUrl = baseUrl.appendRelativeUrl(AccessTokenPolarisHttpClient.AUTHENTICATION_SPEC);
        return authenticationSupport.attemptAuthentication(this, authenticationUrl, requestBuilder);
    }

    public HttpUrl getPolarisServerUrl() {
        return baseUrl;
    }

}

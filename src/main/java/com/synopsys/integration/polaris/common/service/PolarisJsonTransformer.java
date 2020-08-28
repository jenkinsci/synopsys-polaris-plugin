/**
 * polaris-common
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

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.polaris.common.api.PolarisComponent;
import com.synopsys.integration.rest.response.Response;

public class PolarisJsonTransformer {
    private static final String FIELD_NAME_POLARIS_COMPONENT_JSON = "json";

    private final Gson gson;
    private final IntLogger logger;

    public PolarisJsonTransformer(Gson gson, IntLogger logger) {
        this.gson = gson;
        this.logger = logger;
    }

    public <C extends PolarisComponent> C getResponse(Response response, Type responseType) throws IntegrationException {
        String json = response.getContentString();
        return getResponseAs(json, responseType);
    }

    public <C extends PolarisComponent> C getResponseAs(String json, Type responseType) throws IntegrationException {
        try {
            JsonObject jsonElement = gson.fromJson(json, JsonObject.class);
            return getResponseAs(jsonElement, responseType);
        } catch (JsonSyntaxException e) {
            logger.error(String.format("Could not parse the provided json with Gson:%s%s", System.lineSeparator(), json));
            throw new IntegrationException(e.getMessage(), e);
        }
    }

    public <C extends PolarisComponent> C getResponseAs(JsonObject jsonObject, Type responseType) throws IntegrationException {
        String json = gson.toJson(jsonObject);
        try {
            addJsonAsField(jsonObject);
            return gson.fromJson(jsonObject, responseType);
        } catch (JsonSyntaxException e) {
            logger.error(String.format("Could not parse the provided jsonElement with Gson:%s%s", System.lineSeparator(), json));
            throw new IntegrationException(e.getMessage(), e);
        }
    }

    private void addJsonAsField(JsonElement jsonElement) {
        if (jsonElement.isJsonObject()) {
            JsonObject innerObject = jsonElement.getAsJsonObject();
            jsonElement.getAsJsonObject().addProperty(FIELD_NAME_POLARIS_COMPONENT_JSON, gson.toJson(innerObject));
            for (Map.Entry<String, JsonElement> innerObjectFields : innerObject.entrySet()) {
                addJsonAsField(innerObjectFields.getValue());
            }
        } else if (jsonElement.isJsonArray()) {
            for (JsonElement arrayElement : jsonElement.getAsJsonArray()) {
                addJsonAsField(arrayElement);
            }
        }
    }

}

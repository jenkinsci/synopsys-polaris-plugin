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
package com.synopsys.integration.polaris.common.cli.model.json.parser;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.polaris.common.cli.model.CliCommonResponseModel;
import com.synopsys.integration.polaris.common.cli.model.CommonToolInfo;
import com.synopsys.integration.polaris.common.cli.model.json.v2.CliScanV2;
import com.synopsys.integration.polaris.common.cli.model.json.v2.ToolInfoV2;
import com.synopsys.integration.rest.HttpUrl;

public class CliScanV2Parser extends CliScanParser<CliScanV2> {
    public CliScanV2Parser(Gson gson) {
        super(gson);
    }

    @Override
    public TypeToken<CliScanV2> getTypeToken() {
        return new TypeToken<CliScanV2>() {
        };
    }

    public CliCommonResponseModel fromCliScan(JsonObject versionlessModel) throws IntegrationException {
        CliScanV2 cliScanV2 = fromJson(versionlessModel);

        CliCommonResponseModel cliCommonResponseModel = createResponseModel(cliScanV2.issueSummary, cliScanV2.projectInfo, cliScanV2.scanInfo);

        List<CommonToolInfo> tools = Optional.ofNullable(cliScanV2.tools)
                                         .orElse(Collections.emptyList())
                                         .stream()
                                         .map(this::fromToolInfoV2)
                                         .filter(Optional::isPresent)
                                         .map(Optional::get)
                                         .collect(Collectors.toList());

        cliCommonResponseModel.setTools(tools);

        return cliCommonResponseModel;
    }

    private Optional<CommonToolInfo> fromToolInfoV2(ToolInfoV2 toolInfoV2) {
        if (toolInfoV2 != null) {
            try {
                CommonToolInfo commonToolInfo = createCommonToolInfo(toolInfoV2);
                commonToolInfo.setToolName(toolInfoV2.toolName);
                if (StringUtils.isNotBlank(toolInfoV2.issueApiUrl)) {
                    commonToolInfo.setIssueApiUrl(new HttpUrl(toolInfoV2.issueApiUrl));
                }

                return Optional.of(commonToolInfo);
            } catch (IntegrationException ignored) {

            }
        }

        return Optional.empty();
    }

}

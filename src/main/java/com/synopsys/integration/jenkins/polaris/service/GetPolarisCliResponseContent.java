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
package com.synopsys.integration.jenkins.polaris.service;

import java.io.IOException;
import java.nio.file.Files;

import com.synopsys.integration.polaris.common.cli.PolarisCliResponseUtility;
import com.synopsys.integration.polaris.common.exception.PolarisIntegrationException;

import jenkins.security.MasterToSlaveCallable;

public class GetPolarisCliResponseContent extends MasterToSlaveCallable<String, PolarisIntegrationException> {
    private static final long serialVersionUID = -5698280934593066898L;
    private final String workspaceRemotePath;

    public GetPolarisCliResponseContent(String workspaceRemotePath) {
        this.workspaceRemotePath = workspaceRemotePath;
    }

    @Override
    public String call() throws PolarisIntegrationException {
        try {
            return new String(Files.readAllBytes(PolarisCliResponseUtility.getDefaultPathToJson(workspaceRemotePath)));
        } catch (IOException e) {
            throw new PolarisIntegrationException("There was an error getting the Polaris CLI response.", e);
        }
    }

}

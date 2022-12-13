/*
 * synopsys-polaris
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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

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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.synopsys.integration.polaris.common.exception.PolarisIntegrationException;
import com.synopsys.integration.util.OperatingSystemType;

import jenkins.security.MasterToSlaveCallable;

public class GetPathToPolarisCli extends MasterToSlaveCallable<String, PolarisIntegrationException> {
    private static final long serialVersionUID = -8823365241230615671L;
    private final String polarisCliHome;

    public GetPathToPolarisCli(String polarisCliHome) {
        this.polarisCliHome = polarisCliHome;
    }

    @Override
    public String call() throws PolarisIntegrationException {
        Path homePath = Paths.get(polarisCliHome);
        Path binPath = homePath.resolve("bin");

        OperatingSystemType operatingSystemType = OperatingSystemType.determineFromSystem();

        Optional<String> polarisCli = checkFile(operatingSystemType, binPath, "polaris");
        Optional<String> swipCli = checkFile(operatingSystemType, binPath, "swip_cli");

        if (polarisCli.isPresent()) {
            return polarisCli.get();
        } else if (swipCli.isPresent()) {
            return swipCli.get();
        }

        throw new PolarisIntegrationException("The Polaris CLI could not be found in " + binPath.toString() + " on this node. Please verify the cli exists there and is executable.");
    }

    private Optional<String> checkFile(OperatingSystemType operatingSystemType, Path binPath, String filePrefix) {
        String binaryName = filePrefix;
        if (OperatingSystemType.WINDOWS == operatingSystemType) {
            binaryName += ".exe";
        }
        Path binaryPath = binPath.resolve(binaryName);

        try {
            if (!Files.isDirectory(binaryPath) && Files.size(binaryPath) > 0L) {
                Path realFilePath = binaryPath.toRealPath();
                return Optional.of(realFilePath.toString());
            }
        } catch (IOException e) {
            // Do nothing,
        }
        return Optional.empty();
    }
}

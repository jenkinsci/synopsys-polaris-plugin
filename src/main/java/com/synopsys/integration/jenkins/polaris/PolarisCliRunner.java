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
package com.synopsys.integration.jenkins.polaris;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.polaris.extensions.tools.PolarisCli;
import com.synopsys.integration.jenkins.polaris.service.GetPathToPolarisCli;
import com.synopsys.integration.jenkins.polaris.service.PolarisCliArgumentService;
import com.synopsys.integration.jenkins.polaris.service.PolarisEnvironmentService;
import com.synopsys.integration.jenkins.polaris.service.PolarisPhoneHomeService;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.phonehome.PhoneHomeResponse;
import com.synopsys.integration.polaris.common.exception.PolarisIntegrationException;
import com.synopsys.integration.util.IntEnvironmentVariables;
import com.synopsys.integration.util.OperatingSystemType;

public class PolarisCliRunner {
    private final PolarisCliArgumentService polarisCliArgumentService;
    private final PolarisEnvironmentService polarisEnvironmentService;
    private final PolarisPhoneHomeService polarisPhoneHomeService;
    private final JenkinsRemotingService jenkinsRemotingService;
    private final JenkinsConfigService jenkinsConfigService;

    public PolarisCliRunner(PolarisCliArgumentService polarisCliArgumentService, PolarisEnvironmentService polarisEnvironmentService, PolarisPhoneHomeService polarisPhoneHomeService,
        JenkinsRemotingService jenkinsRemotingService, JenkinsConfigService jenkinsConfigService) {
        this.polarisCliArgumentService = polarisCliArgumentService;
        this.polarisEnvironmentService = polarisEnvironmentService;
        this.polarisPhoneHomeService = polarisPhoneHomeService;
        this.jenkinsRemotingService = jenkinsRemotingService;
        this.jenkinsConfigService = jenkinsConfigService;
    }

    public int runPolarisCli(String polarisCliName, String polarisArgumentString) throws IOException, InterruptedException, IntegrationException {
        Optional<PhoneHomeResponse> successfulPhoneHomeResponse = polarisPhoneHomeService.phoneHome();

        try {
            Optional<PolarisCli> polarisCliWithName = jenkinsConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, polarisCliName);

            if (!polarisCliWithName.isPresent()) {
                throw new PolarisIntegrationException("Polaris cannot be executed: No PolarisCli with the name " + polarisCliName + " could be found in the global tool configuration.");
            }

            PolarisCli polarisCli = polarisCliWithName.get();

            IntEnvironmentVariables intEnvironmentVariables = polarisEnvironmentService.createPolarisEnvironment();

            String pathToPolarisCli = jenkinsRemotingService.call(new GetPathToPolarisCli(polarisCli.getHome()));

            OperatingSystemType operatingSystemType = jenkinsRemotingService.getRemoteOperatingSystemType();
            List<String> tokenizedPolarisArguments = jenkinsRemotingService.tokenizeArgumentString(polarisArgumentString);
            List<String> polarisArguments = polarisCliArgumentService.finalizePolarisCliArguments(operatingSystemType, pathToPolarisCli, tokenizedPolarisArguments);

            return jenkinsRemotingService.launch(intEnvironmentVariables, polarisArguments);
        } finally {
            successfulPhoneHomeResponse.ifPresent(PhoneHomeResponse::getImmediateResult);
        }
    }
}

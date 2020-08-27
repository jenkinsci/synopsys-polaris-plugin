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

    public PolarisCliRunner(PolarisCliArgumentService polarisCliArgumentService, PolarisEnvironmentService polarisEnvironmentService,
        PolarisPhoneHomeService polarisPhoneHomeService, JenkinsRemotingService jenkinsRemotingService, JenkinsConfigService jenkinsConfigService) {
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

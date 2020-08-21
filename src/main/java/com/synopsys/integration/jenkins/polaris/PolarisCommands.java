package com.synopsys.integration.jenkins.polaris;

import java.io.IOException;
import java.util.Optional;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.extensions.ChangeBuildStatusTo;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.polaris.extensions.freestyle.WaitForIssues;
import com.synopsys.integration.jenkins.polaris.extensions.tools.PolarisCli;
import com.synopsys.integration.jenkins.polaris.service.GetPathToPolarisCli;
import com.synopsys.integration.jenkins.polaris.service.GetPolarisCliResponseContent;
import com.synopsys.integration.jenkins.polaris.service.PolarisCliArgumentService;
import com.synopsys.integration.jenkins.polaris.service.PolarisCliIssueCountService;
import com.synopsys.integration.jenkins.polaris.service.PolarisEnvironmentService;
import com.synopsys.integration.jenkins.polaris.service.PolarisJenkinsServicesFactory;
import com.synopsys.integration.jenkins.polaris.service.PolarisPhoneHomeService;
import com.synopsys.integration.jenkins.service.JenkinsBuildService;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.jenkins.service.JenkinsServicesFactory;
import com.synopsys.integration.phonehome.PhoneHomeResponse;
import com.synopsys.integration.polaris.common.exception.PolarisIntegrationException;
import com.synopsys.integration.polaris.common.service.JobService;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.util.ArgumentListBuilder;

public class PolarisCommands {
    private final PolarisJenkinsServicesFactory polarisJenkinsServicesFactory;
    private final JenkinsServicesFactory jenkinsServicesFactory;

    public PolarisCommands(PolarisJenkinsServicesFactory polarisJenkinsServicesFactory) {
        this.polarisJenkinsServicesFactory = polarisJenkinsServicesFactory;
        this.jenkinsServicesFactory = polarisJenkinsServicesFactory.createJenkinsServiceFactory();
    }

    public void executePolarisCliFreestyle(String polarisCliName, String polarisArgumentString, WaitForIssues waitForIssues) {
        JenkinsBuildService jenkinsBuildService = jenkinsServicesFactory.createJenkinsBuildService();

        try {
            int exitCode = this.runPolarisCli(polarisCliName, polarisArgumentString);
            if (exitCode > 0) {
                jenkinsBuildService.markBuildFailed("Polaris failed with exit code: " + exitCode);
            }

            if (waitForIssues != null) {
                JenkinsIntLogger logger = polarisJenkinsServicesFactory.getOrCreateLogger();
                ChangeBuildStatusTo buildStatusToSet = Optional.ofNullable(waitForIssues.getBuildStatusForIssues())
                                                           .orElse(ChangeBuildStatusTo.SUCCESS);

                int issueCount = this.getPolarisIssueCount(waitForIssues.getJobTimeoutInMinutes());

                logger.alwaysLog("Polaris Issue Check");
                logger.alwaysLog("Build state for issues: " + buildStatusToSet.getDisplayName());
                logger.alwaysLog(String.format("Found %s issues", issueCount));
                if (issueCount > 0) {
                    jenkinsBuildService.markBuildAs(buildStatusToSet);
                }

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            jenkinsBuildService.markBuildInterrupted();
        } catch (IntegrationException e) {
            jenkinsBuildService.markBuildFailed(e);
        } catch (Exception e) {
            jenkinsBuildService.markBuildUnstable(e);
        }
    }

    public int executePolarisCliPipeline(String polarisCliName, String polarisCliArgumentString, Boolean returnStatus) throws IntegrationException, InterruptedException, IOException {
        int exitCode = runPolarisCli(polarisCliName, polarisCliArgumentString);

        if (exitCode > 0) {
            String errorMsg = "Polaris failed with exit code: " + exitCode;
            if (Boolean.TRUE.equals(returnStatus)) {
                JenkinsIntLogger logger = polarisJenkinsServicesFactory.getOrCreateLogger();
                logger.error(errorMsg);
            } else {
                throw new PolarisIntegrationException(errorMsg);
            }
        }

        return exitCode;
    }

    public int checkForPolarisIssues(Integer jobTimeoutInMinutes, Boolean returnIssueCount) throws InterruptedException, IntegrationException, IOException {
        int issueCount = getPolarisIssueCount(jobTimeoutInMinutes);

        if (issueCount > 0) {
            String defectMessage = String.format("[Polaris] Found %s total issues.", issueCount);
            if (Boolean.TRUE.equals(returnIssueCount)) {
                JenkinsIntLogger logger = polarisJenkinsServicesFactory.getOrCreateLogger();
                logger.error(defectMessage);
            } else {
                throw new PolarisIntegrationException(defectMessage);
            }
        }

        return issueCount;
    }

    protected int runPolarisCli(String polarisCliName, String polarisArgumentString) throws IOException, InterruptedException, IntegrationException {
        PolarisCliArgumentService polarisCliArgumentService = polarisJenkinsServicesFactory.createPolarisCliArgumentService();
        PolarisEnvironmentService polarisEnvironmentService = polarisJenkinsServicesFactory.createPolarisEnvironmentService();
        PolarisPhoneHomeService polarisPhoneHomeService = polarisJenkinsServicesFactory.createPolarisPhoneHomeService();
        JenkinsRemotingService jenkinsRemotingService = jenkinsServicesFactory.createJenkinsRemotingService();
        JenkinsConfigService jenkinsConfigService = jenkinsServicesFactory.createJenkinsConfigService();

        Optional<PhoneHomeResponse> successfulPhoneHomeResponse = polarisPhoneHomeService.phoneHome();

        try {
            Optional<PolarisCli> polarisCliWithName = jenkinsConfigService.getInstallationForNodeAndEnvironment(PolarisCli.DescriptorImpl.class, polarisCliName);

            if (!polarisCliWithName.isPresent()) {
                throw new PolarisIntegrationException("Polaris cannot be executed: No PolarisCli with the name " + polarisCliName + " could be found in the global tool configuration.");
            }

            PolarisCli polarisCli = polarisCliWithName.get();

            IntEnvironmentVariables intEnvironmentVariables = polarisEnvironmentService.createPolarisEnvironment();

            String pathToPolarisCli = jenkinsRemotingService.call(new GetPathToPolarisCli(polarisCli.getHome()));
            ArgumentListBuilder polarisArguments = polarisCliArgumentService.parsePolarisArgumentString(pathToPolarisCli, polarisArgumentString);

            return jenkinsRemotingService.launch(intEnvironmentVariables, polarisArguments.toList());
        } finally {
            successfulPhoneHomeResponse.ifPresent(PhoneHomeResponse::getImmediateResult);
        }
    }

    protected int getPolarisIssueCount(Integer jobTimeoutInMinutes) throws IOException, InterruptedException, IntegrationException {
        Long jobTimeoutInSeconds = Optional.ofNullable(jobTimeoutInMinutes)
                                       .map(value -> value * 60L)
                                       .orElse(JobService.DEFAULT_TIMEOUT);

        PolarisCliIssueCountService polarisCliIssueCountService = polarisJenkinsServicesFactory.createPolarisCliIssueCountService();
        JenkinsRemotingService jenkinsRemotingService = jenkinsServicesFactory.createJenkinsRemotingService();

        String cliCommonResponseModelJson = jenkinsRemotingService.call(new GetPolarisCliResponseContent(jenkinsRemotingService.getRemoteWorkspacePath()));

        return polarisCliIssueCountService.getIssueCount(jobTimeoutInSeconds, cliCommonResponseModelJson);
    }

}

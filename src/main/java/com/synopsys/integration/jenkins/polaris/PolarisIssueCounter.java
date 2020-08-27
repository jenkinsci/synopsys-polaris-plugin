package com.synopsys.integration.jenkins.polaris;

import java.io.IOException;
import java.util.Optional;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.polaris.service.GetPolarisCliResponseContent;
import com.synopsys.integration.jenkins.polaris.service.PolarisCliIssueCountService;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.polaris.common.service.JobService;

public class PolarisIssueCounter {
    private final PolarisCliIssueCountService polarisCliIssueCountService;
    private final JenkinsRemotingService jenkinsRemotingService;

    public PolarisIssueCounter(PolarisCliIssueCountService polarisCliIssueCountService, JenkinsRemotingService jenkinsRemotingService) {
        this.polarisCliIssueCountService = polarisCliIssueCountService;
        this.jenkinsRemotingService = jenkinsRemotingService;
    }

    public int getPolarisIssueCount(Integer jobTimeoutInMinutes) throws IOException, InterruptedException, IntegrationException {
        Long jobTimeoutInSeconds = Optional.ofNullable(jobTimeoutInMinutes)
                                       .map(value -> value * 60L)
                                       .orElse(JobService.DEFAULT_TIMEOUT);

        String cliCommonResponseModelJson = jenkinsRemotingService.call(new GetPolarisCliResponseContent(jenkinsRemotingService.getRemoteWorkspacePath()));

        return polarisCliIssueCountService.getIssueCount(jobTimeoutInSeconds, cliCommonResponseModelJson);
    }
}

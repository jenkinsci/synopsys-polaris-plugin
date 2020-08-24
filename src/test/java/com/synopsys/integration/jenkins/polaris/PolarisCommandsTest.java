package com.synopsys.integration.jenkins.polaris;

import java.util.Optional;

import org.junit.Test;
import org.mockito.Mockito;

import com.synopsys.integration.jenkins.extensions.ChangeBuildStatusTo;
import com.synopsys.integration.jenkins.polaris.extensions.freestyle.WaitForIssues;
import com.synopsys.integration.jenkins.polaris.extensions.tools.PolarisCli;
import com.synopsys.integration.jenkins.polaris.service.PolarisCliArgumentService;
import com.synopsys.integration.jenkins.polaris.service.PolarisEnvironmentService;
import com.synopsys.integration.jenkins.polaris.service.PolarisJenkinsServicesFactory;
import com.synopsys.integration.jenkins.polaris.service.PolarisPhoneHomeService;
import com.synopsys.integration.jenkins.service.JenkinsBuildService;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.jenkins.service.JenkinsServicesFactory;

public class PolarisCommandsTest {
    // TODO: This class is incomplete -- rotte APR 2020

    @Test
    public void testPreserveNullTimeout() throws Throwable {
        PolarisCli mockedPolarisCli = Mockito.mock(PolarisCli.class);
        JenkinsConfigService mockedJenkinsConfigService = Mockito.mock(JenkinsConfigService.class);
        Mockito.when(mockedJenkinsConfigService.getInstallationForNodeAndEnvironment(Mockito.eq(PolarisCli.DescriptorImpl.class), Mockito.anyString())).thenReturn(Optional.of(mockedPolarisCli));

        JenkinsBuildService mockedJenkinsBuildService = Mockito.mock(JenkinsBuildService.class);
        JenkinsRemotingService mockedJenkinsRemotingService = Mockito.mock(JenkinsRemotingService.class);
        JenkinsServicesFactory mockedJenkinsServicesFactory = Mockito.mock(JenkinsServicesFactory.class);
        Mockito.when(mockedJenkinsServicesFactory.createJenkinsBuildService()).thenReturn(mockedJenkinsBuildService);
        Mockito.when(mockedJenkinsServicesFactory.createJenkinsRemotingService()).thenReturn(mockedJenkinsRemotingService);
        Mockito.when(mockedJenkinsServicesFactory.createJenkinsConfigService()).thenReturn(mockedJenkinsConfigService);

        PolarisCliArgumentService mockedPolarisCliArgumentService = Mockito.mock(PolarisCliArgumentService.class);
        PolarisEnvironmentService mockedPolarisEnvironmentService = Mockito.mock(PolarisEnvironmentService.class);
        PolarisPhoneHomeService mockedPolarisPhoneHomeService = Mockito.mock(PolarisPhoneHomeService.class);
        PolarisJenkinsServicesFactory mockedPolarisServicesFactory = Mockito.mock(PolarisJenkinsServicesFactory.class);
        Mockito.when(mockedPolarisServicesFactory.createJenkinsServiceFactory()).thenReturn(mockedJenkinsServicesFactory);
        Mockito.when(mockedPolarisServicesFactory.createPolarisCliArgumentService()).thenReturn(mockedPolarisCliArgumentService);
        Mockito.when(mockedPolarisServicesFactory.createPolarisEnvironmentService()).thenReturn(mockedPolarisEnvironmentService);
        Mockito.when(mockedPolarisServicesFactory.createPolarisPhoneHomeService()).thenReturn(mockedPolarisPhoneHomeService);

        WaitForIssues waitForIssues = new WaitForIssues();
        waitForIssues.setBuildStatusForIssues(ChangeBuildStatusTo.SUCCESS);
        waitForIssues.setJobTimeoutInMinutes(null);
        PolarisCommands spiedPolarisCommands = Mockito.spy(new PolarisCommands(mockedPolarisServicesFactory));
        spiedPolarisCommands.executePolarisCliFreestyle("polarisCliName", "polarisArguments", waitForIssues);

        Mockito.verify(spiedPolarisCommands).getPolarisIssueCount(null);
    }

}

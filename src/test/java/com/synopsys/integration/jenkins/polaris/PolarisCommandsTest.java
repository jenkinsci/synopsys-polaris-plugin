package com.synopsys.integration.jenkins.polaris;

import org.junit.Test;
import org.mockito.Mockito;

import com.synopsys.integration.jenkins.extensions.ChangeBuildStatusTo;
import com.synopsys.integration.jenkins.polaris.extensions.freestyle.WaitForIssues;
import com.synopsys.integration.jenkins.polaris.service.PolarisJenkinsServicesFactory;

public class PolarisCommandsTest {
    // TODO: This class is incomplete -- rotte APR 2020

    @Test
    public void testPreserveNullTimeout() throws Throwable {
        PolarisJenkinsServicesFactory polarisJenkinsServicesFactory = Mockito.mock(PolarisJenkinsServicesFactory.class);

        WaitForIssues waitForIssues = new WaitForIssues();
        waitForIssues.setBuildStatusForIssues(ChangeBuildStatusTo.SUCCESS);
        waitForIssues.setJobTimeoutInMinutes(null);
        PolarisCommands spiedPolarisCommands = Mockito.spy(new PolarisCommands(polarisJenkinsServicesFactory));
        spiedPolarisCommands.executePolarisCliFreestyle("polarisCliName", "polarisArguments", waitForIssues);

        Mockito.verify(spiedPolarisCommands).getPolarisIssueCount(null);
    }

}

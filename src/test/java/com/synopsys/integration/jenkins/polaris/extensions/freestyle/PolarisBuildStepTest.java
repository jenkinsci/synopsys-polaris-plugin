package com.synopsys.integration.jenkins.polaris.extensions.freestyle;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.synopsys.integration.jenkins.polaris.extensions.global.PolarisGlobalConfig;
import com.synopsys.integration.jenkins.polaris.extensions.tools.PolarisCli;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfig;
import com.synopsys.integration.polaris.common.service.PolarisService;
import com.synopsys.integration.polaris.common.service.PolarisServicesFactory;

import hudson.EnvVars;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.model.GlobalConfiguration;

@PowerMockIgnore({ "javax.crypto.*", "javax.net.ssl.*" })
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AbstractBuild.class, JDK.class, Launcher.class, Launcher.ProcStarter.class, PolarisCli.class, GlobalConfiguration.class })
public class PolarisBuildStepTest {
    private static final String POLARIS_ARGUMENTS = "--detect.docker.passthrough.service.timeout=240000 --detect.cleanup=false --detect.source.path=$JAVA_HOME --detect.project.name=\"Test Project'\"";
    private static final String WORKSPACE_REL_PATH = "out/test/PolarisBuildStepTest/testPerform/workspace";

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    // TODO: To improve this test (to test more of PolarisBuildStep.perform(), we could refactor PolarisBuildStep to:
    // - Separate object creation (IntEnvironmentVariables, CreatePolarisEnvironment, GetPathToPolarisCli, ExecutePolarisCli,
    //   and maybe JenkinsIntLogger) out of PolarisBuildStep, so the objects can be mocked in this test.
    //   Then we can verify that they are created correctly, and improve the post-test verification.
    // - Separate afterPerform() out to a separate class, so it can be tested there, and this test can
    //   verify that it gets passed to stepWorkflowResponse.handleResponse()
    @Test
    public void testPerform() throws Throwable {
        // Setup
        AbstractBuild<FreeStyleProject, FreeStyleBuild> build = PowerMockito.mock(AbstractBuild.class);
        Launcher launcher = PowerMockito.mock(Launcher.class);
        BuildListener buildListener = PowerMockito.mock(BuildListener.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(baos, true, "UTF-8");
        Mockito.when(buildListener.getLogger()).thenReturn(printStream);

        FilePath workspaceFilePath = new FilePath(new File(WORKSPACE_REL_PATH));
        Mockito.when(build.getWorkspace()).thenReturn(workspaceFilePath);

        Node node = Mockito.mock(Node.class);
        Mockito.when(build.getBuiltOn()).thenReturn(node);

        EnvVars envVars = Mockito.mock(EnvVars.class);
        Mockito.when(build.getEnvironment(buildListener)).thenReturn(envVars);

        FreeStyleProject project = PowerMockito.mock(FreeStyleProject.class);
        JDK jdk = PowerMockito.mock(JDK.class);
        Mockito.when(build.getProject()).thenReturn(project);
        Mockito.when(project.getJDK()).thenReturn(jdk);
        Mockito.when(jdk.forNode(Mockito.any(Node.class), Mockito.any(TaskListener.class))).thenReturn(jdk);
        Mockito.when(jdk.getHome()).thenReturn("/tmp/jdk");

        VirtualChannel channel = PowerMockito.mock(hudson.remoting.VirtualChannel.class);
        Mockito.when(launcher.getChannel()).thenReturn(channel);

        PolarisCli polarisCli = PowerMockito.mock(PolarisCli.class);
        PowerMockito.mockStatic(PolarisCli.class);
        Mockito.when(PolarisCli.findInstallationWithName("testPolarisCliName")).thenReturn(Optional.of(polarisCli));
        Mockito.when(PolarisCli.installationsExist()).thenReturn(true);

        PolarisGlobalConfig polarisGlobalConfig = Mockito.mock(PolarisGlobalConfig.class);
        ExtensionList extensionList = Mockito.mock(ExtensionList.class);
        PowerMockito.mockStatic(GlobalConfiguration.class);
        Mockito.when(GlobalConfiguration.all()).thenReturn(extensionList);
        Mockito.when(extensionList.get(PolarisGlobalConfig.class)).thenReturn(polarisGlobalConfig);

        PolarisServerConfig polarisServerConfig = Mockito.mock(PolarisServerConfig.class);
        Mockito.when(polarisGlobalConfig.getPolarisServerConfig(Mockito.any(), Mockito.any())).thenReturn(polarisServerConfig);

        PolarisServicesFactory polarisServicesFactory = Mockito.mock(PolarisServicesFactory.class);
        Mockito.when(polarisServerConfig.createPolarisServicesFactory(Mockito.any(IntLogger.class))).thenReturn(polarisServicesFactory);

        PolarisService polarisService = Mockito.mock(PolarisService.class);
        Mockito.when(polarisServicesFactory.createPolarisService()).thenReturn(polarisService);

        WaitForIssues waitForIssues = Mockito.mock(WaitForIssues.class);

        // Test
        PolarisBuildStep polarisBuildStep = new PolarisBuildStep();
        polarisBuildStep.setPolarisCliName("testPolarisCliName");
        polarisBuildStep.setPolarisArguments(POLARIS_ARGUMENTS);
        polarisBuildStep.setWaitForIssues(waitForIssues);
        boolean result = polarisBuildStep.perform(build, launcher, buildListener);

        // Verify
        assertTrue(result);
        // The objects passed to the methods verified below are created within PolarisBuildStep (see TODO above),
        // so all we can verify is that the methods get called (with something).
    }
}

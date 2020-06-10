package com.synopsys.integration.jenkins.polaris.extensions.pipeline;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Optional;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.synopsys.integration.jenkins.polaris.extensions.global.PolarisGlobalConfig;
import com.synopsys.integration.jenkins.polaris.extensions.tools.PolarisCli;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfig;
import com.synopsys.integration.stepworkflow.StepWorkflow;
import com.synopsys.integration.stepworkflow.StepWorkflowResponse;
import com.synopsys.integration.stepworkflow.SubStep;

import hudson.EnvVars;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

@PowerMockIgnore({ "javax.crypto.*", "javax.net.ssl.*" })
@RunWith(PowerMockRunner.class)
@PrepareForTest({ PolarisCli.class, StepWorkflow.class, GlobalConfiguration.class, Jenkins.class })
public class ExecutePolarisCliStepTest {
    public static final String TEST_POLARIS_ARGS = "testArgs";
    private static final String WORKSPACE_REL_PATH = "out/test/PolarisBuildStepTest/testPerform/workspace";
    private static final String TEST_POLARIS_CLI_NAME = "testPolarisCliName";
    private static final String TEST_POLARIS_HOME = "/tmp/polaris";

    // TODO: To improve this test (to test more of ExecutePolarisCliStep.Execution.run(), we could refactor ExecutePolarisCliStep to:
    // - Separate object creation (CreatePolarisEnvironment, GetPathToPolarisCli, ExecutePolarisCli) out of ExecutePolarisCliStep,
    //   so the objects can be mocked in this test.
    //   Then we can verify that they are created correctly, and verify that StepWorkflow.first().then().then().run().getDataOrThrowException() was all
    //   done correctly (verify the arguments passed).
    @Test
    public void test() throws Exception {
        ExecutePolarisCliStep executePolarisCliStep = new ExecutePolarisCliStep(TEST_POLARIS_ARGS);
        executePolarisCliStep.setPolarisCli(TEST_POLARIS_CLI_NAME);

        StepContext stepContext = Mockito.mock(StepContext.class);
        Node node = Mockito.mock(Node.class);
        Mockito.when(stepContext.get(Node.class)).thenReturn(node);
        EnvVars envVars = Mockito.mock(EnvVars.class);
        Mockito.when(stepContext.get(EnvVars.class)).thenReturn(envVars);

        TaskListener listener = Mockito.mock(TaskListener.class);
        Mockito.when(stepContext.get(TaskListener.class)).thenReturn(listener);
        Launcher launcher = Mockito.mock(Launcher.class);
        VirtualChannel virtualChannel = Mockito.mock(VirtualChannel.class);
        Mockito.when(launcher.getChannel()).thenReturn(virtualChannel);
        Mockito.when(stepContext.get(Launcher.class)).thenReturn(launcher);

        FilePath workspaceFilePath = new FilePath(new File(WORKSPACE_REL_PATH));
        Mockito.when(stepContext.get(FilePath.class)).thenReturn(workspaceFilePath);
        ExecutePolarisCliStep.Execution stepExecution = (ExecutePolarisCliStep.Execution) executePolarisCliStep.start(stepContext);

        PolarisGlobalConfig polarisGlobalConfig = Mockito.mock(PolarisGlobalConfig.class);
        ExtensionList extensionList = Mockito.mock(ExtensionList.class);
        PowerMockito.mockStatic(GlobalConfiguration.class);
        Mockito.when(GlobalConfiguration.all()).thenReturn(extensionList);
        Mockito.when(extensionList.get(PolarisGlobalConfig.class)).thenReturn(polarisGlobalConfig);

        PolarisServerConfig polarisServerConfig = Mockito.mock(PolarisServerConfig.class);
        Mockito.when(polarisGlobalConfig.getPolarisServerConfig(Mockito.any(), Mockito.any())).thenReturn(polarisServerConfig);

        PolarisCli polarisCli = PowerMockito.mock(PolarisCli.class);
        PowerMockito.mockStatic(PolarisCli.class);
        Mockito.when(PolarisCli.installationsExist()).thenReturn(true);
        Mockito.when(PolarisCli.findInstallationWithName(TEST_POLARIS_CLI_NAME)).thenReturn(Optional.of(polarisCli));

        Mockito.when(polarisCli.forEnvironment(envVars)).thenReturn(polarisCli);
        Mockito.when(polarisCli.forNode(node, listener)).thenReturn(polarisCli);
        Mockito.when(polarisCli.getHome()).thenReturn(TEST_POLARIS_HOME);

        PowerMockito.mockStatic(StepWorkflow.class);
        StepWorkflow.Builder stepWorkflowBuilder = Mockito.mock(StepWorkflow.Builder.class);
        Mockito.when(StepWorkflow.first(Mockito.any(SubStep.class))).thenReturn(stepWorkflowBuilder);
        Mockito.when(stepWorkflowBuilder.then(Mockito.any(SubStep.class))).thenReturn(stepWorkflowBuilder);

        StepWorkflow stepWorkflow = Mockito.mock(StepWorkflow.class);
        StepWorkflowResponse stepWorkflowResponse = Mockito.mock(StepWorkflowResponse.class);
        Mockito.when(stepWorkflowBuilder.build()).thenReturn(stepWorkflow);
        Mockito.when(stepWorkflow.run()).thenReturn(stepWorkflowResponse);
        Mockito.when(stepWorkflowResponse.getDataOrThrowException()).thenReturn(123);

        PowerMockito.mockStatic(Jenkins.class);
        Jenkins mockedJenkins = Mockito.mock(Jenkins.class);
        Mockito.when(Jenkins.getInstanceOrNull()).thenReturn(mockedJenkins);

        Integer result = stepExecution.run();

        assertEquals(Integer.valueOf(123), result);
    }
}

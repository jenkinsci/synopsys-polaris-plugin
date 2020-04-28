package com.synopsys.integration.jenkins.polaris.workflow;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.stepworkflow.SubStepResponse;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Launcher.ProcStarter.class, Launcher.class })
public class ExecutePolarisCliTest {
    private static final String WORKSPACE_PATH = "out/test/workspace";
    private static final String POLARIS_ARGUMENTS = "test arguments";
    private static final String POLARIS_CLI_PATH = "/tmp/polariscli";
    private static final Integer EXPECTED_RETURN_CODE = Integer.valueOf(0);

    @Test
    public void testRun() throws IOException, InterruptedException {
        final JenkinsIntLogger logger = Mockito.mock(JenkinsIntLogger.class);
        final Launcher launcher = PowerMockito.mock(Launcher.class);

        final IntEnvironmentVariables intEnvironmentVariables = new IntEnvironmentVariables(false);
        final Map<String, String> envVarMap = new HashMap<>();
        envVarMap.put("TEST_VARIABLE", "test value");
        intEnvironmentVariables.putAll(envVarMap);

        final FilePath workspace = new FilePath(new File(WORKSPACE_PATH));
        final TaskListener listener = Mockito.mock(TaskListener.class);
        final String polarisArguments = POLARIS_ARGUMENTS;

        final ExecutePolarisCli executePolarisCli = new ExecutePolarisCli(logger, launcher, intEnvironmentVariables, workspace, listener, polarisArguments);

        final SubStepResponse<String> previousResponse = Mockito.mock(SubStepResponse.class);
        Mockito.when(previousResponse.isFailure()).thenReturn(false);
        Mockito.when(previousResponse.hasData()).thenReturn(true);
        Mockito.when(previousResponse.getData()).thenReturn(POLARIS_CLI_PATH);

        final Launcher.ProcStarter procStarter = PowerMockito.mock(Launcher.ProcStarter.class);
        Mockito.when(launcher.launch()).thenReturn(procStarter);

        final ArgumentListBuilder argumentListBuilder = new ArgumentListBuilder();
        argumentListBuilder.add(POLARIS_CLI_PATH);
        argumentListBuilder.addTokenized(polarisArguments);

        Mockito.when(procStarter.cmds(Mockito.argThat(new ArgumentListBuilderMatcher(argumentListBuilder)))).thenReturn(procStarter);
        Mockito.when(procStarter.envs(envVarMap)).thenReturn(procStarter);
        Mockito.when(procStarter.pwd(workspace)).thenReturn(procStarter);
        Mockito.when(procStarter.stdout(listener)).thenReturn(procStarter);
        Mockito.when(procStarter.quiet(true)).thenReturn(procStarter);
        Mockito.when(procStarter.join()).thenReturn(Integer.valueOf(EXPECTED_RETURN_CODE));

        final SubStepResponse<Integer> response = executePolarisCli.run(previousResponse);

        final Integer actualReturnCode = response.getData();
        assertEquals(EXPECTED_RETURN_CODE, actualReturnCode);

    }

    public class ArgumentListBuilderMatcher implements ArgumentMatcher<ArgumentListBuilder> {
        private final ArgumentListBuilder left;

        public ArgumentListBuilderMatcher(final ArgumentListBuilder left) {
            this.left = left;
        }

        @Override
        public boolean matches(final ArgumentListBuilder right) {
            if ((right == null) || (right.toCommandArray().length != left.toCommandArray().length)) {
                return false;
            }
            for (int i = 0; i < right.toCommandArray().length; i++) {
                if (!right.toCommandArray()[i].equals(left.toCommandArray()[i])) {
                    return false;
                }
            }
            return true;
        }
    }
}

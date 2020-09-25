package com.synopsys.integration.jenkins.polaris.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.util.OperatingSystemType;

public class PolarisCliArgumentServiceTest {
    private static final String POLARIS_CLI_PATH = "/tmp/polariscli";

    private static Stream<Arguments> getWindowsArgumentsAndExpected() {
        return Stream.of(
            Arguments.of(Collections.emptyList(), Collections.singletonList(POLARIS_CLI_PATH)),
            Arguments.of(Arrays.asList("--co", "project.name=\"newProjectname\"", "analyze"), Arrays.asList(POLARIS_CLI_PATH, "--co", "project.name=\\\"newProjectname\\\"", "analyze"))
        );
    }

    private static Stream<Arguments> getUnixArgumentsAndExpected() {
        return Stream.of(
            Arguments.of(Collections.emptyList(), Collections.singletonList(POLARIS_CLI_PATH)),
            Arguments.of(Arrays.asList("--co", "project.name=\"newProjectname\"", "analyze"), Arrays.asList(POLARIS_CLI_PATH, "--co", "project.name=\"newProjectname\"", "analyze"))
        );
    }

    @ParameterizedTest
    @MethodSource("getUnixArgumentsAndExpected")
    public void testFinalizePolarisArgumentsLinux(List<String> tokenizedPolarisArguments, List<String> expectedFinalizedPolarisArguments) {
        JenkinsIntLogger logger = Mockito.mock(JenkinsIntLogger.class);

        PolarisCliArgumentService polarisCliArgumentService = new PolarisCliArgumentService(logger);
        List<String> finalizedPolarisArguments = polarisCliArgumentService.finalizePolarisCliArguments(OperatingSystemType.LINUX, POLARIS_CLI_PATH, tokenizedPolarisArguments);

        Assertions.assertTrue(finalizedPolarisArguments.containsAll(expectedFinalizedPolarisArguments));
        Assertions.assertTrue(expectedFinalizedPolarisArguments.containsAll(finalizedPolarisArguments));
    }

    @ParameterizedTest
    @MethodSource("getUnixArgumentsAndExpected")
    public void testFinalizePolarisArgumentsMac(List<String> tokenizedPolarisArguments, List<String> expectedFinalizedPolarisArguments) {
        JenkinsIntLogger logger = Mockito.mock(JenkinsIntLogger.class);

        PolarisCliArgumentService polarisCliArgumentService = new PolarisCliArgumentService(logger);
        List<String> finalizedPolarisArguments = polarisCliArgumentService.finalizePolarisCliArguments(OperatingSystemType.MAC, POLARIS_CLI_PATH, tokenizedPolarisArguments);

        Assertions.assertTrue(finalizedPolarisArguments.containsAll(expectedFinalizedPolarisArguments));
        Assertions.assertTrue(expectedFinalizedPolarisArguments.containsAll(finalizedPolarisArguments));
    }

    @ParameterizedTest
    @MethodSource("getWindowsArgumentsAndExpected")
    public void testFinalizePolarisArgumentsWindows(List<String> tokenizedPolarisArguments, List<String> expectedFinalizedPolarisArguments) {
        JenkinsIntLogger logger = Mockito.mock(JenkinsIntLogger.class);

        PolarisCliArgumentService polarisCliArgumentService = new PolarisCliArgumentService(logger);
        List<String> finalizedPolarisArguments = polarisCliArgumentService.finalizePolarisCliArguments(OperatingSystemType.WINDOWS, POLARIS_CLI_PATH, tokenizedPolarisArguments);

        Assertions.assertTrue(finalizedPolarisArguments.containsAll(expectedFinalizedPolarisArguments));
        Assertions.assertTrue(expectedFinalizedPolarisArguments.containsAll(finalizedPolarisArguments));
    }

}

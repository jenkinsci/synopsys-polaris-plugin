package com.synopsys.integration.jenkins.polaris.workflow;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.Mockito;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.polaris.extensions.tools.FindOrInstallPolarisCli;
import com.synopsys.integration.polaris.common.cli.PolarisDownloadUtility;

// If env var TEST_POLARIS_URL is set, testInstall() downloads the Polaris CLI from that Polaris server
public class FindOrInstallPolarisCliTestIT {
    public static final String POLARIS_URL_ENVVAR_NAME = "TEST_POLARIS_URL";
    private static final int POLARIS_CLI_DOWNLOAD_TIMEOUT_SECONDS = 90;
    private static final String INSTALLATION_DIR_PARENT_PATH = "out/test/polariscli";
    private static final String POLARIS_VERSIONFILE_NAME = "polarisVersion.txt";
    private static final String POLARIS_LINUX_BINARY = "polaris";
    private static final String POLARIS_WINDOWS_BINARY = "polaris.exe";

    @Test
    public void testInstall() throws IntegrationException {
        String polarisServerUrl = System.getenv(POLARIS_URL_ENVVAR_NAME);
        assumeTrue("Environment variable TEST_POLARIS_URL is not set. Skipping FindOrInstallPolarisCliTest.testInstall().", StringUtils.isNotBlank(polarisServerUrl));

        System.out.printf("Attempting to download Polaris CLI from %s\n", polarisServerUrl);
        JenkinsIntLogger jenkinsIntLogger = Mockito.mock(JenkinsIntLogger.class);
        File requestedInstallationDirParent = new File(INSTALLATION_DIR_PARENT_PATH);
        FileUtils.deleteQuietly(requestedInstallationDirParent); // Force a new download

        // Test
        FindOrInstallPolarisCli findOrInstallPolarisCli = new FindOrInstallPolarisCli(jenkinsIntLogger, polarisServerUrl, POLARIS_CLI_DOWNLOAD_TIMEOUT_SECONDS, null, 0,
            null, null, null, null, INSTALLATION_DIR_PARENT_PATH);
        String returnedInstallationDirPath = findOrInstallPolarisCli.call();

        // Verify
        Path versionFile = requestedInstallationDirParent
                               .toPath()
                               .resolve(PolarisDownloadUtility.POLARIS_CLI_INSTALL_DIRECTORY)
                               .resolve(POLARIS_VERSIONFILE_NAME);
        assertTrue("Expected " + versionFile.toString() + " to exist but it did not.", Files.exists(versionFile));

        // Verify bin directory exists
        Path bin = Paths.get(returnedInstallationDirPath)
                       .resolve("bin");
        assertTrue("Expected " + bin.toString() + " to exist but it did not.", Files.exists(bin));

        // Verify executable exists
        Path executable;
        if (SystemUtils.IS_OS_WINDOWS) {
            executable = bin.resolve(POLARIS_WINDOWS_BINARY);
        } else {
            executable = bin.resolve(POLARIS_LINUX_BINARY);
        }

        assertTrue("Expected " + executable.toString() + " to exist but it did not.", Files.exists(executable));
    }

    private long countMatchingFilepaths(File dir, String pathSubstring) {
        Collection<File> dirContents = FileUtils.listFilesAndDirs(dir, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
        return dirContents.stream()
                   .map(File::getAbsolutePath)
                   .filter(filePath -> filePath.contains(pathSubstring))
                   .count();
    }
}

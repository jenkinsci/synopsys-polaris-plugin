package com.synopsys.integration.jenkins.polaris.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.Test;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.Mockito;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.polaris.extensions.tools.FindOrInstallPolarisCli;

// If env var TEST_POLARIS_URL is set, testInstall() downloads the Polaris CLI from that Polaris server
public class FindOrInstallPolarisCliTest {
    public static final String POLARIS_URL_ENVVAR_NAME = "TEST_POLARIS_URL";
    private static final int POLARIS_CLI_DOWNLOAD_TIMEOUT_SECONDS = 90;
    private static final String INSTALLATION_DIR_PARENT_PATH = "out/test/polariscli";
    private static final long NUM_POLARIS_EXECUTABLES_EXPECTED = 3L;
    private static final String POLARIS_VERSIONFILE_NAME = "polarisVersion.txt";
    private static final String POLARIS_EXE_PATH_SUBSTRING = "/bin/polaris";

    @Test
    public void testInstall() throws IntegrationException {

        final String polarisServerUrl = System.getenv(POLARIS_URL_ENVVAR_NAME);
        assumeTrue(StringUtils.isNotBlank(polarisServerUrl), "Environment variable TEST_POLARIS_URL is not set. Skipping FindOrInstallPolarisCliTest.testInstall().");

        System.out.printf("Attempting to download Polaris CLI from %s\n", polarisServerUrl);
        final JenkinsIntLogger jenkinsIntLogger = Mockito.mock(JenkinsIntLogger.class);
        final File requestedInstallationDirParent = new File(INSTALLATION_DIR_PARENT_PATH);
        FileUtils.deleteQuietly(requestedInstallationDirParent); // Force a new download

        // Test
        final FindOrInstallPolarisCli findOrInstallPolarisCli = new FindOrInstallPolarisCli(jenkinsIntLogger, polarisServerUrl, POLARIS_CLI_DOWNLOAD_TIMEOUT_SECONDS, null, 0,
            null, null, null, null, INSTALLATION_DIR_PARENT_PATH);
        final String returnedInstallationDirPath = findOrInstallPolarisCli.call();

        // Verify
        final long numVersionFiles = countMatchingFilepaths(requestedInstallationDirParent, POLARIS_VERSIONFILE_NAME);
        assertEquals(1L, numVersionFiles);

        // Verify that 3 polaris executables were installed
        final File returnedInstallationDir = new File(returnedInstallationDirPath);
        final long numExesFoundInReturnedInstallationDir = countMatchingFilepaths(returnedInstallationDir, POLARIS_EXE_PATH_SUBSTRING);
        assertEquals(NUM_POLARIS_EXECUTABLES_EXPECTED, numExesFoundInReturnedInstallationDir);

        // Verify that they were installed in the location we requested
        final long numExesFoundInRequestedInstallationDirParent = countMatchingFilepaths(requestedInstallationDirParent, POLARIS_EXE_PATH_SUBSTRING);
        assertEquals(NUM_POLARIS_EXECUTABLES_EXPECTED, numExesFoundInRequestedInstallationDirParent);
    }

    private long countMatchingFilepaths(final File dir, final String pathSubstring) {
        final Collection<File> dirContents = FileUtils.listFilesAndDirs(dir, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
        return dirContents.stream()
                   .map(File::getAbsolutePath)
                   .filter(filePath -> filePath.contains(pathSubstring))
                   .count();
    }
}

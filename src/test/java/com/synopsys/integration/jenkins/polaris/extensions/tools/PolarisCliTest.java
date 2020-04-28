package com.synopsys.integration.jenkins.polaris.extensions.tools;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.DescriptorExtensionList;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolLocationNodeProperty;
import hudson.tools.ToolProperty;
import hudson.tools.ZipExtractionInstaller;
import jenkins.model.Jenkins;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ToolLocationNodeProperty.class, ToolInstallation.class, Jenkins.class, PolarisCli.DescriptorImpl.class })
public class PolarisCliTest {

    public static final String INSTALLER_URL = "https://nodejs.org/dist/v4.2.4/node-v4.2.4-linux-x64.tar.gz";
    public static final String CLI_LABEL = "testLabel";
    public static final String CLI_HOME = "testHome";
    public static final String CLI_NAME = "testName";
    public static final String CLI_SUBDIR = "testSubdir";

    @Test
    public void testForNode() throws IOException, InterruptedException {
        // Setup
        final List<ToolInstaller> installers = new ArrayList<ToolInstaller>();
        installers.add(new ZipExtractionInstaller(CLI_LABEL, INSTALLER_URL, CLI_SUBDIR));
        final List<InstallSourceProperty> properties = new ArrayList<>();
        properties.add(new InstallSourceProperty(installers));

        final PolarisCli polarisCli = new PolarisCli(CLI_NAME, CLI_HOME, properties);
        final Node node = Mockito.mock(Node.class);
        final TaskListener taskListener = Mockito.mock(TaskListener.class);

        PowerMockito.mockStatic(ToolLocationNodeProperty.class);
        Mockito.when(ToolLocationNodeProperty.getToolHome(Mockito.any(Node.class), Mockito.any(ToolInstallation.class), Mockito.any(TaskListener.class))).thenReturn(CLI_HOME);

        // Test
        final PolarisCli createdPolarisCli = polarisCli.forNode(node, taskListener);

        // Verify
        assertEquals(CLI_NAME, createdPolarisCli.getName());
        assertEquals(CLI_HOME, createdPolarisCli.getHome());
        final InstallSourceProperty installSourceProperty = (InstallSourceProperty) createdPolarisCli.getProperties().get(0);
        final ZipExtractionInstaller zipExtractionInstaller = (ZipExtractionInstaller) installSourceProperty.installers.get(0);
        assertEquals(INSTALLER_URL, zipExtractionInstaller.getUrl());
        assertEquals(CLI_LABEL, zipExtractionInstaller.getLabel());
    }

    @Test
    public void testFindInstanceWithName() throws IOException {
        // Setup
        final List<ToolInstaller> installers = new ArrayList<ToolInstaller>();
        installers.add(new ZipExtractionInstaller(CLI_LABEL, INSTALLER_URL, CLI_SUBDIR));
        final List<InstallSourceProperty> properties = new ArrayList<>();
        final InstallSourceProperty testInstallersProperty = new InstallSourceProperty(installers);
        properties.add(testInstallersProperty);

        PowerMockito.mockStatic(ToolInstallation.class);
        final DescriptorExtensionList<ToolInstallation, ToolDescriptor<?>> descriptorExtensionList = Mockito.mock(DescriptorExtensionList.class);
        Mockito.when(ToolInstallation.all()).thenReturn(descriptorExtensionList);

        final PolarisCli.DescriptorImpl polarisCliToolDescriptor = PowerMockito.mock(PolarisCli.DescriptorImpl.class);
        final PolarisCli polarisCli = new PolarisCli(CLI_NAME, CLI_HOME, properties);
        final PolarisCli[] polarisClis = { polarisCli };
        Mockito.when(polarisCliToolDescriptor.getInstallations()).thenReturn(polarisClis);
        Mockito.when(descriptorExtensionList.get(PolarisCli.DescriptorImpl.class)).thenReturn(polarisCliToolDescriptor);

        // Test
        final Optional<PolarisCli> maybeFoundPolarisCli = polarisCli.findInstallationWithName(CLI_NAME);

        // Verify
        final PolarisCli foundPolarisCli = maybeFoundPolarisCli.get();
        assertEquals(CLI_NAME, foundPolarisCli.getName());
        assertEquals(CLI_HOME, foundPolarisCli.getHome());
        final ToolProperty foundInstallSourceProperty = foundPolarisCli.getProperties().get(0);
        assertEquals(testInstallersProperty, foundInstallSourceProperty);
    }
}

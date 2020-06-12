package com.synopsys.integration.jenkins.polaris.extensions.tools;

import hudson.AbortException;
import hudson.tools.ToolInstallation;

public class AbortPolarisCliInstallException extends AbortException {
    public AbortPolarisCliInstallException(ToolInstallation toolInstallation, String reason) {
        super("Cannot install Polaris CLI Installation " + toolInstallation.getName() + " because: " + reason);
    }

}

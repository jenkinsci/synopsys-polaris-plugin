/*
 * synopsys-polaris
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.polaris.extensions.tools;

import hudson.AbortException;
import hudson.tools.ToolInstallation;

public class AbortPolarisCliInstallException extends AbortException {
    public AbortPolarisCliInstallException(ToolInstallation toolInstallation, String reason) {
        super("Cannot install Polaris CLI Installation " + toolInstallation.getName() + " because: " + reason);
    }

}

/**
 * synopsys-polaris
 *
 * Copyright (c) 2020 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.jenkins.polaris.workflow;

import com.synopsys.integration.jenkins.polaris.extensions.tools.PolarisCli;
import com.synopsys.integration.stepworkflow.AbstractSupplyingSubStep;
import com.synopsys.integration.stepworkflow.SubStepResponse;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

public class FindPolarisCli extends AbstractSupplyingSubStep<String> {
    private final VirtualChannel virtualChannel;
    private final PolarisCli polarisCli;
    private final Node node;
    private final TaskListener taskListener;
    private final EnvVars envVars;

    public FindPolarisCli(final VirtualChannel virtualChannel, final PolarisCli polarisCli, final Node node, final TaskListener taskListener, final EnvVars envVars) {
        this.virtualChannel = virtualChannel;
        this.polarisCli = polarisCli;
        this.node = node;
        this.taskListener = taskListener;
        this.envVars = envVars;
    }

    @Override
    public SubStepResponse<String> run() {
        try {
            final String polarisCliToolHome = polarisCli.forEnvironment(envVars)
                                                  .forNode(node, taskListener)
                                                  .getHome();

            if (polarisCliToolHome == null) {
                throw new AbortException("The Polaris CLI installation home could not be determined for the configured Polaris CLI. Please ensure that this installation is correctly configured in the Global Tool Configuration.");
            }

            final GetPathToPolarisCli getPathToPolarisCli = new GetPathToPolarisCli(polarisCliToolHome);
            return SubStepResponse.SUCCESS(virtualChannel.call(getPathToPolarisCli));
        } catch (final Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return SubStepResponse.FAILURE(e);
        }
    }
}

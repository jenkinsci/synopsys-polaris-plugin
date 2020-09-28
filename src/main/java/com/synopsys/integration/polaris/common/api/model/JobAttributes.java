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
package com.synopsys.integration.polaris.common.api.model;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.annotations.SerializedName;
import com.synopsys.integration.polaris.common.api.PolarisAttributes;
import com.synopsys.integration.polaris.common.api.PolarisResponse;

public class JobAttributes extends PolarisResponse implements PolarisAttributes {
    private static final long serialVersionUID = -7775735940473228894L;
    @SerializedName("details")
    private HashMap<String, Object> details = null;

    @SerializedName("failureInfo")
    private FailureInfo failureInfo = null;

    @SerializedName("status")
    private JobStatus status = null;

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = new HashMap<>(details);
    }

    public FailureInfo getFailureInfo() {
        return failureInfo;
    }

    public void setFailureInfo(FailureInfo failureInfo) {
        this.failureInfo = failureInfo;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

}

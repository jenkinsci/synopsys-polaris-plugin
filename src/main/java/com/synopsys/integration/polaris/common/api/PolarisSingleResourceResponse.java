/*
 * synopsys-polaris
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.polaris.common.api;

import com.google.gson.annotations.SerializedName;

public class PolarisSingleResourceResponse<R extends PolarisResource> extends PolarisResponse {
    @SerializedName("data")
    private R data = null;

    /**
     * Get data
     * @return data
     */
    public R getData() {
        return data;
    }

    public void setData(R data) {
        this.data = data;
    }

}

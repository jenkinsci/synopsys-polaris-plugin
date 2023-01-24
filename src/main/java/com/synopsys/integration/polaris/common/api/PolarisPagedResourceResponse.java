/*
 * synopsys-polaris
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.polaris.common.api;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class PolarisPagedResourceResponse<R extends PolarisResource> extends PolarisResponse {
    @SerializedName("data")
    private List<R> data = null;
    @SerializedName("meta")
    private PolarisPaginationMeta meta = null;

    public List<R> getData() {
        return data;
    }

    public void setData(List<R> data) {
        this.data = data;
    }

    public PolarisPaginationMeta getMeta() {
        return meta;
    }

    public void setMeta(PolarisPaginationMeta meta) {
        this.meta = meta;
    }

}

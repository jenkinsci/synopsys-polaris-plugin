/*
 * synopsys-polaris
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.polaris.common.api;

import java.io.Serializable;

import com.synopsys.integration.util.Stringable;

public class PolarisResponse extends Stringable implements Serializable {
    private static final long serialVersionUID = 1968298547235080384L;
    private String json;

    public PolarisResponse() {
        this.json = null;
    }

    public PolarisResponse(String json) {
        this.json = json;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

}

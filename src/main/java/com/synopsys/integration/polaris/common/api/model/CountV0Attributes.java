/*
 * synopsys-polaris
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.polaris.common.api.model;

import com.google.gson.annotations.SerializedName;
import com.synopsys.integration.polaris.common.api.PolarisAttributes;
import com.synopsys.integration.polaris.common.api.PolarisResponse;

public class CountV0Attributes extends PolarisResponse implements PolarisAttributes {
    @SerializedName("value")
    private Integer value;

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

}


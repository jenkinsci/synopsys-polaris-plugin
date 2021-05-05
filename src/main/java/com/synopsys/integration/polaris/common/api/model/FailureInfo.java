/*
 * synopsys-polaris
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.polaris.common.api.model;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;
import com.synopsys.integration.util.Stringable;

public class FailureInfo extends Stringable implements Serializable {
    private static final long serialVersionUID = 9118125719091019152L;
    @SerializedName("userFriendlyFailureReason")
    private String userFriendlyFailureReason;

    @SerializedName("exception")
    private String exception;

    public String getUserFriendlyFailureReason() {
        return userFriendlyFailureReason;
    }

    public String getException() {
        return exception;
    }

}

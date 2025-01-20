package com.cavetale.cavetaleresourcepack;

import com.google.gson.annotations.SerializedName;

public final class TintSourceDye extends TintSource {
    @SerializedName("default")
    private int defaultValue = 0xffffff;

    public TintSourceDye() {
        super("minecraft:dye");
    }

    public TintSourceDye(final int defaultValue) {
        this();
        this.defaultValue = defaultValue;
    }
}

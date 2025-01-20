package com.cavetale.cavetaleresourcepack;

import com.google.gson.annotations.SerializedName;
import lombok.Setter;

/**
 * Represents any tint source with a default value.
 */
@Setter
public final class TintSourceDefault extends TintSource {
    @SerializedName("default")
    private int defaultValue = 0xffffff;

    public TintSourceDefault(final String type) {
        super(type);
    }

    public TintSourceDefault(final String type, final int defaultValue) {
        super(type);
        this.defaultValue = defaultValue;
    }
}

package com.cavetale.cavetaleresourcepack;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * A more practical abstraction of ModelJson.OverrideJson suited for
 * sorting by customModelData.
 */
@Data @RequiredArgsConstructor
public final class ModelOverride implements Comparable<ModelOverride> {
    protected final int customModelData;
    protected final PackPath item;
    protected Double bowPulling;
    protected Double bowPull;

    @Override
    public int compareTo(ModelOverride other) {
        return Integer.compare(customModelData, other.customModelData);
    }
}

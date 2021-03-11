package com.cavetale.cavetaleresourcepack;

import java.util.HashMap;
import java.util.Map;
import lombok.Value;

@Value
public final class ModelOverride implements Comparable<ModelOverride> {
    protected int customModelData;
    protected String itemName;

    @Override
    public int compareTo(ModelOverride other) {
        return Integer.compare(customModelData, other.customModelData);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("model", "mytems:item/" + itemName);
        Map<String, Object> predicate = new HashMap<>();
        predicate.put("custom_model_data", customModelData);
        result.put("predicate", predicate);
        return result;
    }
}

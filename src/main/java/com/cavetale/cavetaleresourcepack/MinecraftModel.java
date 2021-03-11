package com.cavetale.cavetaleresourcepack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public final class MinecraftModel {
    protected String name;
    protected Map<String, Object> map;
    protected List<ModelOverride> overrides = new ArrayList<>();

    MinecraftModel(final String name, final Map<String, Object> map) {
        this.name = name;
        this.map = map;
    }

    public Map<String, Object> cook() {
        Collections.sort(overrides);
        List<Map<String, Object>> list = new ArrayList<>();
        for (ModelOverride mo : overrides) {
            list.add(mo.toMap());
        }
        map.put("overrides", list);
        return map;
    }
}

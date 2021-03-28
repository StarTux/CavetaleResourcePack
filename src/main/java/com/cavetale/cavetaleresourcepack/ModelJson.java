package com.cavetale.cavetaleresourcepack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModelJson {
    protected String parent;
    protected Boolean ambientocclusion;
    @SuppressWarnings("MemberName")
    protected String gui_light;
    protected List<Object> elements;
    protected Map<String, Object> display;
    protected Map<String, String> textures;
    protected List<Object> overrides;

    public void setTexture(String key, String value) {
        if (textures == null) textures = new HashMap<>();
        textures.put(key, value);
    }

    public void addOverrides(List<ModelOverride> overrideList) {
        List<Object> newOverrides = new ArrayList<>();
        for (ModelOverride override : overrideList) {
            newOverrides.add(override.toMap());
        }
        if (this.overrides != null) {
            newOverrides.addAll(this.overrides);
        }
        this.overrides = newOverrides;
    }

    public List<String> getTextureKeys() {
        if (textures == null) return new ArrayList<>(0);
        return new ArrayList<>(textures.keySet());
    }

    public String getTexture(String key) {
        if (textures == null) return null;
        return textures.get(key);
    }
}

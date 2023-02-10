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
    protected List<OverrideJson> overrides;

    public static final class OverrideJson {
        Map<String, Object> predicate = new HashMap<>();
        String model;
    }

    public void setTexture(String key, String value) {
        if (textures == null) textures = new HashMap<>();
        textures.put(key, value);
    }

    public void addOverrides(List<ModelOverride> overrideList) {
        List<OverrideJson> newOverrides = new ArrayList<>();
        if (this.overrides != null) {
            newOverrides.addAll(this.overrides);
        }
        for (ModelOverride override : overrideList) {
            OverrideJson obj = new OverrideJson();
            obj.predicate.put("custom_model_data", override.customModelData);
            if (override.bowPulling != null) {
                obj.predicate.put("pulling", override.bowPulling);
            }
            if (override.bowPull != null) {
                obj.predicate.put("pull", override.bowPull);
            }
            if (override.blocking != null) {
                obj.predicate.put("blocking", override.blocking);
            }
            if (override.angle != null) {
                obj.predicate.put("angle", override.angle);
            }
            obj.model = override.item.toString();
            newOverrides.add(obj);
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

    @Override
    public String toString() {
        return Json.serialize(this);
    }
}

package com.cavetale.cavetaleresourcepack;

import java.util.ArrayList;
import java.util.List;
import lombok.Setter;

@Setter
public final class ItemModelModel extends ItemModel {
    private String model;
    private List<TintSource> tints;

    ItemModelModel() {
        super("model");
    }

    public ItemModelModel(final String modelPath) {
        this();
        this.model = modelPath;
    }

    public void addDyeTint(final int defaultValue) {
        if (tints == null) tints = new ArrayList<>();
        tints.add(new TintSourceDye(defaultValue));
    }

    public void addConstantTint(final int value) {
        if (tints == null) tints = new ArrayList<>();
        tints.add(new TintSourceConstant(value));
    }

    public void addFireworkTint(final int defaultValue) {
        if (tints == null) tints = new ArrayList<>();
        tints.add(new TintSourceDefault("minecraft:firework", defaultValue));
    }

    public void addPotionTint(final int defaultValue) {
        if (tints == null) tints = new ArrayList<>();
        tints.add(new TintSourceDefault("minecraft:potion", defaultValue));
    }

    public void addMapColorTint(final int defaultValue) {
        if (tints == null) tints = new ArrayList<>();
        tints.add(new TintSourceDefault("minecraft:map_color", defaultValue));
    }

    public void addTeamTint(final int defaultValue) {
        if (tints == null) tints = new ArrayList<>();
        tints.add(new TintSourceDefault("minecraft:team", defaultValue));
    }
}

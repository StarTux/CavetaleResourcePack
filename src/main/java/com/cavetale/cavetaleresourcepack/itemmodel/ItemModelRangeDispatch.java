package com.cavetale.cavetaleresourcepack;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
public class ItemModelRangeDispatch extends ItemModel {
    private final String property;
    private Float scale = 1f;
    private List<Entry> entries = new ArrayList<>();
    private ItemModel fallback;

    public ItemModelRangeDispatch(final String property) {
        super("minecraft:range_dispatch");
        this.property = property;
    }

    @RequiredArgsConstructor
    public static final class Entry {
        public final float threshold;
        public final ItemModel model;
    }

    public void addModelEntry(float threshold, String modelPath) {
        entries.add(new Entry(threshold, new ItemModelModel(modelPath)));
    }
}

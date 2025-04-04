package com.cavetale.cavetaleresourcepack.itemmodel;

import lombok.Setter;

@Setter
public final class ItemModelRangeDispatchUseDuration extends ItemModelRangeDispatch {
    private Boolean remaining;

    public ItemModelRangeDispatchUseDuration() {
        super("minecraft:use_duration");
    }
}

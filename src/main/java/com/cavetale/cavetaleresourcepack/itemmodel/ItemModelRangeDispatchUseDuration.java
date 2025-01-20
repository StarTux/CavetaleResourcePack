package com.cavetale.cavetaleresourcepack;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
public final class ItemModelRangeDispatchUseDuration extends ItemModelRangeDispatch {
    private Boolean remaining;

    public ItemModelRangeDispatchUseDuration() {
        super("minecraft:use_duration");
    }
}

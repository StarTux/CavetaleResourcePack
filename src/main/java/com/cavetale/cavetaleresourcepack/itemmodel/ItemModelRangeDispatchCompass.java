package com.cavetale.cavetaleresourcepack;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
public final class ItemModelRangeDispatchCompass extends ItemModelRangeDispatch {
    private String target;
    private Boolean wobble; // Default: true

    public ItemModelRangeDispatchCompass() {
        super("minecraft:compass");
    }

    public void setSpawn() {
        target = "spawn";
    }

    public void setLodestone() {
        target = "lodestone";
    }

    public void setRecovery() {
        target = "recovery";
    }

    public void setNone() {
        target = "none";
    }
}

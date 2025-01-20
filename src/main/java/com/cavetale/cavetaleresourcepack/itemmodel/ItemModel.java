package com.cavetale.cavetaleresourcepack;

import java.io.Serializable;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class ItemModel implements Serializable {
    public final String type;
}

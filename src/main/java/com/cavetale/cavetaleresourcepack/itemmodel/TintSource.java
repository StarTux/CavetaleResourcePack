package com.cavetale.cavetaleresourcepack.itemmodel;

import java.io.Serializable;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class TintSource implements Serializable {
    public final String type;
}

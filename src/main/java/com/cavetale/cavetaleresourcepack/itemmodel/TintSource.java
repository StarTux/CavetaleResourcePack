package com.cavetale.cavetaleresourcepack;

import java.io.Serializable;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class TintSource implements Serializable {
    public final String type;
}

package com.cavetale.cavetaleresourcepack;

public final class TintSourceConstant extends TintSource {
    private int value = 0xffffff;

    public TintSourceConstant() {
        super("minecraft:constant");
    }

    public TintSourceConstant(final int value) {
        this();
        this.value = value;
    }
}

package com.cavetale.cavetaleresourcepack.itemmodel;

import lombok.Setter;

@Setter
public class ItemModelCondition extends ItemModel {
    private String property;
    @SuppressWarnings("MemberName")
    private ItemModel on_true;
    @SuppressWarnings("MemberName")
    private ItemModel on_false;

    public ItemModelCondition() {
        super("minecraft:condition");
    }

    public ItemModelCondition(final String property) {
        this();
        this.property = property;
    }

    public final void setUsingItem() {
        property = "using_item";
    }

    public final void setOnTrue(final ItemModel model) {
        this.on_true = model;
    }

    public final void setOnFalse(final ItemModel model) {
        this.on_false = model;
    }
}

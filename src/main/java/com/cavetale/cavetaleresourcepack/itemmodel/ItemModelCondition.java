package com.cavetale.cavetaleresourcepack;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

    public void setUsingItem() {
        property = "using_item";
    }

    public void setOnTrue(final ItemModel model) {
        this.on_true = model;
    }

    public void setOnFalse(final ItemModel model) {
        this.on_false = model;
    }
}

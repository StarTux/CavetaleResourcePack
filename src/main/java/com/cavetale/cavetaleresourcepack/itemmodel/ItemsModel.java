package com.cavetale.cavetaleresourcepack.itemmodel;

import java.io.Serializable;

public final class ItemsModel implements Serializable {
    private ItemModel model;
    @SuppressWarnings("MemberName")
    private Boolean hand_animation_on_swap = null;

    public ItemsModel model(String modelPath) {
        ItemModelModel modelModel = new ItemModelModel();
        modelModel.setModel(modelPath);
        this.model = modelModel;
        return this;
    }

    public ItemModelModel makeModel(String modelPath) {
        ItemModelModel modelModel = new ItemModelModel();
        modelModel.setModel(modelPath);
        this.model = modelModel;
        return modelModel;
    }

    public ItemModelRangeDispatchCompass makeCompass() {
        final ItemModelRangeDispatchCompass compass = new ItemModelRangeDispatchCompass();
        this.model = compass;
        return compass;
    }

    public ItemModelRangeDispatchUseDuration makeUseDuration() {
        final ItemModelRangeDispatchUseDuration useDuration = new ItemModelRangeDispatchUseDuration();
        this.model = useDuration;
        return useDuration;
    }

    public ItemModelCondition makeCondition() {
        ItemModelCondition condition = new ItemModelCondition();
        this.model = condition;
        return condition;
    }
}

package com.cavetale.cavetaleresourcepack;

import com.cavetale.mytems.Animation;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public final class AnimationJson {
    protected Boolean interpolate;
    protected Integer width;
    protected Integer height;
    protected Integer frametime;
    protected List<Object> frames;

    @Data @AllArgsConstructor
    public static final class Frame {
        protected int index;
        protected int time;
    }

    @Data @AllArgsConstructor
    public static final class Container {
        AnimationJson animation;
    }

    public void normalize() {
        if (frames != null) {
            for (int i = 0; i < frames.size(); i += 1) {
                Object o = frames.get(i);
                if (o instanceof Number n) {
                    frames.set(i, n.intValue());
                } else {
                    frames.set(i, Json.deserialize(Json.serialize(o), Frame.class));
                }
            }
        }
    }

    public void copy(Animation animation) {
        if (animation.interpolate) this.interpolate = true;
        if (animation.width != 0) this.width = animation.width;
        if (animation.height != 0) this.height = animation.height;
        if (animation.frametime != 0) this.frametime = animation.frametime;
        if (animation.frames != null && !animation.frames.isEmpty()) {
            frames = new ArrayList<>();
            for (Animation.Frame it : animation.frames) {
                if (it.time == 0) {
                    frames.add(it.index);
                } else {
                    frames.add(new Frame(it.index, it.time));
                }
            }
        }
        if (frametime == null && frames == null) {
            frametime = 1;
        }
    }

    public static AnimationJson.Container ofMytemsAnimation(Animation animation) {
        AnimationJson result = new AnimationJson();
        result.copy(animation);
        return new Container(result);
    }
}

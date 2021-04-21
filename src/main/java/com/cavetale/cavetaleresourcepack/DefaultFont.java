package com.cavetale.cavetaleresourcepack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public enum DefaultFont {
    // Inv title to left edge
    BACKSPACE_10(PackPath.cavetaleFont("space"), -32768, -10, (char) 0xE001),
    // Inv right edge to title
    BACKSPACE_171(PackPath.cavetaleFont("space"), -32768, -171, (char) 0xE002),
    RAID_REWARD(PackPath.cavetaleFont("raid_reward"), 130, 256, '\uE101'),
    EASTER_EGG(PackPath.cavetaleFont("easter_egg"), 8, 8, '\uE102'),
    EASTER_BUNNY(PackPath.mytemsItem("easter_token"), 8, 8, '\uE103'),
    KITTY_COIN(PackPath.mytemsItem("kitty_coin"), 8, 8, '\uE104');

    public static final int MIN_ASCENT = -32768;

    public final PackPath file;
    public final int ascent;
    public final int height;
    public final char character;

    DefaultFont(final PackPath file, final int ascent, final int height, final char character) {
        this.file = file;
        this.ascent = ascent;
        this.height = height;
        this.character = character;
    }

    public FontProviderJson toJson(Map<PackPath, PackPath> pathMap) {
        FontProviderJson json = new FontProviderJson();
        json.setType("bitmap");
        json.setFile(pathMap.getOrDefault(file, file).toString() + ".png");
        json.setAscent(ascent);
        json.setHeight(height);
        json.setChars(Arrays.asList("" + character));
        return json;
    }

    public static List<FontProviderJson> toList(Map<PackPath, PackPath> pathMap) {
        List<FontProviderJson> list = new ArrayList<>();
        for (DefaultFont it : DefaultFont.values()) {
            list.add(it.toJson(pathMap));
        }
        return list;
    }
}

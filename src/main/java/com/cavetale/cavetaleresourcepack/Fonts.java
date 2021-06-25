package com.cavetale.cavetaleresourcepack;

import com.cavetale.core.font.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class Fonts {
    private Fonts() { }

    public static FontProviderJson toJson(Font font, Map<PackPath, PackPath> pathMap) {
        FontProviderJson json = new FontProviderJson();
        json.setType("bitmap");
        PackPath file = PackPath.fromString(font.getFilename());
        json.setFile(pathMap.getOrDefault(file, file).toString() + ".png");
        json.setAscent(font.getAscent());
        json.setHeight(font.getAscent());
        json.setChars(Arrays.asList("" + font.getCharacter()));
        return json;
    }

    public static <E extends Enum<E> & Font> List<FontProviderJson> toList(Class<E> fontClass, Map<PackPath, PackPath> pathMap) {
        List<FontProviderJson> list = new ArrayList<>();
        for (Font it : fontClass.getEnumConstants()) {
            list.add(toJson(it, pathMap));
        }
        return list;
    }
}

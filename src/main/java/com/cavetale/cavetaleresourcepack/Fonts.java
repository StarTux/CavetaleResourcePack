package com.cavetale.cavetaleresourcepack;

import com.cavetale.core.font.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
        json.setHeight(font.getHeight());
        json.setChars(new ArrayList<>(List.of("" + font.getCharacter())));
        return json;
    }

    public static <E extends Enum<E> & Font> List<FontProviderJson> toList(Class<E> fontClass, Map<PackPath, PackPath> pathMap) {
        List<FontProviderJson> list = new ArrayList<>();
        Map<String, FontProviderJson> filenameMap = new HashMap<>();
        for (Font it : fontClass.getEnumConstants()) {
            FontProviderJson json = filenameMap.get(it.getFilename());
            if (json != null) {
                json.getChars().add("" + it.getCharacter());
            } else {
                json = toJson(it, pathMap);
                list.add(json);
                filenameMap.put(it.getFilename(), json);
            }
        }
        return list;
    }
}

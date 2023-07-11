package com.cavetale.cavetaleresourcepack;

import com.cavetale.core.font.Font;
import java.util.ArrayList;
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
        List<String> chars = new ArrayList<>();
        chars.add("" + font.getCharacter());
        for (int i = 1; i < font.getRows(); i += 1) {
            chars.add("\u0000");
        }
        json.setChars(chars);
        return json;
    }

    public static <E extends Enum<E> & Font> List<FontProviderJson> toList(Class<E> fontClass, Map<PackPath, PackPath> pathMap, boolean all) {
        List<FontProviderJson> list = new ArrayList<>();
        Map<String, FontProviderJson> filenameMap = new HashMap<>();
        for (Font it : fontClass.getEnumConstants()) {
            if (!all && !it.isEssential()) continue;
            if ((int) it.getCharacter() < 0xE000) {
                System.err.println(it + ": Character out of range: 0x" + Integer.toHexString((int) it.getCharacter()));
            }
            FontProviderJson json = filenameMap.get(it.getFilename());
            if (json != null && it.getHeight() > 0) {
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

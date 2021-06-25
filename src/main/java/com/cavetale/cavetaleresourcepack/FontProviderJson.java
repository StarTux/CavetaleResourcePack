package com.cavetale.cavetaleresourcepack;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public final class FontProviderJson implements Comparable<FontProviderJson> {
    String type;
    String file;
    int ascent;
    int height;
    List<String> chars;

    @Override
    public int compareTo(FontProviderJson other) {
        return chars.get(0).compareTo(other.chars.get(0));
    }
}

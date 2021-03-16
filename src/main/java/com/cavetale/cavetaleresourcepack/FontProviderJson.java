package com.cavetale.cavetaleresourcepack;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public final class FontProviderJson {
    String type;
    String file;
    int ascent;
    int height;
    List<String> chars;
}

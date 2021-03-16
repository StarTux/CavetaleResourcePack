package com.cavetale.cavetaleresourcepack;

import java.util.List;
import lombok.Data;

@Data
public final class FontJson {
    List<FontProviderJson> providers;

    public static FontJson ofList(List<FontProviderJson> list) {
        FontJson json = new FontJson();
        json.setProviders(list);
        return json;
    }
}

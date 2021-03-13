package com.cavetale.cavetaleresourcepack;

import com.cavetale.mytems.Mytems;
import java.nio.file.Path;
import lombok.Data;

@Data
public final class ItemInfo {
    protected String name;
    protected Mytems mytems;
    protected Path modelPath;
    protected Path texturePath;
    protected Path mcMetaPath;
    protected String modelFileName;
    protected String textureFileName;

    ItemInfo(final Path file) {
        String str = file.toString();
        int index = str.indexOf(".");
        if (index >= 0) str = str.substring(0, index);
        this.name = str;
        try {
            this.mytems = Mytems.valueOf(str.toUpperCase());
        } catch (IllegalArgumentException iae) {
        }
    }
}

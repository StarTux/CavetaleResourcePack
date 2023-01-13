package com.cavetale.cavetaleresourcepack;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Data;

// example "mytems:item/boss_chest_front"
@Data @AllArgsConstructor
public final class PackPath {
    protected String namespace; // e.g. minecraft, mytems
    protected String folder; // e.g. item, block OR models, textures
    protected String filename; // e.g. player_head, dr_acula_staff

    public static PackPath fromString(String str) {
        String namespace;
        if (str.contains(":")) {
            String[] toks = str.split(":");
            if (toks.length != 2) throw new IllegalArgumentException("More than one colon: " + str);
            namespace = toks[0];
            str = toks[1];
        } else {
            namespace = "minecraft";
        }
        String[] comps = str.split("\\/");
        if (comps.length < 2) {
            throw new IllegalArgumentException("No slash: " + str);
        }
        String folder = String.join("/", Arrays.copyOfRange(comps, 0, comps.length - 1));
        String filename = comps[comps.length - 1];
        return new PackPath(namespace, folder, filename);
    }

    public static PackPath fromPath(Path path) {
        String str = path.toString();
        if (!path.startsWith("assets")) throw new IllegalArgumentException(str);
        int nameCount = path.getNameCount();
        String namespace = path.getName(1).toString();
        String[] folders = new String[nameCount - 4];
        for (int i = 3; i < nameCount - 1; i += 1) {
            folders[i - 3] = path.getName(i).toString();
        }
        String folder = String.join("/", folders);
        String filename = path.getName(nameCount - 1).toString();
        if (filename.contains(".")) {
            int index = filename.indexOf(".");
            filename = filename.substring(0, index);
        }
        return new PackPath(namespace, folder, filename);
    }

    public static PackPath of(String namespace, String folder, String filename) {
        return new PackPath(namespace, folder, filename);
    }

    public static PackPath minecraft(String folder, String filename) {
        return new PackPath("minecraft", folder, filename);
    }

    public static PackPath mytems(String folder, String filename) {
        return new PackPath("mytems", folder, filename);
    }

    public static PackPath minecraftItem(String filename) {
        return minecraft("item", filename);
    }

    public static PackPath mytemsItem(String filename) {
        return mytems("item", filename);
    }

    public static PackPath cavetaleFont(String filename) {
        return new PackPath("cavetale", "font", filename);
    }

    @Override
    public String toString() {
        return namespace + ":" + folder + "/" + filename;
    }

    public Path toPath(String context, String suffix) {
        return Paths.get("assets/" + namespace + "/" + context + "/" + folder + "/" + filename + suffix);
    }

    public PackPath withName(String newName) {
        return new PackPath(namespace, folder, newName);
    }
}

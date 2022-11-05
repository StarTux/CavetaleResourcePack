package com.cavetale.cavetaleresourcepack;

import com.cavetale.core.font.DefaultFont;
import com.cavetale.core.font.VanillaEffects;
import com.cavetale.core.font.VanillaItems;
import com.cavetale.core.font.VanillaPaintings;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.MytemsCategory;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import org.bukkit.Material;

public final class Main {
    private static Random random = new Random();
    private static Set<String> usedNames = new HashSet<>();
    static boolean doObfuscate = false;
    static boolean verbose = false;
    static boolean doMakeVanillaItems = false;
    static Map<PackPath, PackPath> texturePathMap = new HashMap<>();
    static Map<PackPath, PackPath> modelPathMap = new HashMap<>();
    static Map<Material, List<ModelOverride>> materialOverridesMap = new HashMap<>();
    static Map<PackPath, BufferedImage> textureImageMap = new HashMap<>();
    static int nextRandomFile;
    static Path vanillaPath = null;
    static final Path SOURCE = Path.of("src/resourcepack");

    private Main() { }

    public static void main(String[] args) throws IOException {
        if (run(args)) return;
        String usage = ""
            + "USAGE"
            + "\n java CavetaleResourcePack OPTIONS VANILLAPATH"
            + "\n OPTIONS"
            + "\n -o --obfuscate  Obfuscate the output"
            + "\n -v --verbose    Verbose output"
            + "\n --vanillaitems  Make vanilla items";
        System.out.println(usage);
    }

    static boolean run(String[] args) throws IOException {
        for (String arg : args) {
            if (arg.startsWith("--")) {
                switch (arg.substring(2)) {
                case "obfuscate":
                    doObfuscate = true;
                    break;
                case "verbose":
                    verbose = true;
                    break;
                case "vanillaitems":
                    doMakeVanillaItems = true;
                    break;
                default:
                    return false;
                }
            } else if (arg.startsWith("-")) {
                for (int i = 1; i < arg.length(); i += 1) {
                    char c = arg.charAt(i);
                    switch (c) {
                    case 'o':
                        doObfuscate = true;
                        break;
                    case 'v':
                        verbose = true;
                        break;
                    default:
                        return false;
                    }
                }
            } else {
                if (vanillaPath == null) {
                    vanillaPath = Paths.get(arg);
                } else {
                    return false;
                }
            }
        }
        if (doMakeVanillaItems) {
            makeVanillaItemsFont();
        } else {
            makeResourcePack();
        }
        return true;
    }

    static void makeResourcePack() throws IOException {
        Path dest = Paths.get("target/resourcepack");
        Files.createDirectories(dest);
        // Copy required files
        copyJson(SOURCE, dest, "pack.mcmeta");
        copyPng(SOURCE, dest, "pack.png");
        // Copy (and obfuscate) textures
        makeTextureFiles(SOURCE, dest, Paths.get("assets/mytems/textures/item"));
        makeTextureFiles(SOURCE, dest, Paths.get("assets/cavetale/textures/font"));
        copyExistingModelFiles(SOURCE, dest, Paths.get("assets/mytems/models/item"));
        buildMytemModels(dest);
        buildDefaultFont(dest);
        // Pack it up
        Path zipPath = Paths.get("target/Cavetale.zip");
        zip(zipPath, dest);
        sha1sum(zipPath, Paths.get("target/Cavetale.zip.sha1"));
    }

    static void buildMytemModels(Path dest) throws IOException {
        for (Mytems mytems : Mytems.values()) {
            if (mytems.category == MytemsCategory.POCKET_MOB) {
                PackPath packPath = PackPath.mytemsItem("pocket_mob");
                if (doObfuscate) packPath = modelPathMap.get(packPath);
                materialOverridesMap.computeIfAbsent(mytems.material, m -> new ArrayList<>())
                    .add(new ModelOverride(mytems.customModelData, packPath));
                continue;
            }
            if (mytems.customModelData == null) continue;
            if (mytems.material == null) continue;
            Path modelSource;
            String modelPath = "assets/mytems/models/item/" + mytems.id + ".json";
            modelSource = SOURCE.resolve(modelPath);
            if (Files.isRegularFile(modelSource)) continue; // Already done in copyExistingModelFiles()
            // Generate a model json file
            ModelJson modelJson = new ModelJson();
            if (mytems == Mytems.UNICORN_HORN) {
                modelJson.parent = PackPath.of("minecraft", "block", "end_rod").toString();
            } else if (mytems.material == Material.SHIELD) {
                PackPath path = PackPath.of("mytems", "item", "template_shield");
                modelJson.parent = modelPathMap.getOrDefault(path, path).toString();
            } else if (isHandheld(mytems)) {
                modelJson.parent = new PackPath("minecraft", "item", "handheld").toString();
            } else {
                modelJson.parent = new PackPath("minecraft", "item", "generated").toString();
            }
            // Put in the (maybe obfuscated) texture file paths
            PackPath texturePath = texturePathMap.get(PackPath.mytemsItem(mytems.id + "_animated"));
            if (texturePath == null) texturePath = texturePathMap.get(PackPath.mytemsItem(mytems.id));
            if (texturePath == null) throw new NullPointerException("null: " + PackPath.mytemsItem(mytems.id));
            if (mytems.material == Material.END_ROD) {
                modelJson.setTexture("end_rod", texturePath.toString());
            } else {
                modelJson.setTexture("layer0", texturePath.toString());
                if (mytems.material.name().startsWith("LEATHER_")) {
                    modelJson.setTexture("layer1", texturePath.toString());
                }
            }
            PackPath modelPackPath = PackPath.mytemsItem(mytems.id);
            if (doObfuscate) {
                PackPath obfuscated = PackPath.mytemsItem(randomFileName());
                if (verbose) {
                    System.err.println("buildMytemModels " + modelPackPath + " => " + obfuscated);
                }
                modelPathMap.put(modelPackPath, obfuscated);
                modelPackPath = obfuscated;
            } else {
                modelPathMap.put(modelPackPath, modelPackPath);
            }
            Path modelDest = dest.resolve(modelPackPath.toPath("models", ".json"));
            Files.createDirectories(modelDest.getParent());
            Json.save(modelDest.toFile(), modelJson, !doObfuscate);
            materialOverridesMap.computeIfAbsent(mytems.material, m -> new ArrayList<>())
                .add(new ModelOverride(mytems.customModelData, modelPackPath));
            if (mytems.material == Material.BOW) {
                // Bow pulling special case
                System.out.println("Bow: " + mytems);
                double[] pulls = {0.0, 0.65, 0.9};
                for (int i = 0; i < pulls.length; i += 1) {
                    PackPath bowPullingTexturePath = PackPath.mytemsItem(mytems.id + "_pulling_" + i);
                    if (!texturePathMap.containsKey(bowPullingTexturePath)) break; // texture must exist!
                    PackPath bowPullingModelPath = PackPath.mytemsItem(mytems.id + "_pulling_" + i);
                    if (doObfuscate) bowPullingTexturePath = texturePathMap.getOrDefault(bowPullingTexturePath, bowPullingTexturePath);
                    boolean modelExists = modelPathMap.containsKey(bowPullingModelPath);
                    if (doObfuscate) bowPullingModelPath = modelPathMap.getOrDefault(bowPullingModelPath, bowPullingModelPath);
                    if (!modelExists) { // make model if necessary
                        if (doObfuscate) {
                            PackPath tmp = PackPath.mytemsItem(randomFileName());
                            modelPathMap.put(bowPullingModelPath, tmp);
                            bowPullingModelPath = tmp;
                        }
                        ModelJson bowPullingModelJson = new ModelJson();
                        bowPullingModelJson.parent = "item/generated";
                        bowPullingModelJson.setTexture("layer0", bowPullingTexturePath.toString());
                        Path bowPullingModelDest = dest.resolve(bowPullingModelPath.toPath("models", ".json"));
                        Files.createDirectories(bowPullingModelDest.getParent());
                        Json.save(bowPullingModelDest.toFile(), bowPullingModelJson, !doObfuscate);
                    }
                    ModelOverride bowPullingOverride = new ModelOverride(mytems.customModelData, bowPullingModelPath);
                    bowPullingOverride.setBowPulling(1.0);
                    if (i != 0) bowPullingOverride.setBowPull(pulls[i]);
                    materialOverridesMap.get(mytems.material).add(bowPullingOverride);
                }
            } else if (mytems.material == Material.SHIELD) {
                // Shield blocking
                System.out.println("Shield: " + mytems);
                PackPath shieldBlockingModelPath = PackPath.mytemsItem(mytems.id + "_blocking");
                boolean modelExists = modelPathMap.containsKey(shieldBlockingModelPath);
                if (doObfuscate) shieldBlockingModelPath = modelPathMap.getOrDefault(shieldBlockingModelPath, shieldBlockingModelPath);
                if (!modelExists) { // make model if necessary
                    if (doObfuscate) {
                        PackPath tmp = PackPath.mytemsItem(randomFileName());
                        modelPathMap.put(shieldBlockingModelPath, tmp);
                        shieldBlockingModelPath = tmp;
                    }
                    ModelJson shieldBlockingModelJson = new ModelJson();
                    PackPath templateShieldBlockingPath = PackPath.mytemsItem("template_shield_blocking");
                    shieldBlockingModelJson.parent = modelPathMap.getOrDefault(templateShieldBlockingPath, templateShieldBlockingPath).toString();
                    shieldBlockingModelJson.setTexture("layer0", texturePath.toString());
                    Path shieldBlockingModelDest = dest.resolve(shieldBlockingModelPath.toPath("models", ".json"));
                    Files.createDirectories(shieldBlockingModelDest.getParent());
                    Json.save(shieldBlockingModelDest.toFile(), shieldBlockingModelJson, !doObfuscate);
                }
                ModelOverride shieldBlockingOverride = new ModelOverride(mytems.customModelData, shieldBlockingModelPath);
                shieldBlockingOverride.setBlocking(1.0);
                materialOverridesMap.get(mytems.material).add(shieldBlockingOverride);
            }
        }
        for (Map.Entry<Material, List<ModelOverride>> entry : materialOverridesMap.entrySet()) {
            Material material = entry.getKey();
            String modelPath = "assets/minecraft/models/item/" + material.getKey().getKey() + ".json";
            Path modelDest = dest.resolve(modelPath);
            Path modelSource = SOURCE.resolve(modelPath); // pre-written vanilla model
            if (Files.isRegularFile(modelSource)) {
                ModelJson minecraftModel = Json.load(modelSource.toFile(), ModelJson.class, ModelJson::new);
                for (ModelJson.OverrideJson override : minecraftModel.overrides) {
                    PackPath overrideModelPath = PackPath.fromString(override.model);
                    PackPath obfuscated = modelPathMap.get(overrideModelPath);
                    if (obfuscated != null) {
                        override.model = obfuscated.toString();
                    }
                }
                Files.createDirectories(modelDest.getParent());
                Json.save(modelDest.toFile(), minecraftModel, !doObfuscate);
            } else {
                modelSource = vanillaPath.resolve(modelPath);
                if (!Files.isRegularFile(modelSource)) {
                    System.err.println("File not found: " + modelSource);
                }
                ModelJson minecraftModel = Json.load(modelSource.toFile(), ModelJson.class, ModelJson::new);
                List<ModelOverride> overrides = entry.getValue();
                Collections.sort(overrides);
                minecraftModel.addOverrides(overrides);
                Files.createDirectories(modelDest.getParent());
                Json.save(modelDest.toFile(), minecraftModel, !doObfuscate);
            }
        }
    }

    static void buildDefaultFont(Path dest) throws IOException {
        Path fontDest = dest.resolve("assets/cavetale/font");
        Files.createDirectories(fontDest);
        List<FontProviderJson> fontProviderList = new ArrayList<>();
        fontProviderList.addAll(Fonts.toList(DefaultFont.class, texturePathMap));
        fontProviderList.addAll(Fonts.toList(VanillaEffects.class, texturePathMap));
        fontProviderList.addAll(Fonts.toList(VanillaPaintings.class, texturePathMap));
        fontProviderList.addAll(Fonts.toList(VanillaItems.class, texturePathMap));
        for (Mytems mytems : Mytems.values()) {
            if (mytems.character > 0) {
                PackPath clearPackPath;
                PackPath packPath;
                switch (mytems) {
                default:
                    clearPackPath = PackPath.mytemsItem(mytems.id);
                    packPath = doObfuscate
                        ? texturePathMap.get(clearPackPath)
                        : clearPackPath;
                }
                if (packPath == null) throw new NullPointerException(mytems + ": packPath=null");
                FontProviderJson it;
                BufferedImage image = textureImageMap.get(clearPackPath);
                if (image.getWidth() == image.getHeight()) {
                    it = new FontProviderJson("bitmap", packPath.toString() + ".png", 8, 8, List.of(mytems.character + ""));
                } else if (mytems.animation.length > 1) {
                    int ratio = image.getHeight() / image.getWidth();
                    if (mytems.animation.length != ratio) {
                        throw new IllegalStateException(mytems + ": " + mytems.animation.length + " != " + ratio);
                    }
                    int w = image.getWidth() / 2;
                    System.out.println("buildDefaultFont animation " + clearPackPath + ": " + ratio + ":1, " + w);
                    List<String> list = new ArrayList<>(ratio);
                    for (char chr : mytems.animation) {
                        list.add("" + chr);
                    }
                    it = new FontProviderJson("bitmap", packPath.toString() + ".png", w, w, list);
                } else {
                    int ratio = image.getHeight() / image.getWidth();
                    int w = image.getWidth() / 2;
                    System.out.println("buildDefaultFont " + clearPackPath + ": " + ratio + ":1, " + w);
                    List<String> list = new ArrayList<>(ratio);
                    int glyphIndex = mytems.category == MytemsCategory.COIN ? 2 : 0;
                    for (int i = 0; i < ratio; i += 1) {
                        list.add(i == glyphIndex
                                 ? mytems.character + ""
                                 : "\u0000");
                    }
                    it = new FontProviderJson("bitmap", packPath.toString() + ".png", w, w, list);
                }
                fontProviderList.add(it);
            }
        }
        Collections.sort(fontProviderList);
        Json.save(fontDest.resolve("default.json").toFile(), FontJson.ofList(fontProviderList), !doObfuscate);
    }

    /**
     * Find all texture files under the given directory and copy them
     * to their appropriate destination path. Put the obfuscated name
     * in the texturePathMap if required.
     * @param source root of the global source path
     * @param dest root the global dest path
     * @param the relative path
     */
    static void makeTextureFiles(Path source, Path dest, Path relative) throws IOException {
        Map<Path, String> pathMap = new HashMap<>();
        Files.walkFileTree(source.resolve(relative), new FileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path path, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                    try {
                        Path relative = source.relativize(path);
                        pathMap.put(relative, relative.getFileName().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path path, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        List<Path> paths = new ArrayList<>(pathMap.keySet());
        Collections.sort(paths, (a, b) -> (pathMap.get(a).compareTo(pathMap.get(b))));
        for (Path path : paths) {
            Path local = source.resolve(path);
            Path file = local.getFileName();
            if (file.toString().endsWith(".png")) {
                Path relative2 = relative.resolve(file);
                PackPath packPath = PackPath.fromPath(relative2);
                PackPath packPathValue = doObfuscate ? packPath.withName(randomFileName()) : packPath;
                if (verbose) {
                    System.err.println("makeTextureFiles " + packPath + " => " + packPathValue);
                }
                texturePathMap.put(packPath, packPathValue);
                try {
                    BufferedImage image = copyPng(local, dest.resolve(packPathValue.toPath("textures", ".png")));
                    textureImageMap.put(packPath, image);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                Path localMcMeta = local.getParent().resolve(file.toString() + ".mcmeta");
                if (Files.isRegularFile(localMcMeta)) {
                    try {
                        copyJson(localMcMeta, dest.resolve(packPathValue.toPath("textures", ".png.mcmeta")));
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Find all existing mytems model files and copy them. Obfuscate if desired.
     * This will flatten the target model namespace.
     */
    static void copyExistingModelFiles(Path source, Path dest, Path relative) throws IOException {
        Map<Path, String> pathMap = new HashMap<>();
        Files.walkFileTree(source.resolve(relative), new FileVisitor<Path>() {
                @Override public FileVisitResult postVisitDirectory(Path path, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes exc) {
                    return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                    try {
                        Path relative = source.relativize(path);
                        pathMap.put(relative, relative.getFileName().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFileFailed(Path path, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        List<Path> paths = new ArrayList<>(pathMap.keySet());
        Collections.sort(paths, (a, b) -> (pathMap.get(a).compareTo(pathMap.get(b))));
        Map<Path, ModelJson> pathModelMap = new HashMap<>();
        for (Path path : paths) {
            ModelJson modelJson = Json.load(source.resolve(path).toFile(), ModelJson.class);
            pathModelMap.put(path, modelJson);
            PackPath modelPackPath = PackPath.fromPath(path);
            if (doObfuscate) {
                PackPath obfuscated = modelPackPath.withName(randomFileName());
                if (verbose) {
                    System.out.println("copyExistingModelFiles " + modelPackPath + " => " + obfuscated);
                }
                modelPathMap.put(modelPackPath, obfuscated);
            }
            for (String key : modelJson.getTextureKeys()) {
                String value = modelJson.getTexture(key);
                if (value.startsWith("#")) continue;
                PackPath texturePackPath = PackPath.fromString(value);
                PackPath texturePackPathValue = texturePathMap.get(texturePackPath);
                if (texturePackPathValue != null) {
                    modelJson.setTexture(key, texturePackPathValue.toString());
                }
            }
        }
        // 2nd pass. This time we can fix parent relationships!
        Files.createDirectories(dest.resolve("assets/mytems/models/item"));
        for (Path path : paths) {
            ModelJson modelJson = pathModelMap.get(path);
            if (modelJson.parent != null) {
                PackPath parentPath = PackPath.fromString(modelJson.parent);
                if (Objects.equals("mytems", parentPath.namespace)) {
                    PackPath obfuscated = modelPathMap.get(parentPath);
                    if (obfuscated != null) modelJson.parent = obfuscated.toString();
                }
            }
            PackPath modelPackPath = PackPath.fromPath(path);
            if (doObfuscate) modelPackPath = modelPathMap.get(modelPackPath);
            Path destPath = dest.resolve(modelPackPath.toPath("models", ".json"));
            Json.save(destPath.toFile(), modelJson, !doObfuscate);
            String name = pathMap.get(path);
            name = name.substring(0, name.length() - 5); // strip .json
            Mytems mytems = Mytems.forId(name);
            if (verbose) {
                System.out.println("copyExistingModelFiles2 " + path + " : " + mytems);
            }
            if (mytems != null && mytems.material != null) {
                materialOverridesMap.computeIfAbsent(mytems.material, m -> new ArrayList<>())
                    .add(new ModelOverride(mytems.customModelData, modelPackPath));
            }
        }
    }

    static String randomFileName() {
        return Integer.toHexString(++nextRandomFile);
    }

    static boolean isHandheld(Mytems mytems) {
        switch (mytems.category) {
        case PAINTBRUSH:
            return false;
        default: break;
        }
        switch (mytems) {
        case BLUNDERBUSS:
            return true;
        default: break;
        }
        switch (mytems.material) {
        case BAMBOO:
        case BLAZE_ROD:
        case BONE:
        case CARROT_ON_A_STICK:
        case DEBUG_STICK:
        case DIAMOND_AXE:
        case DIAMOND_HOE:
        case DIAMOND_PICKAXE:
        case DIAMOND_SHOVEL:
        case DIAMOND_SWORD:
        case FISHING_ROD:
        case GOLDEN_AXE:
        case GOLDEN_HOE:
        case GOLDEN_PICKAXE:
        case GOLDEN_SHOVEL:
        case GOLDEN_SWORD:
        case IRON_AXE:
        case IRON_HOE:
        case IRON_PICKAXE:
        case IRON_SHOVEL:
        case IRON_SWORD:
        case NETHERITE_AXE:
        case NETHERITE_HOE:
        case NETHERITE_PICKAXE:
        case NETHERITE_SHOVEL:
        case NETHERITE_SWORD:
        case STICK:
        case STONE_AXE:
        case STONE_HOE:
        case STONE_PICKAXE:
        case STONE_SHOVEL:
        case STONE_SWORD:
        case WARPED_FUNGUS_ON_A_STICK:
        case WOODEN_AXE:
        case WOODEN_HOE:
        case WOODEN_PICKAXE:
        case WOODEN_SHOVEL:
        case WOODEN_SWORD:
            return true;
        default:
            return false;
        }
    }

    static void zip(final Path zipFile, final Path sourcePath) throws IOException {
        List<Path> paths = new ArrayList<>();
        Files.walkFileTree(sourcePath, new FileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path path, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                    try {
                        Path relative = sourcePath.relativize(path);
                        paths.add(relative);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path path, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        Collections.sort(paths);
        FileTime zeroTime = FileTime.fromMillis(0L);
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            zipOutputStream.setLevel(Deflater.BEST_COMPRESSION);
            zipOutputStream.setMethod(ZipOutputStream.DEFLATED);
            for (Path path : paths) {
                ZipEntry zipEntry = new ZipEntry(path.toString());
                zipEntry.setCreationTime(zeroTime);
                zipEntry.setLastAccessTime(zeroTime);
                zipEntry.setLastModifiedTime(zeroTime);
                zipOutputStream.putNextEntry(zipEntry);
                Files.copy(sourcePath.resolve(path), zipOutputStream);
            }
        }
    }

    static void sha1sum(final Path source, final Path target) throws IOException {
        try {
            byte[] b = Files.readAllBytes(source);
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(b);
            String result = bytesToHex(hash);
            Files.write(target, (result + "  " + source.getFileName() + "\n").getBytes());
            if (verbose) {
                System.err.println("sha1sum " + result);
            }
        } catch (NoSuchAlgorithmException nsa) {
            throw new IllegalArgumentException(nsa);
        }
    }

    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    static BufferedImage copyPng(final Path source, final Path dest) throws IOException {
        Files.createDirectories(dest.getParent());
        BufferedImage image = ImageIO.read(source.toFile());
        if (!doObfuscate) {
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
            return image;
        }
        ImageIO.write(image, "png", dest.toFile());
        return image;
    }

    static BufferedImage copyPng(final Path source, final Path dest, String filename) throws IOException {
        return copyPng(source.resolve(filename), dest.resolve(filename));
    }

    static void copyJson(final Path source, final Path dest) throws IOException {
        Files.createDirectories(dest.getParent());
        if (!doObfuscate) {
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        Object o = Json.load(source.toFile(), Object.class);
        Json.save(dest.toFile(), o);
    }

    static void copyJson(final Path source, final Path dest, String filename) throws IOException {
        copyJson(source.resolve(filename), dest.resolve(filename));
    }

    /**
     * Run this occasionally to amend the vanilla items list.
     */
    static void makeVanillaItemsFont() throws IOException {
        int min = 0xE400;
        for (VanillaItems it : VanillaItems.values()) {
            if ((int) it.getCharacter() >= min) {
                min = (int) it.getCharacter() + 1;
            }
        }
        List<Material> list = Arrays.asList(Material.values());
        Collections.sort(list, (a, b) -> a.name().compareTo(b.name()));
        for (Material material : list) {
            if (!material.isItem()) continue;
            if (material.isLegacy()) continue;
            if (material.isAir()) continue;
            VanillaItems vanillaItems = VanillaItems.of(material);
            if (vanillaItems != null) {
                PackPath packPath = PackPath.fromString(vanillaItems.getFilename());
                Path path = vanillaPath.resolve(packPath.toPath("textures", ".png"));
                if (!Files.isRegularFile(path)) {
                    System.err.println("//" + vanillaItems + " seems wrong: " + vanillaItems.getFilename() + " / " + packPath.toString());
                }
            }
            Path path;
            String key = material.getKey().getKey();
            switch (material) {
            case TNT: path = vanillaPath.resolve("assets/minecraft/textures/item/tnt_side.png"); break;
            case CLOCK: path = vanillaPath.resolve("assets/minecraft/textures/item/clock_00.png"); break;
            case COMPASS: path = vanillaPath.resolve("assets/minecraft/textures/item/compass_16.png"); break;
            case RECOVERY_COMPASS: path = vanillaPath.resolve("assets/minecraft/textures/item/recovery_compass_16.png"); break;
            case SMALL_DRIPLEAF: path = vanillaPath.resolve("assets/minecraft/textures/block/small_dripleaf_stem_bottom.png"); break;
            case BIG_DRIPLEAF: path = vanillaPath.resolve("assets/minecraft/textures/block/big_dripleaf_top.png"); break;
            case SMOOTH_RED_SANDSTONE: path = vanillaPath.resolve("assets/minecraft/textures/block/red_sandstone_top.png"); break;
            case SMOOTH_SANDSTONE: path = vanillaPath.resolve("assets/minecraft/textures/block/sandstone_top.png"); break;
            case SMOOTH_QUARTZ: path = vanillaPath.resolve("assets/minecraft/textures/block/quartz_block_bottom.png"); break;
            case STICKY_PISTON: path = vanillaPath.resolve("assets/minecraft/textures/block/piston_top_sticky.png"); break;
            case LECTERN: path = vanillaPath.resolve("assets/minecraft/textures/block/lectern_top.png"); break;
            default:
                path = vanillaPath.resolve("assets/minecraft/textures/item/" + key + ".png");
            }
            if (!Files.isRegularFile(path)) path = null;
            if (path != null) {
                BufferedImage image = ImageIO.read(path.toFile());
                if (image.getWidth() != 16 || image.getHeight() != 16) {
                    path = null;
                }
            }
            if (path == null) {
                path = vanillaPath.resolve("assets/minecraft/textures/block/" + key + ".png");
            }
            if (!Files.isRegularFile(path)) path = vanillaPath.resolve("assets/minecraft/textures/block/" + key + "_front.png");
            if (!Files.isRegularFile(path)) path = vanillaPath.resolve("assets/minecraft/textures/block/" + key + "_side.png");
            if (!Files.isRegularFile(path)) path = vanillaPath.resolve("assets/minecraft/textures/block/" + key + "_side0.png");
            if (!Files.isRegularFile(path)) path = vanillaPath.resolve("assets/minecraft/textures/block/" + key + "_side1.png");
            if (!Files.isRegularFile(path)) path = vanillaPath.resolve("assets/minecraft/textures/block/" + key + "_top.png");
            if (!Files.isRegularFile(path)) path = vanillaPath.resolve("assets/minecraft/textures/item/" + key + "_base.png");
            if (!Files.isRegularFile(path) && key.endsWith("_block")) {
                String sub = key.substring(0, key.length() - 6);
                if (!Files.isRegularFile(path)) path = vanillaPath.resolve("assets/minecraft/textures/block/" + sub + ".png");
                if (!Files.isRegularFile(path)) path = vanillaPath.resolve("assets/minecraft/textures/block/" + sub + "_top.png");
            }
            if (!Files.isRegularFile(path)) path = null;
            if (path == null) {
                if (vanillaItems == null) {
                    System.err.println("// Nothing found: " + material);
                }
                continue;
            }
            BufferedImage image = ImageIO.read(path.toFile());
            if (image.getWidth() != 16) {
                System.err.println("// Spurious size: " + material + ": " + image.getWidth());
            }
            int scale = image.getHeight() / image.getWidth();
            PackPath packPath = PackPath.fromPath(vanillaPath.relativize(path));
            if (vanillaItems != null) {
                if (!vanillaItems.filename.equals(packPath.toString())) {
                    System.err.println("// " + vanillaItems + " Changed: " + vanillaItems.getFilename() + " => " + packPath.toString());
                }
                continue;
            }
            int character = min++;
            System.out.println(material + "(Material." + material
                               + ", \"" + packPath.toString()
                               + "\", 8, 8, " + scale + ", '\\u" + Integer.toHexString(character).toUpperCase() + "'),");
        }
    }
}

package com.cavetale.cavetaleresourcepack;

import com.cavetale.cavetaleresourcepack.itemmodel.*;
import com.cavetale.core.font.DefaultFont;
import com.cavetale.core.font.VanillaEffects;
import com.cavetale.core.font.VanillaItems;
import com.cavetale.core.font.VanillaPaintings;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.MytemsCategory;
import com.cavetale.mytems.MytemsTag;
import com.cavetale.mytems.farming.GrowthStage;
import com.cavetale.mytems.item.pocketmob.PocketMobType;
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
    static Map<PackPath, BufferedImage> textureImageMap = new HashMap<>();
    static int nextRandomFile;
    static Path vanillaPath = null;
    static final Path SOURCE = Path.of("src/resourcepack");
    static final Path DEST = Paths.get("target/resourcepack");
    static final Path MYTEMS_ITEMS = DEST.resolve("assets/mytems/items");

    private Main() { }

    public static void main(String[] args) throws IOException {
        if (run(args)) return;
        String usage = ""
            + "USAGE"
            + "\n java CavetaleResourcePack OPTIONS VANILLAPATH"
            + "\n OPTIONS"
            + "\n -o --obfuscate  Obfuscate the output"
            + "\n -v --verbose    Verbose output"
            + "\n --vanillaitems  Make vanilla items"
            + "\n --makegif PNG SCALE\tMake a gif";
        System.out.println(usage);
    }

    static boolean run(String[] args) throws IOException {
        for (int i = 0; i < args.length; i += 1) {
            String arg = args[i];
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
                case "makegif":
                    if (i != 0 || args.length != 3) return false;
                    GifMaker.makeGif(args[i + 1], Integer.parseInt(args[i + 2]));
                    return true;
                default:
                    return false;
                }
            } else if (arg.startsWith("-")) {
                for (int j = 1; j < arg.length(); j += 1) {
                    char c = arg.charAt(j);
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
        Files.createDirectories(MYTEMS_ITEMS);
        // Copy required files
        copyJson(SOURCE, dest, "pack.mcmeta");
        copyPng(SOURCE, dest, "pack.png");
        for (String lang : LANG) {
            copyJson(SOURCE.resolve("assets/minecraft/lang/en_us.json"),
                     dest.resolve("assets/minecraft/lang/" + lang + ".json"));
        }
        // Copy (and obfuscate) textures
        makeTextureFiles(SOURCE, dest, Paths.get("assets/mytems/textures/item"));
        makeTextureFiles(SOURCE, dest, Paths.get("assets/cavetale/textures/font"));
        copyExistingModelFiles(SOURCE, dest, Paths.get("assets/mytems/models/item"));
        buildMytemModels(dest);
        buildDefaultFont(dest, dest.resolve("assets/cavetale/font"), true);
        buildDefaultFont(dest, dest.resolve("assets/minecraft/font"), false);
        buildMytemsAnimations();
        buildFarming();
        // Pack it up
        final Path zipPath = Paths.get("target/Cavetale.zip");
        final Path sha1Path = Paths.get("target/Cavetale.zip.sha1");
        zip(zipPath, dest);
        String sha1 = sha1sum(zipPath, sha1Path);
        Files.copy(zipPath, Paths.get("target/" + sha1 + ".zip"));
        Files.copy(sha1Path, Paths.get("target/" + sha1 + ".zip.sha1"));
    }

    /**
     * Build all the items models and the corresponding items model
     * files, new in 1.21.4.
     *
     * See https://minecraft.wiki/w/Items_model_definition
     * See https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-4
     */
    static void buildMytemModels(Path dest) throws IOException {
        for (Mytems mytems : Mytems.values()) {
            final Path itemsModelPath = MYTEMS_ITEMS.resolve(mytems.id + ".json");
            final ItemsModel itemsModel = new ItemsModel();
            if (mytems.category == MytemsCategory.POCKET_MOB) {
                PackPath packPath = PackPath.mytemsItem("pocket_mob");
                if (doObfuscate) packPath = modelPathMap.get(packPath);
                final ItemModelModel itemModel = itemsModel.makeModel(packPath.toString());
                final PocketMobType type = PocketMobType.of(mytems);
                itemModel.addDyeTint(type.layer0 | 0xff000000);
                itemModel.addDyeTint(type.layer1 | 0xff000000);
                Json.save(itemsModelPath.toFile(), itemsModel, !doObfuscate);
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
            // Decide the parent
            if (mytems == Mytems.UNICORN_HORN) {
                modelJson.parent = PackPath.of("minecraft", "block", "end_rod").toString();
            } else if (mytems.category == MytemsCategory.GOLF_CLUB) {
                modelJson.parent = new PackPath("mytems", "item", "golf_club").toString();
            } else if (mytems.material == Material.SHIELD) {
                PackPath path = PackPath.of("mytems", "item", "template_shield");
                modelJson.parent = modelPathMap.getOrDefault(path, path).toString();
            } else if (mytems.category == MytemsCategory.COUNTRY_FLAG
                       || mytems.category == MytemsCategory.PRIDE_FLAGS
                       || mytems.category == MytemsCategory.FUN_FLAGS) {
                PackPath path = PackPath.of("mytems", "item", "template_flag");
                modelJson.parent = modelPathMap.getOrDefault(path, path).toString();
            } else if (mytems == Mytems.MOM || mytems == Mytems.DAD) {
                PackPath path = PackPath.of("mytems", "item", "template_tattoo");
                modelJson.parent = modelPathMap.getOrDefault(path, path).toString();
            } else if (mytems.category == MytemsCategory.CAVEBOY) {
                modelJson.parent = new PackPath("minecraft", "item", "generated").toString();
            } else if (isHandheld(mytems)) {
                modelJson.parent = new PackPath("minecraft", "item", "handheld").toString();
            } else if (mytems.material == Material.MACE) {
                modelJson.parent = new PackPath("minecraft", "item", "handheld_mace").toString();
            } else {
                modelJson.parent = new PackPath("minecraft", "item", "generated").toString();
            }
            // Put in the (maybe obfuscated) texture file paths
            PackPath texturePath;
            if (mytems.category == MytemsCategory.MUSICAL_NOTE) {
                texturePath = texturePathMap.get(PackPath.mytemsItem("musical_note"));
            } else {
                texturePath = texturePathMap.get(PackPath.mytemsItem(mytems.id + "_item"));
            }
            if (texturePath == null) texturePath = texturePathMap.get(PackPath.mytemsItem(mytems.id));
            if (texturePath == null) throw new NullPointerException("null: " + PackPath.mytemsItem(mytems.id));
            // Figure out layered textures
            if (mytems.material == Material.END_ROD) {
                modelJson.setTexture("end_rod", texturePath.toString());
            } else {
                modelJson.setTexture("layer0", texturePath.toString());
                if (mytems.category == MytemsCategory.MUSICAL_NOTE) {
                    PackPath overlayPath = texturePathMap.get(PackPath.mytemsItem(mytems.id));
                    if (overlayPath == null) throw new NullPointerException("MUSICAL_NOTE overlay is null: " + PackPath.mytemsItem(mytems.id));
                    modelJson.setTexture("layer1", overlayPath.toString());
                } else if (mytems.material.name().startsWith("LEATHER_")) {
                    // Leather armor has the uncolored overlay on the 1 layer
                    PackPath overlayPath = texturePathMap.get(PackPath.mytemsItem(mytems.id + "_overlay"));
                    if (overlayPath != null) {
                        modelJson.setTexture("layer1", overlayPath.toString());
                    }
                } else if (mytems.material == Material.POTION) {
                    // Potions have the colored overlay on the 0 layer.
                    PackPath overlayPath = texturePathMap.get(PackPath.mytemsItem(mytems.id + "_overlay"));
                    if (overlayPath != null) {
                        modelJson.setTexture("layer0", overlayPath.toString());
                    }
                    modelJson.setTexture("layer1", texturePath.toString());
                }
            }
            PackPath modelPackPath = PackPath.mytemsItem(mytems.id);
            if (doObfuscate) {
                PackPath obfuscated = PackPath.mytemsItem(randomFileName());
                if (verbose) {
                    System.out.println("buildMytemModels " + modelPackPath + " => " + obfuscated);
                }
                modelPathMap.put(modelPackPath, obfuscated);
                modelPackPath = obfuscated;
            } else {
                modelPathMap.put(modelPackPath, modelPackPath);
            }
            Path modelDest = dest.resolve(modelPackPath.toPath("models", ".json"));
            Files.createDirectories(modelDest.getParent());
            Json.save(modelDest.toFile(), modelJson, !doObfuscate);
            if (mytems.material == Material.BOW) {
                // Bow pulling special case
                System.out.println("Bow: " + mytems);
                final ItemModelRangeDispatchUseDuration useDuration = itemsModel.makeUseDuration();
                // 100 is a magic number
                // We simply probe a few pulling numbers.  This is not
                // elegant but it should work.
                for (int i = 0; i < 100; i += 1) {
                    PackPath bowPullingTexturePath = PackPath.mytemsItem(mytems.id + "_pulling_" + i);
                    if (!texturePathMap.containsKey(bowPullingTexturePath)) {
                        continue;
                    }
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
                    useDuration.addModelEntry((float) i, bowPullingModelPath.toString());
                }
                useDuration.setFallback(new ItemModelModel(modelPackPath.toString()));
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
                final ItemModelCondition condition = itemsModel.makeCondition();
                condition.setUsingItem();
                condition.setOnFalse(new ItemModelModel(modelPackPath.toString()));
                condition.setOnTrue(new ItemModelModel(shieldBlockingModelPath.toString()));
            } else if (mytems.material == Material.COMPASS) {
                // Compass in all directions
                System.out.println("Compass: " + mytems);
                final ItemModelRangeDispatchCompass compass = itemsModel.makeCompass();
                compass.setScale(360f);
                compass.setFallback(new ItemModelModel(modelPackPath.toString()));
                compass.setLodestone();
                int compassCount = 0;
                int j = 0;
                for (int i = 0; i < 360; i += 1) {
                    String istring = String.format("%03d", i);
                    PackPath compassTexturePath = PackPath.mytemsItem(mytems.id + "_" + istring);
                    if (!texturePathMap.containsKey(compassTexturePath)) continue;
                    PackPath compassModelPath = PackPath.mytemsItem(mytems.id + "_" + istring);
                    if (doObfuscate) compassTexturePath = texturePathMap.getOrDefault(compassTexturePath, compassTexturePath);
                    boolean modelExists = modelPathMap.containsKey(compassModelPath);
                    if (doObfuscate) compassModelPath = modelPathMap.getOrDefault(compassModelPath, compassModelPath);
                    if (!modelExists) { // make model if necessary
                        if (doObfuscate) {
                            PackPath tmp = PackPath.mytemsItem(randomFileName());
                            modelPathMap.put(compassModelPath, tmp);
                            compassModelPath = tmp;
                        }
                        ModelJson compassModelJson = new ModelJson();
                        compassModelJson.parent = "item/generated";
                        compassModelJson.setTexture("layer0", compassTexturePath.toString());
                        Path compassModelDest = dest.resolve(compassModelPath.toPath("models", ".json"));
                        Files.createDirectories(compassModelDest.getParent());
                        Json.save(compassModelDest.toFile(), compassModelJson, !doObfuscate);
                    }
                    compass.addModelEntry(((float) i + (float) j) * 0.5f,
                                          compassModelPath.toString());
                    j = i;
                    compassCount += 1;
                }
            } else {
                final ItemModelModel model = itemsModel.makeModel(modelPackPath.toString());
                if (mytems == Mytems.POTION_FLASK) {
                    model.addPotionTint(0xff000000);
                    model.addConstantTint(0xffffffff);
                } else if (mytems.category == MytemsCategory.UI_COLOR) {
                    model.addDyeTint(0xffffffff);
                } else if (mytems.category == MytemsCategory.MUSICAL_NOTE) {
                    model.addDyeTint(0xffffffff);
                    model.addConstantTint(0xffffffff);
                } else if (mytems.category == MytemsCategory.GEM_SLOT) {
                    model.addDyeTint(0xffffffff);
                }
            }
            Json.save(itemsModelPath.toFile(), itemsModel, !doObfuscate);
        }
    }

    static void buildMytemsAnimations() {
        for (Mytems mytems : Mytems.values()) {
            if (mytems.animation == null) continue;
            AnimationJson.Container json = AnimationJson.ofMytemsAnimation(mytems.animation);
            PackPath packPath = PackPath.mytems("item", mytems.id);
            if (doObfuscate) packPath = texturePathMap.getOrDefault(packPath, packPath);
            Path path = DEST.resolve(packPath.toPath("textures", ".png.mcmeta"));
            if (Files.isRegularFile(path)) {
                System.err.println("Animation Exists: " + mytems + ", " + path);
                AnimationJson.Container orig = Json.load(path.toFile(), AnimationJson.Container.class);
                orig.animation.normalize();
                if (!orig.equals(json)) {
                    System.err.println(Json.serialize(orig));
                    System.err.println(Json.serialize(json));
                }
            }
            Json.save(path.toFile(), json, !doObfuscate);
            System.out.println("Animated " + mytems + ": " + path);
        }
    }

    static void buildDefaultFont(Path dest, Path fontDest, boolean all) throws IOException {
        Files.createDirectories(fontDest);
        List<FontProviderJson> fontProviderList = new ArrayList<>();
        fontProviderList.addAll(Fonts.toList(DefaultFont.class, texturePathMap, all));
        fontProviderList.addAll(Fonts.toList(VanillaEffects.class, texturePathMap, all));
        fontProviderList.addAll(Fonts.toList(VanillaPaintings.class, texturePathMap, all));
        fontProviderList.addAll(Fonts.toList(VanillaItems.class, texturePathMap, all));
        for (Mytems mytems : Mytems.values()) {
            if (!all && !mytems.isEssential()) continue;
            if (mytems.character == 0) continue;
            PackPath clearPackPath;
            clearPackPath = PackPath.mytemsItem(mytems.id + "_font");
            if (!texturePathMap.containsKey(clearPackPath)) clearPackPath = PackPath.mytemsItem(mytems.id);
            final PackPath packPath = doObfuscate
                ? texturePathMap.get(clearPackPath)
                : clearPackPath;
            if (packPath == null) throw new NullPointerException(mytems + ": packPath=null");
            FontProviderJson it;
            BufferedImage image = textureImageMap.get(clearPackPath);
            if (image == null) throw new NullPointerException("buildMytemsDefaultFont " + mytems + " " + clearPackPath);
            if (image.getWidth() >= image.getHeight()) {
                if ((int) mytems.character < 0xE000) {
                    System.err.println(mytems + ": Character out of range: 0x" + Integer.toHexString((int) mytems.character));
                }
                it = new FontProviderJson("bitmap", packPath.toString() + ".png", 8, 8, List.of(mytems.character + ""));
            } else if (mytems.characters.length > 1) {
                int ratio = image.getHeight() / image.getWidth();
                if (mytems.characters.length != ratio) {
                    throw new IllegalStateException(mytems + ": " + mytems.characters.length + " != " + ratio);
                }
                int w = image.getWidth() / 2;
                System.out.println("buildDefaultFont animation " + clearPackPath + ": " + ratio + ":1, " + w);
                List<String> list = new ArrayList<>(ratio);
                for (char chr : mytems.characters) {
                    if ((int) chr < 0xE000) {
                        System.err.println(mytems + ": Character out of range: 0x" + Integer.toHexString((int) chr));
                    }
                    list.add("" + chr);
                }
                it = new FontProviderJson("bitmap", packPath.toString() + ".png", w, w, list);
            } else {
                if ((int) mytems.character < 0xE000) {
                    System.err.println(mytems + ": Character out of range: 0x" + Integer.toHexString((int) mytems.character));
                }
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
                    System.out.println("makeTextureFiles " + packPath + " => " + packPathValue);
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
            if (mytems != null) {
                final Path itemsModelPath = MYTEMS_ITEMS.resolve(mytems.id + ".json");
                final ItemsModel itemsModel = new ItemsModel();
                final ItemModelModel itemModel = itemsModel.makeModel(modelPackPath.toString());
                if (MytemsTag.FURNITURE.isTagged(mytems)) {
                    itemModel.addDyeTint(0xff_ff_ff_ff);
                }
                Json.save(itemsModelPath.toFile(), itemsModel, !doObfuscate);
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

    private static String sha1sum(final Path source, final Path target) throws IOException {
        try {
            byte[] b = Files.readAllBytes(source);
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(b);
            String result = bytesToHex(hash);
            Files.write(target, (result + "  " + source.getFileName() + "\n").getBytes());
            if (verbose) {
                System.err.println("sha1sum " + result);
            }
            return result;
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
        BufferedImage image;
        try {
            image = ImageIO.read(source.toFile());
        } catch (IOException ioe) {
            System.err.println(source);
            throw new RuntimeException(ioe);
        }
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
     * Copy a JSON file containing a font, translating all file
     * references to their obfuscated counterparts.
     */
    static void copyFontJson(final Path source, final Path dest, String filename) throws IOException {
        if (!doObfuscate) {
            copyJson(source, dest, filename);
        } else {
            FontJson json = Json.load(source.resolve(filename).toFile(), FontJson.class);
            for (FontProviderJson provider : json.providers) {
                String sourceFileName = provider.file;
                sourceFileName = sourceFileName.substring(0, sourceFileName.length() - 4); // strip .png
                final PackPath sourceFile = PackPath.fromString(sourceFileName);
                final PackPath destFile = texturePathMap.get(sourceFile);
                if (destFile == null) {
                    throw new IllegalStateException("Unknown PackPath: " + sourceFile);
                }
                provider.file = destFile.toString() + ".png";
            }
            Path destPath = dest.resolve(filename);
            Files.createDirectories(destPath.getParent());
            Json.save(destPath.toFile(), json);
        }
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
            if (material.name().startsWith("LEGACY_")) continue;
            if (material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR) continue;
            VanillaItems vanillaItems = VanillaItems.of(material);
            Path path = null;
            if (vanillaItems != null) {
                PackPath packPath = PackPath.fromString(vanillaItems.getFilename());
                path = vanillaPath.resolve(packPath.toPath("textures", ".png"));
                if (!Files.isRegularFile(path)) {
                    System.err.println("//" + vanillaItems + " seems wrong: " + vanillaItems.getFilename() + " / " + packPath.toString());
                    path = null;
                }
            }
            String key = material.getKey().getKey();
            if (path == null) {
                path = switch (material) {
                case TNT -> vanillaPath.resolve("assets/minecraft/textures/item/tnt_side.png");
                case CLOCK -> vanillaPath.resolve("assets/minecraft/textures/item/clock_00.png");
                case COMPASS -> vanillaPath.resolve("assets/minecraft/textures/item/compass_16.png");
                case RECOVERY_COMPASS -> vanillaPath.resolve("assets/minecraft/textures/item/recovery_compass_16.png");
                case SMALL_DRIPLEAF -> vanillaPath.resolve("assets/minecraft/textures/block/small_dripleaf_stem_bottom.png");
                case BIG_DRIPLEAF -> vanillaPath.resolve("assets/minecraft/textures/block/big_dripleaf_top.png");
                case SMOOTH_RED_SANDSTONE -> vanillaPath.resolve("assets/minecraft/textures/block/red_sandstone_top.png");
                case SMOOTH_SANDSTONE -> vanillaPath.resolve("assets/minecraft/textures/block/sandstone_top.png");
                case SMOOTH_QUARTZ -> vanillaPath.resolve("assets/minecraft/textures/block/quartz_block_bottom.png");
                case STICKY_PISTON -> vanillaPath.resolve("assets/minecraft/textures/block/piston_top_sticky.png");
                case LECTERN -> vanillaPath.resolve("assets/minecraft/textures/block/lectern_top.png");
                case SUSPICIOUS_SAND -> vanillaPath.resolve("assets/minecraft/textures/block/suspicious_sand_3.png");
                case SUSPICIOUS_GRAVEL -> vanillaPath.resolve("assets/minecraft/textures/block/suspicious_gravel_3.png");
                case TRIAL_SPAWNER -> vanillaPath.resolve("assets/minecraft/textures/block/trial_spawner_side_inactive.png");
                case DECORATED_POT -> vanillaPath.resolve("assets/minecraft/textures/entity/decorated_pot/decorated_pot_side.png");
                default -> vanillaPath.resolve("assets/minecraft/textures/item/" + key + ".png");
                };
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
            int character;
            if (vanillaItems != null) {
                if (!vanillaItems.filename.equals(packPath.toString())) {
                    System.err.println("// " + vanillaItems + " Changed: " + vanillaItems.getFilename() + " => " + packPath.toString());
                }
                character = (int) vanillaItems.character;
            } else {
                character = min++;
            }
            System.out.println("    " + material + "(Material." + material
                               + ", \"" + packPath.toString()
                               + "\", 8, 8, " + scale + ", '\\u" + Integer.toHexString(character).toUpperCase() + "'),");
        }
    }

    private static void buildFarming() throws IOException {
        for (GrowthStage growthStage : GrowthStage.values()) {
            final String key = growthStage.name().toLowerCase();
            final Path imgSrc = Path.of("src/farming/" + growthStage.getFarmingPlantType().name().toLowerCase() + "/" + key + ".png");
            final PackPath packPath = PackPath.mytemsItem(key);
            final PackPath packPathValue = doObfuscate ? packPath.withName(randomFileName()) : packPath;
            texturePathMap.put(packPath, packPathValue);
            final Path imgDst = DEST.resolve(packPathValue.toPath("textures", ".png"));
            copyPng(imgSrc, imgDst);
            // Model
            final ModelJson modelJson = new ModelJson();
            modelJson.parent = switch (growthStage.getModel()) {
            case X -> "minecraft:block/cross";
            case XTALL -> {
                PackPath path = PackPath.mytemsItem("billboard_x_tall");
                if (doObfuscate) path = modelPathMap.getOrDefault(path, path);
                yield path.toString();
            }
            case X3TALL -> {
                PackPath path = PackPath.mytemsItem("billboard_x_3tall");
                if (doObfuscate) path = modelPathMap.getOrDefault(path, path);
                yield path.toString();
            }
            default -> "minecraft:block/cross";
            };
            modelJson.setTexture("cross", packPathValue.toString());
            final Path modelDst = DEST.resolve(packPathValue.toPath("models", ".json"));
            Files.createDirectories(modelDst.getParent());
            Json.save(modelDst.toFile(), modelJson, !doObfuscate);
            // Items Model
            final Path itemsModelPath = MYTEMS_ITEMS.resolve(key + ".json");
            final ItemsModel itemsModel = new ItemsModel();
            final ItemModelModel itemModel = itemsModel.makeModel(packPathValue.toString());
            Json.save(itemsModelPath.toFile(), itemsModel, !doObfuscate);
        }
    }

    private static final String[] LANG = {
        "af_za", "ar_sa", "ast_es", "az_az", "ba_ru", "bar", "be_by",
        "bg_bg", "br_fr", "brb", "bs_ba", "ca_es", "cs_cz", "cy_gb",
        "da_dk", "de_at", "de_ch", "de_de", "el_gr", "en_au", "en_ca",
        "en_gb", "en_nz", "en_pt", "en_ud", "en_us", "enp", "enws",
        "eo_uy", "es_ar", "es_cl", "es_ec", "es_es", "es_mx", "es_uy",
        "es_ve", "esan", "et_ee", "eu_es", "fa_ir", "fi_fi", "fil_ph",
        "fo_fo", "fr_ca", "fr_fr", "fra_de", "fur_it", "fy_nl",
        "ga_ie", "gd_gb", "gl_es", "haw_us", "he_il", "hi_in",
        "hr_hr", "hu_hu", "hy_am", "id_id", "ig_ng", "io_en", "is_is",
        "isv", "it_it", "ja_jp", "jbo_en", "ka_ge", "kk_kz", "kn_in",
        "ko_kr", "ksh", "kw_gb", "la_la", "lb_lu", "li_li", "lmo",
        "lol_us", "lt_lt", "lv_lv", "lzh", "mk_mk", "mn_mn", "ms_my",
        "mt_mt", "nds_de", "nl_be", "nl_nl", "nn_no", "no_no‌",
        "oc_fr", "ovd", "pl_pl", "pt_br", "pt_pt", "qya_aa", "ro_ro",
        "rpr", "ru_ru", "se_no", "sk_sk", "sl_si", "so_so", "sq_al",
        "sr_sp", "sv_se", "sxu", "szl", "ta_in", "th_th", "tl_ph",
        "tlh_aa", "tok", "tr_tr", "tt_ru", "uk_ua", "val_es",
        "vec_it", "vi_vn", "yi_de", "yo_ng", "zh_cn", "zh_hk",
        "zh_tw", "zlm_arab",
    };
}

package com.cavetale.cavetaleresourcepack;

import com.cavetale.mytems.Mytems;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    static Map<PackPath, PackPath> texturePathMap = new HashMap<>();

    private Main() { }

    public static void main(String[] args) throws IOException {
        if (run(args)) return;
        String usage = ""
            + "USAGE"
            + "\n java CavetaleResourcePack OPTIONS SOURCEPATH VANILLAPATH"
            + "\n OPTIONS"
            + "\n -o --obfuscate  obfuscate the output"
            + "\n -v --verbose    verbose output";
        System.out.println(usage);
    }

    static boolean run(String[] args) throws IOException {
        Path sourcePath = null;
        Path vanillaPath = null;
        for (String arg : args) {
            if (arg.startsWith("--")) {
                switch (arg.substring(2)) {
                case "obfuscate":
                    doObfuscate = true;
                    break;
                case "verbose":
                    verbose = true;
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
                if (sourcePath == null) {
                    sourcePath = Paths.get(arg);
                } else if (vanillaPath == null) {
                    vanillaPath = Paths.get(arg);
                } else {
                    return false;
                }
            }
        }
        makeResourcePack(sourcePath, vanillaPath);
        return true;
    }

    static void makeResourcePack(final Path source, final Path vanilla) throws IOException {
        Path dest = Paths.get("target/resourcepack");
        Files.createDirectories(dest);
        // Copy required files
        copyJson(source, dest, "pack.mcmeta");
        copyPng(source, dest, "pack.png");
        // Copy (and obfuscate) textures
        Files.list(source.resolve("assets/mytems/textures/item")).forEach(local -> makeTextureFile(source, dest, local));
        Files.list(source.resolve("assets/cavetale/textures/font")).forEach(local -> makeTextureFile(source, dest, local));
        // Build the mytems models
        Map<Material, List<ModelOverride>> materialOverridesMap = new HashMap<>();
        for (Mytems mytems : Mytems.values()) {
            if (mytems.customModelData == null) continue;
            if (mytems.material == null) continue;
            String modelPath = "assets/mytems/models/item/" + mytems.id + ".json";
            Path modelSource = source.resolve(modelPath);
            ModelJson modelJson;
            if (Files.isRegularFile(modelSource)) {
                modelJson = Json.load(modelSource.toFile(), ModelJson.class, ModelJson::new);
                for (String key : modelJson.getTextureKeys()) {
                    String value = modelJson.getTexture(key);
                    PackPath packPath = PackPath.fromString(value);
                    PackPath packPathValue = texturePathMap.get(packPath);
                    if (packPathValue != null) {
                        modelJson.setTexture(key, packPathValue.toString());
                    }
                }
            } else {
                // Generate a model json file
                modelJson = new ModelJson();
                if (mytems == Mytems.UNICORN_HORN) {
                    modelJson.parent = PackPath.of("minecraft", "block", "end_rod").toString();
                } else if (isHandheld(mytems.material)) {
                    modelJson.parent = new PackPath("minecraft", "item", "handheld").toString();
                } else {
                    modelJson.parent = new PackPath("minecraft", "item", "generated").toString();
                }
                // Put in the (maybe obfuscated) texture file paths
                PackPath texturePath = texturePathMap.get(PackPath.mytemsItem(mytems.id));
                if (mytems.material == Material.END_ROD) {
                    modelJson.setTexture("end_rod", texturePath.toString());
                } else {
                    modelJson.setTexture("layer0", texturePath.toString());
                    if (mytems.material.name().startsWith("LEATHER_")) {
                        modelJson.setTexture("layer1", texturePath.toString());
                    }
                }
            }
            PackPath modelPackPath = doObfuscate ? PackPath.mytemsItem(randomFileName()) : PackPath.mytemsItem(mytems.id);
            Path modelDest = dest.resolve(modelPackPath.toPath("models", ".json"));
            Files.createDirectories(modelDest.getParent());
            Json.save(modelDest.toFile(), modelJson, !doObfuscate);
            materialOverridesMap.computeIfAbsent(mytems.material, m -> new ArrayList<>())
                .add(new ModelOverride(mytems.customModelData, modelPackPath));
        }
        for (Map.Entry<Material, List<ModelOverride>> entry : materialOverridesMap.entrySet()) {
            Material material = entry.getKey();
            List<ModelOverride> overrides = entry.getValue();
            String modelPath = "assets/minecraft/models/item/" + material.name().toLowerCase() + ".json";
            Path modelSource = vanilla.resolve(modelPath);
            if (!Files.isRegularFile(modelSource)) {
                System.err.println("File not found: " + modelSource);
            }
            ModelJson minecraftModel = Json.load(modelSource.toFile(), ModelJson.class, ModelJson::new);
            Collections.sort(overrides);
            minecraftModel.addOverrides(overrides);
            Path modelDest = dest.resolve(modelPath);
            Files.createDirectories(modelDest.getParent());
            Json.save(modelDest.toFile(), minecraftModel, !doObfuscate);
        }
        // Build the default font
        Path fontDest = dest.resolve("assets/cavetale/font");
        Files.createDirectories(fontDest);
        Json.save(fontDest.resolve("default.json").toFile(), FontJson.ofList(DefaultFont.toList(texturePathMap)), !doObfuscate);
        // Pack it up
        Path zipPath = Paths.get("target/Cavetale.zip");
        zip(zipPath, dest);
        sha1sum(zipPath, Paths.get("target/Cavetale.zip.sha1"));
    }

    static void makeTextureFile(Path source, Path dest, Path local) {
        Path file = local.getFileName();
        if (file.toString().endsWith(".png")) {
            Path relative = source.relativize(local);
            PackPath packPath = PackPath.fromPath(relative);
            PackPath packPathValue = doObfuscate ? packPath.withName(randomFileName()) : packPath;
            texturePathMap.put(packPath, packPathValue);
            try {
                copyPng(local, dest.resolve(packPathValue.toPath("textures", ".png")));
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

    static String randomFileName() {
        String pool = "0123456789abcdefghijklmnopqrstuvwxyz";
        String result;
        do {
            result = "";
            for (int i = 0; i < 5; i += 1) {
                result = result + pool.charAt(random.nextInt(pool.length()));
            }
        } while (usedNames.contains(result));
        usedNames.add(result);
        return result;
    }

    static boolean isHandheld(Material mat) {
        switch (mat) {
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
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            zipOutputStream.setLevel(Deflater.BEST_COMPRESSION);
            zipOutputStream.setMethod(ZipOutputStream.DEFLATED);
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
                            ZipEntry zipEntry = new ZipEntry(relative.toString());
                            zipOutputStream.putNextEntry(zipEntry);
                            Files.copy(path, zipOutputStream);
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
        }
    }

    static void sha1sum(final Path source, final Path target) throws IOException {
        try {
            byte[] b = Files.readAllBytes(source);
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(b);
            String result = bytesToHex(hash);
            Files.write(target, (result + "  " + source.getFileName() + "\n").getBytes());
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

    static void copyPng(final Path source, final Path dest) throws IOException {
        Files.createDirectories(dest.getParent());
        if (!doObfuscate) {
            Files.copy(source, dest);
            return;
        }
        BufferedImage image = ImageIO.read(source.toFile());
        ImageIO.write(image, "png", dest.toFile());
    }

    static void copyPng(final Path source, final Path dest, String filename) throws IOException {
        copyPng(source.resolve(filename), dest.resolve(filename));
    }

    static void copyJson(final Path source, final Path dest) throws IOException {
        Files.createDirectories(dest.getParent());
        if (!doObfuscate) {
            Files.copy(source, dest);
            return;
        }
        Object o = Json.load(source.toFile(), Object.class);
        Json.save(dest.toFile(), o);
    }

    static void copyJson(final Path source, final Path dest, String filename) throws IOException {
        copyJson(source.resolve(filename), dest.resolve(filename));
    }
}

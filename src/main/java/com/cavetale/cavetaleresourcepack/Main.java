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
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
    static Map<PackPath, PackPath> texturePathMap = new HashMap<>();
    static Map<PackPath, PackPath> modelPathMap = new HashMap<>();
    static int nextRandomFile;

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
        makeTextureFiles(source, dest, Paths.get("assets/mytems/textures/item"));
        makeTextureFiles(source, dest, Paths.get("assets/cavetale/textures/font"));
        // Build the mytems models
        Map<Material, List<ModelOverride>> materialOverridesMap = new HashMap<>();
        List<PackPath> extraItemModels = new ArrayList<>(); // Store source path of parents found in mytems item model files
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
                if (modelJson.parent != null) {
                    PackPath parentPath = PackPath.fromString(modelJson.parent);
                    if (Objects.equals("mytems", parentPath.namespace)) {
                        if (extraItemModels.contains(parentPath)) {
                            if (doObfuscate) {
                                PackPath obfuscated = modelPathMap.get(parentPath);
                                modelJson.parent = obfuscated.toString();
                            }
                        } else {
                            extraItemModels.add(parentPath);
                            if (doObfuscate) {
                                PackPath obfuscated = parentPath.withName(randomFileName());
                                if (verbose) {
                                    System.err.println(parentPath + " => " + obfuscated);
                                }
                                modelPathMap.put(parentPath, obfuscated);
                                modelJson.parent = obfuscated.toString();
                            }
                        }
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
            PackPath modelPackPath = PackPath.mytemsItem(mytems.id);
            if (doObfuscate) {
                PackPath obfuscated = PackPath.mytemsItem(randomFileName());
                if (verbose) {
                    System.err.println(modelPackPath + " => " + obfuscated);
                }
                modelPathMap.put(modelPackPath, obfuscated);
                modelPackPath = obfuscated;
            }
            Path modelDest = dest.resolve(modelPackPath.toPath("models", ".json"));
            Files.createDirectories(modelDest.getParent());
            Json.save(modelDest.toFile(), modelJson, !doObfuscate);
            materialOverridesMap.computeIfAbsent(mytems.material, m -> new ArrayList<>())
                .add(new ModelOverride(mytems.customModelData, modelPackPath));
        }
        // Build extra item models. They were referenced in the actual
        // models' parent field. Their names have already been
        // obfuscated. If the extra models likewise reference a
        // parent, they will be added to the list and processed later.
        for (int i = 0; i < extraItemModels.size(); i += 1) {
            PackPath packPath = extraItemModels.get(i);
            Path path = packPath.toPath("models", ".json");
            ModelJson modelJson = Json.load(source.resolve(path).toFile(), ModelJson.class, ModelJson::new);
            if (modelJson.parent != null) {
                PackPath parentPath = PackPath.fromString(modelJson.parent);
                if (Objects.equals("mytems", parentPath.namespace)) {
                    if (extraItemModels.contains(parentPath)) {
                        if (doObfuscate) {
                            PackPath obfuscated = modelPathMap.get(parentPath);
                            modelJson.parent = obfuscated.toString();
                        }
                    } else {
                        extraItemModels.add(parentPath);
                        if (doObfuscate) {
                            PackPath obfuscated = parentPath.withName(randomFileName());
                            if (verbose) {
                                System.err.println(parentPath + " => " + obfuscated);
                            }
                            modelPathMap.put(parentPath, obfuscated);
                            modelJson.parent = obfuscated.toString();
                        }
                    }
                }
            }
            if (doObfuscate) {
                packPath = modelPathMap.get(packPath);
                path = packPath.toPath("models", ".json");
            }
            Json.save(dest.resolve(path).toFile(), modelJson, !doObfuscate);
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
                    System.err.println(packPath + " => " + packPathValue);
                }
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
    }

    static String randomFileName() {
        return Integer.toHexString(++nextRandomFile);
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
                System.err.println(result);
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

    static void copyPng(final Path source, final Path dest) throws IOException {
        Files.createDirectories(dest.getParent());
        if (!doObfuscate) {
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
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
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        Object o = Json.load(source.toFile(), Object.class);
        Json.save(dest.toFile(), o);
    }

    static void copyJson(final Path source, final Path dest, String filename) throws IOException {
        copyJson(source.resolve(filename), dest.resolve(filename));
    }
}

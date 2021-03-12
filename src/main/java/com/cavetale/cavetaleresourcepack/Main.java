package com.cavetale.cavetaleresourcepack;

import com.cavetale.mytems.util.Json;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.bukkit.Material;

public final class Main {
    private static Random random = new Random();
    private static Set<String> usedNames = new HashSet<>();

    private Main() { }

    public static void main(String[] args) throws IOException {
        if (run(args)) return;
        String usage = ""
            + "USAGE"
            + "\n java CavetaleResourcePack OPTIONS SOURCEPATH VANILLAPATH"
            + "\n OPTIONS"
            + "\n -o obfuscate the output";
        System.out.println(usage);
    }

    static boolean run(String[] args) throws IOException {
        boolean obfuscate = false;
        Path sourcePath = null;
        Path vanillaPath = null;
        for (String arg : args) {
            if (arg.startsWith("-")) {
                switch (arg) {
                case "-o":
                    obfuscate = true;
                    break;
                default:
                    return false;
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
        makeResourcePack(sourcePath, vanillaPath, obfuscate);
        return true;
    }

    static void makeResourcePack(final Path sourcePath, final Path vanillaPath, final boolean obfuscate) throws IOException {
        Path targetPath = Paths.get("target/resourcepack");
        System.out.println(sourcePath);
        System.out.println(vanillaPath);
        System.out.println(targetPath);
        Files.createDirectories(targetPath);
        Map<String, ItemInfo> mytemsMap = new HashMap<>();
        Map<String, MinecraftModel> minecraftItemMap = new HashMap<>();
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
                        if (relative.getNameCount() == 1) {
                            Files.copy(path, targetPath.resolve(relative), StandardCopyOption.REPLACE_EXISTING);
                        } else if (relative.getNameCount() > 1) {
                            Path directory = relative.getParent();
                            Path file = relative.getFileName();
                            if (directory.equals(Paths.get("assets/mytems/textures/item"))) {
                                ItemInfo itemInfo = new ItemInfo(file);
                                mytemsMap.computeIfAbsent(itemInfo.name, n -> itemInfo).texturePath = relative;
                            } else if (directory.equals(Paths.get("assets/mytems/models/item"))) {
                                ItemInfo itemInfo = new ItemInfo(file);
                                mytemsMap.computeIfAbsent(itemInfo.name, n -> itemInfo).modelPath = relative;
                            }
                        }
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
        Path targetModelsPath = targetPath.resolve(Paths.get("assets/mytems/models/item"));
        Files.createDirectories(targetModelsPath);
        Path targetTexturesPath = targetPath.resolve(Paths.get("assets/mytems/textures/item"));
        Files.createDirectories(targetTexturesPath);
        for (ItemInfo itemInfo : mytemsMap.values()) {
            if (itemInfo.mytems == null) {
                System.err.println("Mytem not found: " + itemInfo.name);
                continue;
            }
            if (itemInfo.mytems.material == null) {
                System.err.println("Mytems.material is null: " + itemInfo.mytems);
                continue;
            }
            if (itemInfo.mytems.customModelData == null) {
                System.err.println("Mytems.customModelData is null: " + itemInfo.mytems);
                continue;
            }
            if (itemInfo.texturePath == null) {
                System.err.println("Missing texture: " + itemInfo.name);
                continue;
            }
            if (obfuscate) {
                itemInfo.modelFileName = randomFileName();
                itemInfo.textureFileName = randomFileName();
            } else {
                itemInfo.modelFileName = itemInfo.name;
                itemInfo.textureFileName = itemInfo.name;
            }
            if (itemInfo.modelPath == null) {
                Map<String, Object> modelFileObject = new HashMap<>();
                if (isHandheld(itemInfo.mytems.material)) {
                    modelFileObject.put("parent", "minecraft:item/handheld");
                } else {
                    modelFileObject.put("parent", "minecraft:item/generated");
                }
                Map<String, Object> texturesMap = new HashMap<>();
                modelFileObject.put("textures", texturesMap);
                texturesMap.put("layer0", "mytems:item/" + itemInfo.textureFileName);
                String modelJson = Json.serialize(modelFileObject);
                Files.write(targetModelsPath.resolve(itemInfo.modelFileName + ".json"), modelJson.getBytes());
            } else if (obfuscate) {
                Map<String, Object> modelFileObject = (Map<String, Object>) Json.load(sourcePath.resolve(itemInfo.modelPath).toFile(), Map.class, () -> null);
                String modelJson = Json.serialize(modelFileObject);
                modelJson = modelJson.replace(itemInfo.name, itemInfo.textureFileName);
                Files.write(targetModelsPath.resolve(itemInfo.modelFileName + ".json"), modelJson.getBytes());
            } else {
                Files.copy(sourcePath.resolve(itemInfo.modelPath), targetModelsPath.resolve(itemInfo.modelFileName + ".json"),
                           StandardCopyOption.REPLACE_EXISTING);
            }
            Files.copy(sourcePath.resolve(itemInfo.texturePath), targetTexturesPath.resolve(itemInfo.textureFileName + ".png"),
                       StandardCopyOption.REPLACE_EXISTING);
            String minecraftItemName = itemInfo.mytems.material.name().toLowerCase();
            MinecraftModel minecraftModel = minecraftItemMap.computeIfAbsent(minecraftItemName, n -> {
                    Path minecraftModelPath = vanillaPath.resolve("assets/minecraft/models/item/" + n + ".json");
                    if (!Files.isReadable(minecraftModelPath)) {
                        System.err.println("Not found: " + minecraftModelPath);
                        return null;
                    }
                    Map<String, Object> map = (Map<String, Object>) Json.load(minecraftModelPath.toFile(), Map.class, () -> null);
                    return new MinecraftModel(n, map);
                });
            minecraftModel.overrides.add(new ModelOverride(itemInfo.mytems.customModelData, itemInfo.modelFileName));
        }
        Path targetMinecraftModelsPath = targetPath.resolve("assets/minecraft/models/item");
        Files.createDirectories(targetMinecraftModelsPath);
        for (MinecraftModel minecraftModel : minecraftItemMap.values()) {
            Path target = targetMinecraftModelsPath.resolve(minecraftModel.name + ".json");
            Json.save(target.toFile(), minecraftModel.cook(), !obfuscate);
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
}
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
import java.util.Map;
import java.util.Random;

public final class Main {
    static Random random = new Random();

    private Main() { }

    public static void main(String[] args) throws Exception {
        boolean obfuscate = true;
        Path sourcePath = Paths.get(args[0]);
        Path vanillaPath = Paths.get(args[1]);
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
                System.out.println("Mytem not found: " + itemInfo.name);
                continue;
            }
            if (itemInfo.modelPath == null) {
                System.out.println("Missing model: " + itemInfo.name);
                continue;
            }
            if (itemInfo.texturePath == null) {
                System.out.println("Missing texture: " + itemInfo.name);
                continue;
            }
            if (obfuscate) {
                itemInfo.modelFileName = randomFileName();
                itemInfo.textureFileName = randomFileName();
            } else {
                itemInfo.modelFileName = itemInfo.name;
                itemInfo.textureFileName = itemInfo.name;
            }
            if (obfuscate) {
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
        String result = "";
        for (int i = 0; i < 5; i += 1) {
            result = result + pool.charAt(random.nextInt(pool.length()));
        }
        return result;
    }
}

package com.cavetale.cavetaleresourcepack;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class Find {
    private Find() { }

    public static List<Path> files(Path origin, Predicate<Path> predicate) throws IOException {
        List<Path> result = new ArrayList<>();
        Files.walkFileTree(origin, new FileVisitor<Path>() {
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
                    if (predicate.test(path)) {
                        result.add(path);
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path path, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        return result;
    }
}

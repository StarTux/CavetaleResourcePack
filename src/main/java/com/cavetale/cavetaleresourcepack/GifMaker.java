package com.cavetale.cavetaleresourcepack;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public final class GifMaker {
    public static void makeGif(String png, int scale) throws IOException {
        if (!png.endsWith(".png")) throw new IllegalStateException("Not a png: " + png);
        Path pngPath = Path.of(png);
        if (!Files.isRegularFile(pngPath)) throw new IllegalStateException("Not a file: " + pngPath);
        Path mcmetaPath = Path.of(png + ".mcmeta");
        if (!Files.isRegularFile(mcmetaPath)) throw new IllegalStateException("Not a file: " + mcmetaPath);
        AnimationJson.Container container = Json.load(mcmetaPath.toFile(), AnimationJson.Container.class);
        AnimationJson mcmeta = container.animation;
        mcmeta.normalize();
        String name = pngPath.getFileName().toString();
        name = name.substring(0, name.length() - 4);
        Path destFolder = Paths.get("target/gif/" + name);
        Files.createDirectories(destFolder);
        BufferedImage image = ImageIO.read(pngPath.toFile());
        final int width = image.getWidth();
        final int count = image.getHeight() / width;
        int fileIndex = 0;
        List<Object> frames = mcmeta.frames;
        if (frames == null) {
            frames = new ArrayList<>();
            for (int i = 0; i < count; i += 1) frames.add(i);
        }
        for (Object o : frames) {
            final int frame;
            final int ticks;
            if (o instanceof Number n) {
                frame = n.intValue();
                ticks = mcmeta.frametime;
            } else if (o instanceof AnimationJson.Frame frameo) {
                frame = frameo.index;
                ticks = frameo.time != 0
                    ? frameo.time
                    : mcmeta.frametime;
            } else {
                throw new IllegalStateException("Not a frame: " + o);
            }
            for (int i = 0; i < ticks; i += 1) {
                final int size = scale * width;
                BufferedImage output = new BufferedImage(size, size, image.getType());
                Graphics2D gfx = output.createGraphics();
                gfx.drawImage(image, 0, 0, size, size,
                              0, frame * width, width, (frame + 1) * width,
                              null);
                Path destFile = destFolder.resolve(String.format(String.format("%s-%03d.png", name, fileIndex++)));
                ImageIO.write(output, "png", destFile.toFile());
            }
        }
        System.out.println("Wrote " + count + " images to " + destFolder);
    }

    private GifMaker() { }
}

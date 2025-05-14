package xyz.lynxs.terrarium.world.gen;

import org.slf4j.Logger;
import xyz.lynxs.terrarium.Terrarium;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static xyz.lynxs.terrarium.Terrarium.CONFIG;
import static xyz.lynxs.terrarium.Terrarium.CONFIG1;
import static xyz.lynxs.terrarium.Util.pack;


public class BiomeProvider {

        private static final String CACHE_DIR = "/biomes/";
        private static final Logger LOGGER = Terrarium.LOGGER;
        private static final Map<Long, int[][]> biomeMap= new ConcurrentHashMap<>();
        private static final int QUALITY = 7;
        private static BufferedImage getClimateFromHeightmap(int xTile, int zTile, Path cachePath, URI path) {



        URI uri = path.resolve(QUALITY + "/" + xTile + "/" + zTile + ".png");
        File cacheFile = cachePath.resolve(QUALITY + "/" + xTile + "/" + zTile + ".png").toFile();

        if (cacheFile.exists()) {
            try {
                return ImageIO.read(cacheFile);

            } catch (Exception e) {
                LOGGER.error("Failed to load tile from cache: {}", e.getMessage());
            }
        }
        try {
            try (InputStream inputStream = uri.toURL().openStream()) {
                BufferedImage tileImage = ImageIO.read(inputStream);

                if (tileImage == null) {
                    throw new IOException("Failed to read image from URL: " + uri);
                }

                Path cacheDir = Paths.get(cacheFile.getParent());
                if (!Files.exists(cacheDir)) {
                    Files.createDirectories(cacheDir);
                }
                ImageIO.write(tileImage, "png", cacheFile);
                return tileImage;

            }
        } catch (IOException e) {
            LOGGER.error("Failed to download tile: {}", e.getMessage());
        }
        return new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
    }

    private static int[][] toByteArr(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] arr = new int[height][width]; // Typically better for row-major access

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                arr[y][x] = image.getRGB(x, y);
            }
        }
        return arr;
    }

    public static int getClimate(int x, int z) {
        if(biomeMap.size() > 16) biomeMap.clear();
        // Convert world coordinates to temperature data coordinates
        double scale = Math.pow(2, QUALITY - CONFIG.zoom);
        int scaledX = (int) (x * scale);
        int scaledZ = (int) (z * scale);
        int Xtile = scaledX >> 8;
        int Ztile = scaledZ >> 8;
        // Get base temperature values
        // Bilinear interpolation for smooth transitions

        return biomeMap.computeIfAbsent(pack(Xtile, Ztile), k -> toByteArr(getClimateFromHeightmap(Xtile, Ztile, Path.of(CONFIG1.CACHE_DIR + CACHE_DIR), CONFIG1.BIOME_URL)))[scaledX & 0xFF][scaledZ & 0xFF];
    }



}

package xyz.lynxs.terrarium.world.gen;


import xyz.lynxs.terrarium.Terrarium;
import org.slf4j.Logger;
import xyz.lynxs.terrarium.Util;

import javax.imageio.ImageIO;
import java.awt.*;
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


public class HeightProvider {
    private static final String ELEV_CACHE_DIR = "/elevation/";
    private static final String WATER_CACHE_DIR = "/water/";

    private static final Logger LOGGER = Terrarium.LOGGER;
    public static int size = (int) (256 * Math.pow(2, CONFIG.zoom));



    private static final Map<Long, short[][]> elevMap = new ConcurrentHashMap<>();
    private static final Map<Long, short[][]> waterMap = new ConcurrentHashMap<>();

    public static void init(){
        size = (int) (256 * Math.pow(2, CONFIG.zoom));
    }

    private static BufferedImage getElevationFromHeightmap(int xTile, int zTile, Path cachePath, URI path) {



        URI uri = path.resolve(CONFIG.zoom + "/" + xTile + "/" + zTile + ".png");
        File cacheFile = cachePath.resolve(CONFIG.zoom + "/" + xTile + "/" + zTile + ".png").toFile();

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


    private static short[][] toIntHeightmap(BufferedImage image){
        short[][] arr = new short[image.getWidth()][image.getHeight()];
        for(int i = 0; i < image.getWidth(); i++){
            for(int j = 0; j < image.getHeight(); j++){
                int rgb = image.getRGB(i, j);

                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                double elevation = (red * 256 + green + blue / 256.0) - 32768.0;

                arr[i][j] = (short) ((elevation / 8850) * CONFIG.worldHeight);

            }
        }
        return arr;
    }


    public static short getElevation(int x, int z) {
        return elevMap.computeIfAbsent(pack(x >> 8, z >> 8), k -> toIntHeightmap(getElevationFromHeightmap(x >> 8, z >> 8, Path.of(CONFIG1.CACHE_DIR + ELEV_CACHE_DIR), CONFIG1.ELEVATION_URL)))[x & 0xFF][z & 0xFF];
    }
    public static short getWaterElevation(int x, int z){
        return waterMap.computeIfAbsent(pack(x >> 8, z >> 8), k -> toIntHeightmap(getElevationFromHeightmap(x >> 8, z >> 8, Path.of(CONFIG1.CACHE_DIR + WATER_CACHE_DIR), CONFIG1.WATER_URL)))[x & 0xFF][z & 0xFF];
    }


}
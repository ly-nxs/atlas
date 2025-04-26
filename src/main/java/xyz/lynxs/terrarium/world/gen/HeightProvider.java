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
    private static final String CACHE_DIR = "/elevation/";
    private static final Logger LOGGER = Terrarium.LOGGER;
    public static int size = (int) (256 * Math.pow(2, CONFIG.zoom));
    private static final Map<Long, short[][]> elevMap = new ConcurrentHashMap<>();

    public static void init(){
        size = (int) (256 * Math.pow(2, CONFIG.zoom));
    }

    private static BufferedImage getElevationFromHeightmap(int xTile, int zTile) {


        String cachePath = CONFIG1.CACHE_DIR + CACHE_DIR + CONFIG.zoom + "/" + xTile + "/" + zTile + ".png";
        URI uri = CONFIG1.ELEVATION_URL.resolve(CONFIG.zoom + "/" + xTile + "/" + zTile + ".png");

        File cacheFile = new File(cachePath);
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

                Color rgb = new Color(image.getRGB(i, j));
                double elevation = (rgb.getRed() * 256 + rgb.getGreen() + rgb.getBlue() / 256.0) - 32768;
                arr[i][j] = (short) ((elevation / 8850) * CONFIG.worldHeight);

            }
        }
        return arr;
    }


    public static short getElevation(int x, int z) {
        if(elevMap.size() > 32) elevMap.clear();
        int xTile = x / 256;
        int zTile = z / 256;
        int xPixel = x - (xTile * 256);
        int zPixel = z - (zTile * 256);
        return elevMap.computeIfAbsent(pack(xTile, zTile), k -> toIntHeightmap(getElevationFromHeightmap(xTile, zTile)))[xPixel][zPixel];
    }

    public static short[][] getChunk(int x, int z){
        if(elevMap.size() > 32) elevMap.clear();
        x = x - (x % 16);
        z = z - (z % 16);
        int xTile = x / 256;
        int zTile = z / 256;
        int xPixel = x - (xTile * 256);
        int zPixel = z - (zTile * 256);
        return Util.getSubArraySystemCopy(elevMap.computeIfAbsent(pack(xTile, zTile), k -> toIntHeightmap(getElevationFromHeightmap(xTile, zTile))), xPixel, zPixel, xPixel + 16, zPixel + 16);
    }
}
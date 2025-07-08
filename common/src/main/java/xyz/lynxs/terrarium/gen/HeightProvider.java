package xyz.lynxs.terrarium.gen;


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


public class HeightProvider {
    private static final String ELEV_CACHE_DIR = "/elevation/";
    private static final String WATER_CACHE_DIR = "/water/";

    private static final Logger LOGGER = Terrarium.LOGGER;
    public static int size = (int) (256 * Math.pow(2, CONFIG.zoom));



    private static final Map<Long, short[][]> elevMap = new ConcurrentHashMap<>();
    private static final Map<Long, float[][]> steepMap = new ConcurrentHashMap<>();
    private static final Map<Long, short[][]> waterMap = new ConcurrentHashMap<>();
    private static final Map<Long, short[]> minMaxMap = new ConcurrentHashMap<>();

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
        for(int i = 0; i < image.getWidth(); i++) for(int j = 0; j < image.getHeight(); j++){
                int rgb = image.getRGB(i, j);

                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                double elevation = (red * 256 + green + blue / 256.0) - 32768.0;

                arr[i][j] = (short) ((elevation / 8850) * CONFIG.worldHeight);
        }
        return arr;
    }

    public static float[][] computeSteepnessMap(short[][] elevationTile) {
        int size = elevationTile.length;
        float[][] steepness = new float[size][size];

        for (int x = 1; x < size - 1; x++) for (int z = 1; z < size - 1; z++) {
                // Central differences for gradient
                float dx = (elevationTile[x+1][z] - elevationTile[x-1][z]) / 2.0f;
                float dz = (elevationTile[x][z+1] - elevationTile[x][z-1]) / 2.0f;

                // Steepness = magnitude of gradient
                steepness[x][z] = (float) Math.sqrt(dx * dx + dz * dz) / ((float) CONFIG.worldHeight / 100);
        }
        return steepness;
    }

    public static short[] computeMinMax(short[][] elevationTile){
        short max = Short.MIN_VALUE;
        short min = Short.MAX_VALUE;

        for (short[] arr : elevationTile) for (short elevation : arr) {
               max = elevation > max ? elevation : max;
               min = elevation < min ? elevation : min;
        }
        return new short[]{min, max};
    }


    public static short getElevation(int xx, int zz) {
        int x = xx + CONFIG.adjustXoffset;
        int z = zz + CONFIG.adjustZoffset;
        if(elevMap.size() > 16) elevMap.clear();
        return elevMap.computeIfAbsent(pack(x >> 8, z >> 8), k -> toIntHeightmap(getElevationFromHeightmap(x >> 8, z >> 8, Path.of(CONFIG1.CACHE_DIR + ELEV_CACHE_DIR), CONFIG1.ELEVATION_URL)))[x & 0xFF][z & 0xFF];
    }
    public static float getSteepness(int xx, int zz){
        int x = xx + CONFIG.adjustXoffset;
        int z = zz + CONFIG.adjustZoffset;
        if(steepMap.size() > 16) steepMap.clear();
        return steepMap.computeIfAbsent(pack(x >> 8, z >> 8),k -> computeSteepnessMap(elevMap.computeIfAbsent(pack(x >> 8, z >> 8), j -> toIntHeightmap(getElevationFromHeightmap(x >> 8, z >> 8, Path.of(CONFIG1.CACHE_DIR + ELEV_CACHE_DIR), CONFIG1.ELEVATION_URL)))))[x & 0xFF][z & 0xFF];
    }
    public static short[] getMinMax(int xx, int zz){
        int x = xx + CONFIG.adjustXoffset;
        int z = zz + CONFIG.adjustZoffset;
        if(minMaxMap.size() > 16) minMaxMap.clear();
        return minMaxMap.computeIfAbsent(pack(x >> 8, z >> 8), k -> computeMinMax(elevMap.computeIfAbsent(pack(x >> 8, z >> 8), j -> toIntHeightmap(getElevationFromHeightmap(x >> 8, z >> 8, Path.of(CONFIG1.CACHE_DIR + ELEV_CACHE_DIR), CONFIG1.ELEVATION_URL)))));
    }
    public static short getWaterElevation(int x, int z){
        return waterMap.computeIfAbsent(pack(x >> 8, z >> 8), k -> toIntHeightmap(getElevationFromHeightmap(x >> 8, z >> 8, Path.of(CONFIG1.CACHE_DIR + WATER_CACHE_DIR), CONFIG1.WATER_URL)))[x & 0xFF][z & 0xFF];
    }


}
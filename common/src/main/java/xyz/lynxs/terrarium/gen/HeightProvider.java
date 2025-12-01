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
import java.util.concurrent.*;

import static xyz.lynxs.terrarium.Terrarium.CONFIG;
import static xyz.lynxs.terrarium.Terrarium.CONFIG1;
import static xyz.lynxs.terrarium.Util.pack;


public class HeightProvider {
    private static final String ELEV_CACHE_DIR = "/elevation/";
    private static final String WATER_CACHE_DIR = "/water/";

    private static final Logger LOGGER = Terrarium.LOGGER;
    public static int size = (int) (256 * Math.pow(2, CONFIG.zoom));


    // Define a simple record (Java 16+) or class to hold all tile data
    public record TileData(
            short[][] elevation,
            float[][] steepness,
            short[] maxMinElevation
    ) {
        // Dummy data for error case
        public static final TileData DUMMY = new TileData(new short[256][256], new float[256][256], new short[]{0,0});
    }

    // Dedicated thread pool for heavy I/O/Computation tasks
    private static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(4);

    // Cache map changed to hold Future<TileData> to indicate work in progress
    private static final Map<Long, Future<TileData>> tileFutures = new ConcurrentHashMap<>();

    private static TileData loadAndComputeTile(int xTile, int zTile) {
        // 1. I/O: Load the image (disk or network)
        BufferedImage image = getElevationFromHeightmap(xTile, zTile, Path.of(CONFIG1.CACHE_DIR + ELEV_CACHE_DIR), CONFIG1.ELEVATION_URL);

        // 2. Computation: Convert to short[][]
        short[][] elevation = toIntHeightmap(image);

        // 3. Computation: Compute steepness
        float[][] steepness = computeSteepnessMap(elevation);

        // 4. Computation: Compute max
        short[] maxMinElevation = computeMaxMin(elevation);

        return new TileData(elevation, steepness, maxMinElevation);
    }

    /**
     * Helper method to submit the I/O task if not present, and then synchronously wait for the result.
     * This method will only block the first time a tile is requested per tile coordinate.
     */
    private static TileData getTileBlocking(int xTile, int zTile) {
        long key = pack(xTile, zTile);

        // Use computeIfAbsent to submit the task only once.
        Future<TileData> future = tileFutures.computeIfAbsent(key, k -> {
            LOGGER.debug("Submitting tile load task for ({}, {}) to background executor.", xTile, zTile);
            return IO_EXECUTOR.submit(() -> loadAndComputeTile(xTile, zTile));
        });

        try {
            // CRITICAL: .get() blocks the current thread (game thread) until the result is available.
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Thread interrupted while waiting for tile ({}, {}).", xTile, zTile);
            tileFutures.remove(key); // Remove failed future
            return TileData.DUMMY;
        } catch (ExecutionException e) {
            LOGGER.error("Failed to execute tile load task for ({}, {}): {}", xTile, zTile, e.getCause().getMessage());
            tileFutures.remove(key); // Remove failed future
            return TileData.DUMMY;
        }
    }

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

                arr[i][j] = (short) ((elevation / 8850) * (CONFIG.worldHeight - CONFIG.startingY));
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
                steepness[x][z] = (float) Math.sqrt(dx * dx + dz * dz);
        }
        return steepness;
    }

    public static short[] computeMaxMin(short[][] elevationTile){
        // Check for an empty or null array to prevent errors.
        if (elevationTile == null || elevationTile.length == 0 || elevationTile[0].length == 0) {
            return new short[]{Short.MIN_VALUE, Short.MAX_VALUE}; // Return the smallest possible short if array is invalid.
        }

        // Initialize maxElevation with the smallest possible short value.
        short maxElevation = Short.MIN_VALUE;
        short minElevation = Short.MAX_VALUE;

        // Iterate through each row of the 2D array.
        for (short[] row : elevationTile) {
            // Iterate through each short value in the current row.
            for (short value : row) {
                // Compare the current value with the stored maximum.
                if (value > maxElevation) {
                    maxElevation = value; // Update maxElevation if a larger value is found.
                }
                if(value < minElevation){
                    minElevation = value;
                }
            }
        }

        return new short[]{(short) (maxElevation + CONFIG.startingY), (short) (minElevation + CONFIG.startingY)};
    }

    public static short getElevation(int xx, int zz) {
        int x = xx + CONFIG.adjustXoffset;
        int z = zz + CONFIG.adjustZoffset;

        // Call the helper function which handles the Future<TileData> resolution
        TileData tile = getTileBlocking(x >> 8, z >> 8);

        // Access the data from the resolved TileData object
        return (short) (tile.elevation[x & 0xFF][z & 0xFF] + CONFIG.startingY);
    }

    public static float getSteepness(int xx, int zz){
        int x = xx + CONFIG.adjustXoffset;
        int z = zz + CONFIG.adjustZoffset;

        // Call the helper function which handles the Future<TileData> resolution
        TileData tile = getTileBlocking(x >> 8, z >> 8);

        // Access the data from the resolved TileData object
        return tile.steepness[x & 0xFF][z & 0xFF];
    }

    public static short[] getMaxMin(int xx, int zz){
        int x = xx + CONFIG.adjustXoffset;
        int z = zz + CONFIG.adjustZoffset;

        // Call the helper function which handles the Future<TileData> resolution
        TileData tile = getTileBlocking(x >> 8, z >> 8);

        // Access the data from the resolved TileData object
        return tile.maxMinElevation;
    }
}
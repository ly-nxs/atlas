package xyz.lynxs.terrarium.gen;

import org.jetbrains.annotations.NotNull;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

import static xyz.lynxs.terrarium.Terrarium.CONFIG;
import static xyz.lynxs.terrarium.Terrarium.CONFIG1;
import static xyz.lynxs.terrarium.Util.pack;


public class BiomeProvider {

    private static final String CACHE_DIR = "/biomes/";
    private static final Logger LOGGER = Terrarium.LOGGER;
    private static final int QUALITY = 7;

    // --- CONCURRENCY SETUP ---
    // Dedicated thread pool for heavy I/O/Computation tasks. Use a small fixed pool.
    private static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(
            4,
            // Naming the threads is useful for debugging logs
            new NamedThreadFactory("Terrarium-Biome-Loader")
    );

    // Cache map changed to hold Future<int[][]> to manage work in progress.
    private static final Map<Long, Future<int[][]>> biomeFutures = new ConcurrentHashMap<>();

    // Placeholder array for errors, so we never return null.
    private static final int[][] DUMMY_BIOME_ARRAY = new int[256][256];


    // --- EXISTING STATIC DATA ---
    private static final Map<Integer, Double> biomeHumidity = new ConcurrentHashMap<>();
    private static final Map<Integer, Double> biomeTemperature = new ConcurrentHashMap<>();

    // --- UTILITIES ---

    // Simple thread factory to name threads for better debugging
    private static class NamedThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private int count = 0;
        public NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }
        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread t = new Thread(r, namePrefix + "-" + count++);
            t.setDaemon(true);
            return t;
        }
    }


    public static void init(){
        // Initialization code for biomeHumidity and biomeTemperature maps...
        biomeHumidity.put(6072275, -0.33);
        biomeHumidity.put(11655278, 0.5);
        biomeHumidity.put(49408, 0.33);
        biomeHumidity.put(1737786, 0.3);
        biomeHumidity.put(8971063, -0.33);
        biomeHumidity.put(14732139, -0.75);
        biomeHumidity.put(14440537, 0.0);
        biomeHumidity.put(6379956, -0.25);
        biomeHumidity.put(11765807, -0.25);
        biomeHumidity.put(7767588, 0.66);
        biomeHumidity.put(11649118, -0.25);
        biomeHumidity.put(79104, 0.75);

        biomeTemperature.put(6072275, -0.66);
        biomeTemperature.put(11655278, -0.33);
        biomeTemperature.put(49408, 0.0);
        biomeTemperature.put(1737786, -0.2);
        biomeTemperature.put(8971063, 0.33);
        biomeTemperature.put(14732139, 0.9);
        biomeTemperature.put(14440537, 0.33);
        biomeTemperature.put(6379956, 0.5);
        biomeTemperature.put(11765807, 0.25);
        biomeTemperature.put(7767588, 0.0);
        biomeTemperature.put(11649118, 0.66);
        biomeTemperature.put(79104, 0.33);
    }

    private static BufferedImage getClimateTileImage(int xTile, int zTile, Path cachePath, URI path) {
        // Renamed to clarify its purpose: I/O for the image tile.
        URI uri = path.resolve(QUALITY + "/" + xTile + "/" + zTile + ".png");
        File cacheFile = cachePath.resolve(QUALITY + "/" + xTile + "/" + zTile + ".png").toFile();

        if (cacheFile.exists()) {
            try {
                return ImageIO.read(cacheFile);
            } catch (Exception e) {
                LOGGER.error("Failed to load biome tile from cache: {}", e.getMessage());
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
            LOGGER.error("Failed to download biome tile: {}", e.getMessage());
        }
        return new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
    }

    private static int[][] toByteArr(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        // Array is stored as [Z][X] (height=Z, width=X)
        int[][] arr = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Keep only the RGB part (discard alpha if present)
                arr[y][x] = image.getRGB(x, y) & 0x00FFFFFF;
            }
        }
        return arr;
    }

    /**
     * Unified heavy task: I/O (get image) + Computation (to array).
     * This runs on the IO_EXECUTOR thread.
     */
    private static int[][] loadBiomeTile(int xTile, int zTile) {
        BufferedImage image = getClimateTileImage(xTile, zTile, Path.of(CONFIG1.CACHE_DIR + CACHE_DIR), CONFIG1.BIOME_URL);
        return toByteArr(image);
    }

    /**
     * Renamed for clarity: Helper method to submit the I/O task if not present, and
     * then synchronously wait for the result using Future.get().
     */
    private static int[][] getBiomeArrayBlocking(int xTile, int zTile) {
        long key = pack(xTile, zTile);

        // Submit the task to the background thread only if it hasn't been submitted yet.
        Future<int[][]> future = biomeFutures.computeIfAbsent(key, k -> {
            LOGGER.debug("Submitting biome tile load task for ({}, {}) to background executor.", xTile, zTile);
            return IO_EXECUTOR.submit(() -> loadBiomeTile(xTile, zTile));
        });

        try {
            // CRITICAL: .get() blocks the current thread until the result is available.
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Thread interrupted while waiting for biome tile ({}, {}).", xTile, zTile);
            biomeFutures.remove(key);
            return DUMMY_BIOME_ARRAY;
        } catch (ExecutionException e) {
            LOGGER.error("Failed to execute biome tile load task for ({}, {}): {}", xTile, zTile, e.getCause().getMessage());
            biomeFutures.remove(key);
            return DUMMY_BIOME_ARRAY;
        }
    }

    /**
     * Samples the biome key (color integer) at precise scaled coordinates (x, z),
     * handling tile boundaries automatically.
     */
    private static int getBiomeKeyAtScaledCoord(int scaledX, int scaledZ) {
        // Calculate which 256x256 tile this coordinate belongs to
        int Xtile = scaledX >> 8;
        int Ztile = scaledZ >> 8;

        // Calculate the local index within that tile (0-255)
        int localX = scaledX & 0xFF;
        int localZ = scaledZ & 0xFF;

        // Fetch the correct tile data array (blocking until ready, but cached)
        int[][] biomeArray = getBiomeArrayBlocking(Xtile, Ztile);

        // Use localZ for row and localX for column ([Z][X] based on toByteArr)
        // Check array bounds safety (should always pass if DUMMY is 256x256)
        if (localZ >= 0 && localZ < biomeArray.length && localX >= 0 && localX < biomeArray[localZ].length) {
            return biomeArray[localZ][localX];
        }

        return DUMMY_BIOME_ARRAY[0][0];
    }


    public static int getClimate(int xx, int zz) {
        int x = xx + CONFIG.adjustXoffset;
        int z = zz + CONFIG.adjustZoffset;

        // 1. Calculate precise scaled coordinates (double)
        double scale = Math.pow(2, QUALITY - CONFIG.zoom);
        double preciseScaledX = x * scale;
        double preciseScaledZ = z * scale;

        // 2. Identify the top-left corner of the sampling square (P00)
        int scaledX0 = (int) Math.floor(preciseScaledX);
        int scaledZ0 = (int) Math.floor(preciseScaledZ);

        // 3. Fractional part used for weighting (u and v)
        double u = preciseScaledX - scaledX0; // weight towards x1 (right)
        double v = preciseScaledZ - scaledZ0; // weight towards z1 (bottom)

        // 4. Sample the four nearest biome keys (colors)
        // P00 (top-left)
        int c00 = getBiomeKeyAtScaledCoord(scaledX0, scaledZ0);

        // P10 (top-right)
        int c10 = getBiomeKeyAtScaledCoord(scaledX0 + 1, scaledZ0);

        // P01 (bottom-left)
        int c01 = getBiomeKeyAtScaledCoord(scaledX0, scaledZ0 + 1);

        // P11 (bottom-right)
        int c11 = getBiomeKeyAtScaledCoord(scaledX0 + 1, scaledZ0 + 1);

        // 5. Calculate weights (inverse distance squared is common, but simple bilinear weights work for smooth transition)
        double w00 = (1.0 - u) * (1.0 - v); // Weight for P00
        double w10 = (u) * (1.0 - v);       // Weight for P10
        double w01 = (1.0 - u) * (v);       // Weight for P01
        double w11 = (u) * (v);             // Weight for P11

        // 6. Select the biome key with the highest weight to preserve discrete colors/keys
        int result = c00;
        double maxWeight = w00;

        if (w10 > maxWeight) {
            maxWeight = w10;
            result = c10;
        }
        if (w01 > maxWeight) {
            maxWeight = w01;
            result = c01;
        }
        if (w11 > maxWeight) {
            // maxWeight = w11; // Not strictly necessary to track maxWeight after final check
            result = c11;
        }

        return result;
    }

    public static double getHumidity(int xx, int zz){
        int clim = getClimate(xx, zz);
        if(!biomeHumidity.containsKey(clim)) {
            LOGGER.error("No key for humidity: {}", clim);
        }
        return biomeHumidity.getOrDefault(clim, 0.0);
    }

    public static double getTemperature(int xx, int zz){
        return biomeTemperature.getOrDefault(getClimate(xx, zz), 0.0);
    }

    //TODO: actually do this
    public static short getTreeline(int xx, int zz){
        int x = xx + CONFIG.adjustXoffset;
        int z = zz + CONFIG.adjustZoffset;

        return 256;
    }
}

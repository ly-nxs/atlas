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


public class BiomeProvider {

        private static final String CACHE_DIR = "/biomes/";
        private static final Logger LOGGER = Terrarium.LOGGER;
        private static final Map<Long, int[][]> biomeMap = new ConcurrentHashMap<>();
        private static final Map<Integer, Double> biomeHumidity = new ConcurrentHashMap<>();
        private static final Map<Integer, Double> biomeTemperature = new ConcurrentHashMap<>();
        private static final int QUALITY = 7;

        public static void init(){

            biomeHumidity.put(6072275, -0.66);
            biomeHumidity.put(11655278, 0.5);
            biomeHumidity.put(49408, 0.1);
            biomeHumidity.put(1737786, 0.66);
            biomeHumidity.put(-15039430, -0.25);
            biomeHumidity.put(-2045077, -0.1);
            biomeHumidity.put(14440537, 0.0);
            biomeHumidity.put(6379956, -0.25);
            biomeHumidity.put(11765807, -0.25);
            biomeHumidity.put(7767588, 0.66);
            biomeHumidity.put(-2336679, -0.5);
            biomeHumidity.put(79104, 0.75);

            biomeTemperature.put(6072275, -0.66);
            biomeTemperature.put(11655278, -0.5);
            biomeTemperature.put(49408, 0.0);
            biomeTemperature.put(1737786, -0.1);
            biomeTemperature.put(8971063, -0.1);
            biomeTemperature.put(-15039430, 0.5);
            biomeTemperature.put(-2045077, 0.75);
            biomeTemperature.put(6379956, 0.5);
            biomeTemperature.put(11765807, 0.25);
            biomeTemperature.put(7767588, 0.0);
            biomeTemperature.put(-2336679, 0.5);
            biomeTemperature.put(79104, 0.33);
        }

        private static BufferedImage getClimateFromHeightmap(int xTile, int zTile, Path cachePath, URI path) {
        /*
        {
  "mappings": [
    {
      "tag": "terrarium:tundra",
      "color": "#5CA7D3",
      "comment": "Tundra"
    },
    {
      "tag": "terrarium:taiga",
      "color": "#B1D86E",
      "comment": "Boreal Forests/Taiga"
    },
    {
      "tag": "terrarium:temperate_broadleaf_mixed",
      "color": "#00C100",
      "comment": "Temperate Broadleaf & Mixed Forests"
    },
    {
      "tag": "terrarium:temperate_conifers",
      "color": "#1A843A",
      "comment": "Temperate Conifer Forests"
    },
    {
      "tag": "terrarium:temperate_grasslands",
      "color": "#88E337",
      "comment": "Temperate Grasslands, Savannas & Shrublands"
    },
    {
      "tag": "terrarium:deserts_shrubs",
      "color": "#E0CB6B",
      "comment": "Deserts & Xeric Shrubs"
    },
    {
      "tag": "terrarium:mediterranean",
      "color": "#DC5859",
      "comment": "Mediterranean Forests, Woodlands & Scrub"
    },
    {
      "tag": "terrarium:flooded_grasslands",
      "color": "#6159B4",
      "comment": "Flooded Grasslands & Savannas"
    },
    {
      "tag": "terrarium:tropical_grasslands",
      "color": "#B3882F",
      "comment": "Tropical & Subtropical Grasslands, Savannas & Shrublands"
    },
    {
      "tag": "terrarium:tropical_coniferous_forests",
      "color": "#768624",
      "comment": "Tropical & Subtropical Coniferous Forests"
    },
    {
      "tag": "terrarium:tropical_dry_broadleaf_forests",
      "color": "#B1C05E",
      "comment": "Tropical & Subtropical Dry Broadleaf Forests"
    },
    {
      "tag": "terrarium:tropical_moist_broadleaf_forests",
      "color": "#013500",
      "comment": "Tropical & Subtropical Moist Broadleaf Forests"
    }
  ],
  "default_biome": "minecraft:plains"
  }
         */

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

    public static int getClimate(int xx, int zz) {
        int x = xx + CONFIG.adjustXoffset;
        int z = zz + CONFIG.adjustZoffset;
        if(biomeMap.size() > 16) biomeMap.clear();
        // Convert world coordinates to temperature data coordinates
        double scale = Math.pow(2, QUALITY - CONFIG.zoom);
        int scaledX = (int) (x * scale);
        int scaledZ = (int) (z * scale);
        int Xtile = scaledX >> 8;
        int Ztile = scaledZ >> 8;
        // Get base temperature values

        return biomeMap.computeIfAbsent(pack(Xtile, Ztile), k -> toByteArr(getClimateFromHeightmap(Xtile, Ztile, Path.of(CONFIG1.CACHE_DIR + CACHE_DIR), CONFIG1.BIOME_URL)))[scaledX & 0xFF][scaledZ & 0xFF];
    }
    public static double getHumidity(int xx, int zz){
            int clim = getClimate(xx, zz);
            if(!biomeHumidity.containsKey(clim))
                LOGGER.error("No key for: {}", clim);
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

package xyz.lynxs.terrarium;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.Path;


public class TerrariumConfig {
    public URI ELEVATION_URL = URI.create("https://s3.amazonaws.com/elevation-tiles-prod/terrarium/");
    public URI WATER_URL = URI.create("https://data.lynxs.xyz/water/");
    public URI BIOME_URL = URI.create("https://data.lynxs.xyz/biome/");
    public String CACHE_DIR = "./tiles";


    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    private static File getFile(Path filename, boolean type) {
        return type ? FabricLoader.getInstance().getConfigDir().resolve(filename).toFile() : filename.toFile();
    }

    public static <T> @NotNull T load(Class<T> clazz, Path filename, boolean type) {

        var file = getFile(filename, type);
        T config = null;

        if (file.exists()) {
            try (var reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(file)
                    )
            )) {
                config = GSON.fromJson(reader, clazz);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (config == null) {
            try {
                config = clazz.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        TerrariumConfig.save(config,  filename, type);
        return config;
    }

    public static <T> void save(T config, Path filename, boolean type) {

        File file = getFile(filename, type);
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdir()) {
                System.err.println("Failed to create a directory for " + file);
            }
        }

        try (var writer = new OutputStreamWriter(
                new FileOutputStream(file)
        )) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
package xyz.lynxs.terrarium;


import java.nio.file.Path;
import java.util.ArrayList;

import java.util.function.Consumer;


record Config<T>(Class<T> clazz, Consumer<T> apply, Path filename) {}

public class ConfigManager {
    private static final ArrayList<Config<?>> configs = new ArrayList<>();

    public static <T> T register(Class<T> clazz, Path filename, Consumer<T> apply) {
        configs.add(new Config<>(clazz, apply, filename));
        return TerrariumConfig.load(clazz, filename, true);
    }

}
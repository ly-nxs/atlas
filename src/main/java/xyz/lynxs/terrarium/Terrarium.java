package xyz.lynxs.terrarium;


import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import xyz.lynxs.terrarium.preset.PresetConfig;
import xyz.lynxs.terrarium.world.gen.TerrariumRegistries;
import xyz.lynxs.terrarium.world.gen.biome.BiomeColorRegistry;
import xyz.lynxs.terrarium.world.gen.biome.TerrariumBiomeSource;
import xyz.lynxs.terrarium.world.gen.chunk.TerrariumChunkGenerator;

import net.fabricmc.api.ModInitializer;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static xyz.lynxs.terrarium.TerrariumConfig.load;
import static xyz.lynxs.terrarium.world.gen.HeightProvider.init;

public class Terrarium implements ModInitializer {
    public static final String MOD_ID = "terrarium";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static PresetConfig CONFIG = new PresetConfig();
    public  static TerrariumConfig CONFIG1 = ConfigManager.register(TerrariumConfig.class, Path.of("terrarium.json"), newConfig -> CONFIG1 = newConfig);
    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    // Server-side world load
    public static void onServerWorldLoad(MinecraftServer server, ServerWorld world) {
        TerrariumBiomeSource.sampler = new PerlinNoiseSampler(world.getRandom());


        if (world.getChunkManager().getChunkGenerator().getCodecKey().get().getValue().getNamespace().contains("terrarium")) {
            BiomeColorRegistry.load(server.getResourceManager(), server);
            CONFIG = load(CONFIG.getClass(), server.getSavePath(WorldSavePath.ROOT).resolve("terrarium.json"), false);
            init();
            LOGGER.info("Terrarium World Loaded!");
        }
    }


    @Override
    public void onInitialize() {
        LOGGER.info("Terrarium Loaded");
        // Register custom chunk generator
        Registry.register(
                Registries.BIOME_SOURCE,
                id("biome_source"),
                TerrariumBiomeSource.CODEC
        );

        // Register chunk generator
        Registry.register(
                Registries.CHUNK_GENERATOR,
                id("chunk_generator"),
                TerrariumChunkGenerator.CODEC
        );
        TerrariumRegistries.register();
        ServerWorldEvents.LOAD.register(Terrarium::onServerWorldLoad);

    }




}

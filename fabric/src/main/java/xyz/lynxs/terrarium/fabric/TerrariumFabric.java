package xyz.lynxs.terrarium.fabric;

import dev.architectury.registry.registries.DeferredRegister;
import net.fabricmc.fabric.impl.biome.modification.BuiltInRegistryKeys;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.DimensionTypes;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;
import xyz.lynxs.terrarium.Terrarium;
import net.fabricmc.api.ModInitializer;
import xyz.lynxs.terrarium.gen.TerrariumDimensionType;
import xyz.lynxs.terrarium.gen.TerrariumRegistries;

public final class TerrariumFabric implements ModInitializer {

    public static ResourceKey<DimensionType> END;
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        Terrarium.init();
        TerrariumRegistries.register();


    }
}

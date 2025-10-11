package xyz.lynxs.terrarium.gen;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import xyz.lynxs.terrarium.Terrarium;
import xyz.lynxs.terrarium.gen.dfs.*;

public class TerrariumRegistries {
    public static void register() {
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, Terrarium.id("heightmap"), HeightmapDensity.CODEC.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE,Terrarium.id("depth"), DepthDensity.CODEC.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE,Terrarium.id("weirdness"), WeirdnessDensity.CODEC.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE,Terrarium.id("erosion"), ErosionDensity.CODEC.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE,Terrarium.id("humidity"), HumidityDensity.CODEC.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE,Terrarium.id("temperature"), TemperatureDensity.CODEC.codec());
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE,Terrarium.id("continentalness"), ContinentalnessDensity.CODEC.codec());
    }
}


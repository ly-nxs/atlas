package xyz.lynxs.terrarium.world.gen.biome;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import xyz.lynxs.terrarium.world.gen.BiomeProvider;


import java.util.List;
import java.util.stream.Stream;

import static xyz.lynxs.terrarium.Terrarium.CONFIG;
import static xyz.lynxs.terrarium.world.gen.BiomeProvider.getClimate;
import static xyz.lynxs.terrarium.world.gen.HeightProvider.getElevation;


public class TerrariumBiomeSource extends BiomeSource {


    private static final MapCodec<RegistryEntry<Biome>> BIOME_CODEC;
    /**
     * Used to parse a custom biome source, when a preset hasn't been provided.
     */
    public static final MapCodec<MultiNoiseUtil.Entries<RegistryEntry<Biome>>> CUSTOM_CODEC;
    private static final MapCodec<RegistryEntry<MultiNoiseBiomeSourceParameterList>> PRESET_CODEC;
    public static final Codec<TerrariumBiomeSource> CODEC;
    private final Either<MultiNoiseUtil.Entries<RegistryEntry<Biome>>, RegistryEntry<MultiNoiseBiomeSourceParameterList>> biomeEntries;
    public static PerlinNoiseSampler sampler;
    public TerrariumBiomeSource(Either<MultiNoiseUtil.Entries<RegistryEntry<Biome>>, RegistryEntry<MultiNoiseBiomeSourceParameterList>> biomeRegistry) {
        this.biomeEntries = biomeRegistry;

    }
    private MultiNoiseUtil.Entries<RegistryEntry<Biome>> getBiomeRegistry() {
        return this.biomeEntries.map((entries) -> {
            return entries;
        }, (parameterListEntry) -> {
            return parameterListEntry.value().getEntries();
        });
    }



    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        return MultiNoiseBiomeSource.CODEC;
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        return this.getBiomeRegistry().getEntries().stream().map(Pair::getSecond);
    }


    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
        int adjustedX = x + CONFIG.adjustXoffset;
        int adjustedZ = z + CONFIG.adjustZoffset;
        int elevation = getElevation(adjustedX, adjustedZ) + CONFIG.startingY;
        // Get climate and biome tag safely
        int climate = BiomeProvider.getClimate(adjustedX, adjustedZ);
        TagKey<Biome> biomeTag = BiomeColorRegistry.getTagForColor(climate); // Fallback to plains if not found
        RegistryEntryList<Biome> biomes = elevation > 64 ? BiomeColorRegistry.getBiomesForTag(biomeTag) : BiomeColorRegistry.getBiomesForTag(BiomeTags.IS_OCEAN);

        double noiseVal = Math.abs(sampler.sample(((double) x / biomes.size()) * CONFIG.noise_biome_scale, y * 0.1 , ((double) z / biomes.size()) * CONFIG.noise_biome_scale));

        return biomes.get((int) ((noiseVal) * biomes.size()));
    }



    @Override
    public void addDebugInfo(List<String> info, BlockPos pos, MultiNoiseUtil.MultiNoiseSampler noiseSampler) {
        int i = BiomeCoords.fromBlock(pos.getX());
        int k = BiomeCoords.fromBlock(pos.getZ());
        int adjustedZ = k + CONFIG.adjustZoffset;
        int adjustedX = i + CONFIG.adjustXoffset;
        int climate = getClimate(adjustedX, adjustedZ);
        info.add(
                "Biome builder PV: "
                        + "Biome: " + BiomeColorRegistry.getMappingForTag(BiomeColorRegistry.getTagForColor(climate)).comment().orElse("error")

        );
    }
    static {

        BIOME_CODEC = Biome.REGISTRY_CODEC.fieldOf("biome");
        CUSTOM_CODEC = MultiNoiseUtil.Entries.createCodec(BIOME_CODEC).fieldOf("biomes");
        PRESET_CODEC = MultiNoiseBiomeSourceParameterList.REGISTRY_CODEC.fieldOf("preset").withLifecycle(Lifecycle.stable());
        CODEC = Codec.mapEither(CUSTOM_CODEC, PRESET_CODEC).xmap(TerrariumBiomeSource::new, source -> source.biomeEntries).codec();
    }

}
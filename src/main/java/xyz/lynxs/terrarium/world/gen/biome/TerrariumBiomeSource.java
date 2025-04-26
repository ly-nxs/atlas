package xyz.lynxs.terrarium.world.gen.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import xyz.lynxs.terrarium.world.gen.HeightProvider;


import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static xyz.lynxs.terrarium.Terrarium.CONFIG;
import static xyz.lynxs.terrarium.Util.truncate;
import static xyz.lynxs.terrarium.world.gen.BiomeProvider.getClimate;


public class TerrariumBiomeSource extends BiomeSource {
    private final List<BiomeEntry> biomeEntries;
    private final PerlinNoiseSampler noiseSampler;
    private final RegistryEntry<ChunkGeneratorSettings> settings;
    private final Double noiseScales;


    public static final Codec<TerrariumBiomeSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeEntry.CODEC.listOf().fieldOf("biomes").forGetter(source -> source.biomeEntries),
                    ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter(source -> source.settings),
                    Codec.DOUBLE.fieldOf("noise_scale").forGetter(source -> source.noiseScales)
            ).apply(instance, TerrariumBiomeSource::new)
    );

    public TerrariumBiomeSource(List<BiomeEntry> biomeEntries, RegistryEntry<ChunkGeneratorSettings> settings, Double noiseScales ) {
        this(biomeEntries, Random.create(), settings, noiseScales);
    }

    public TerrariumBiomeSource(List<BiomeEntry> biomeEntries, Random random, RegistryEntry<ChunkGeneratorSettings> settings, Double noiseScales) {
        this.biomeEntries = biomeEntries;
        this.noiseSampler = new PerlinNoiseSampler(random);
        this.settings = settings;
        this.noiseScales = noiseScales;
    }


    public record BiomeEntry(
            RegistryEntry<Biome> biome,
            double precipitation,
            double temperature,
            double noiseWeight
    ) {
        public static final Codec<BiomeEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Biome.REGISTRY_CODEC.fieldOf("biome").forGetter(BiomeEntry::biome),
                        Codec.DOUBLE.fieldOf("precipitation").forGetter(BiomeEntry::precipitation) ,
                        Codec.DOUBLE.fieldOf("temperature").forGetter(BiomeEntry::temperature),
                        Codec.DOUBLE.fieldOf("noise_weight").forGetter(BiomeEntry::noiseWeight)
                ).apply(instance, BiomeEntry::new)
        );
    }

    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        return biomeEntries.stream().map(BiomeEntry::biome);
    }


    @Override
    public RegistryEntry<Biome> getBiome(int x, int elevation, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
        int adjustedX = x + CONFIG.adjustXoffset;
        int adjustedZ = z + CONFIG.adjustZoffset;
        double precipitation = (adjustedX > 0 && adjustedZ > 0) && (adjustedX < HeightProvider.size && adjustedZ < HeightProvider.size) ? getClimate(adjustedX, adjustedZ, true) : 2;
        double temperature = (adjustedX > 0 && adjustedZ > 0) && (adjustedX < HeightProvider.size && adjustedZ < HeightProvider.size) ?  getClimate(adjustedX, adjustedZ, false) : 2;

        precipitation =  precipitation > 1 ? noise.sample(x, elevation, z).humidityNoise() : precipitation;
        temperature =  temperature > 1 ? noise.sample(x, elevation, z).temperatureNoise() : temperature;

        return findBestBiome(precipitation, temperature, getNoiseValue(adjustedX, elevation, adjustedZ));
    }


    private double getNoiseValue(int x, int elevation, int z) {
        return noiseSampler.sample(x * CONFIG.noise_biome_scale, elevation * CONFIG.noise_biome_scale, z * CONFIG.noise_biome_scale);
    }

    private RegistryEntry<Biome> findBestBiome(double precip, double temperature, double noise) {
        if (biomeEntries.isEmpty()) {
            throw new IllegalStateException("No biomes available!");
        }

        return biomeEntries.stream()
                .min(Comparator.comparingDouble(b -> {
                    // Calculate squared Euclidean distance
                    double precipDiff = Math.pow(precip - b.precipitation(), 2) * 0.5;
                    double tempDiff = Math.pow(temperature - b.temperature(), 2) ;
                    double noiseDiff = Math.pow(noise - b.noiseWeight(), 2);
                    return Math.sqrt(precipDiff + tempDiff + noiseDiff);
                }))
                .orElseThrow() // Should not throw if biomeEntries is not empty
                .biome();
    }




    @Override
    public void addDebugInfo(List<String> info, BlockPos pos, MultiNoiseUtil.MultiNoiseSampler noiseSampler) {
        int i = BiomeCoords.fromBlock(pos.getX());
        int j = BiomeCoords.fromBlock(pos.getY());
        int k = BiomeCoords.fromBlock(pos.getZ());
        int adjustedZ = k + CONFIG.adjustZoffset;
        int adjustedX = i + CONFIG.adjustXoffset;

        info.add(
                "Biome builder PV: "
                        + " Precipitation: "
                        + truncate((adjustedX > 0 && adjustedZ > 0) && (adjustedX < HeightProvider.size && adjustedZ < HeightProvider.size) ? getClimate(adjustedX, adjustedZ, true) : -1.000, 3)
                        + " Temperature: "
                        + truncate((adjustedX > 0 && adjustedZ > 0) && (adjustedX < HeightProvider.size && adjustedZ < HeightProvider.size) ? getClimate(adjustedX, adjustedZ, false) : -1.000, 3)
                        + " Noise: "
                        + truncate(getNoiseValue(i, j, k), 3)
        );
    }

}
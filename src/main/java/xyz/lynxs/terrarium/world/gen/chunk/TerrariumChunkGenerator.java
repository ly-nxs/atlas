package xyz.lynxs.terrarium.world.gen.chunk;

import com.google.common.annotations.VisibleForTesting;
import xyz.lynxs.terrarium.accessor.TerrariumSurfaceBuilderAccessor;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.StructureWeightSampler;
import net.minecraft.world.gen.carver.CarverContext;
import net.minecraft.world.gen.carver.CarvingMask;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.noise.NoiseConfig;
import xyz.lynxs.terrarium.world.gen.biome.TerrariumBiomeSource;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static xyz.lynxs.terrarium.Terrarium.CONFIG;
import static xyz.lynxs.terrarium.Util.gridToLatLon;
import static xyz.lynxs.terrarium.world.gen.HeightProvider.*;

public class TerrariumChunkGenerator extends ChunkGenerator {
    private static final BlockState AIR = Blocks.AIR.getDefaultState();

    private final RegistryEntry<ChunkGeneratorSettings> settings;

    private final TerrariumBiomeSource biomeSource;

    public TerrariumChunkGenerator(
            TerrariumBiomeSource biomeSource, // Use custom type instead of generic BiomeSource
            RegistryEntry<ChunkGeneratorSettings> settings
    ) {
        super(biomeSource); // Pass to parent

        this.biomeSource = biomeSource; // Store reference
        this.settings = settings;
    }


    public int getFromMap(int x, int z) {
        // Center offset - assumes world center is land
        int adjustedX = x + CONFIG.adjustXoffset;
        int adjustedZ = z + CONFIG.adjustZoffset;

        if (adjustedX < 0 || adjustedZ < 0 || adjustedX > size || adjustedZ > size)
            return getMinimumY() - 1;

        return getElevation(adjustedX, adjustedZ) + CONFIG.startingY;
    }

    public RegistryEntry<ChunkGeneratorSettings> getSettings() {
        return this.settings;
    }

    public static final MapCodec<TerrariumChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    TerrariumBiomeSource.CODEC // Use your biome source's CODEC
                            .fieldOf("biome_source")
                            .forGetter(TerrariumChunkGenerator::getBiomeSource),
                    ChunkGeneratorSettings.REGISTRY_CODEC
                            .fieldOf("settings")
                            .forGetter(TerrariumChunkGenerator::getSettings)
            ).apply(instance, TerrariumChunkGenerator::new)
    );

    public TerrariumBiomeSource getBiomeSource() {
        return this.biomeSource;
    }

    /**
     */
    @Override
    protected MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }



    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk2) {

        BiomeAccess biomeAccess2 = biomeAccess.withSource((biomeX, biomeY, biomeZ) -> this.biomeSource.getBiome(biomeX, biomeY, biomeZ, noiseConfig.getMultiNoiseSampler()));
        ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(RandomSeed.getSeed()));
        int i = 8;
        ChunkPos chunkPos = chunk2.getPos();
        ChunkNoiseSampler chunkNoiseSampler = chunk2.getOrCreateChunkNoiseSampler(chunk -> this.createChunkNoiseSampler(chunk, structureAccessor, Blender.getBlender(chunkRegion), noiseConfig));
        AquiferSampler aquiferSampler = chunkNoiseSampler.getAquiferSampler();
        CarverContext carverContext = new CarverContext(new NoiseChunkGenerator(this.biomeSource, this.settings),
                /*this is fine because the only thing the NCG is used for is like, the height limit or something*/
                chunkRegion.getRegistryManager(), chunk2.getHeightLimitView(), chunkNoiseSampler, noiseConfig, this.settings.value().surfaceRule());
        CarvingMask carvingMask = ((ProtoChunk) chunk2).getOrCreateCarvingMask();
        for (int j = -i; j <= i; ++j) {
            for (int k = -i; k <= i; ++k) {
                ChunkPos chunkPos2 = new ChunkPos(chunkPos.x + j, chunkPos.z + k);
                Chunk chunk22 = chunkRegion.getChunk(chunkPos2.x, chunkPos2.z);
                RegistryEntry<Biome> biome = this.biomeSource.getBiome(BiomeCoords.fromBlock(chunkPos2.getStartX()), 0, BiomeCoords.fromBlock(chunkPos2.getStartZ()), noiseConfig.getMultiNoiseSampler());
                GenerationSettings generationSettings = chunk22.getOrCreateGenerationSettings(() -> this.getGenerationSettings(biome));
                Iterable<RegistryEntry<ConfiguredCarver<?>>> iterable = generationSettings.getCarversForStep();
                int l = 0;
                for (RegistryEntry<ConfiguredCarver<?>> registryEntry : iterable) {
                    ConfiguredCarver<?> configuredCarver = registryEntry.value();
                    chunkRandom.setCarverSeed(seed + (long) l, chunkPos2.x, chunkPos2.z);
                    if (configuredCarver.shouldCarve(chunkRandom)) {
                        configuredCarver.carve(carverContext, chunk2, biomeAccess2::getBiome, chunkRandom, aquiferSampler, chunkPos2, carvingMask);
                    }
                    ++l;
                }
            }
        }
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        if (SharedConstants.isOutsideGenerationArea(chunk.getPos())) {
            return;
        }
        HeightContext heightContext = new HeightContext(this, region);
        this.buildSurface(chunk, heightContext, noiseConfig, structures, region.getBiomeAccess(), region.getRegistryManager().getOrThrow(RegistryKeys.BIOME), Blender.getBlender(region));
    }

    @VisibleForTesting
    public void buildSurface(Chunk chunk, HeightContext heightContext, NoiseConfig noiseConfig, StructureAccessor structureAccessor, BiomeAccess biomeAccess, Registry<Biome> biomeRegistry, Blender blender) {
        ChunkNoiseSampler chunkNoiseSampler = chunk.getOrCreateChunkNoiseSampler(chunk3 -> this.createChunkNoiseSampler(chunk3, structureAccessor, blender, noiseConfig));
        ChunkGeneratorSettings chunkGeneratorSettings = this.settings.value();
        ((TerrariumSurfaceBuilderAccessor) noiseConfig.getSurfaceBuilder()).buildSurface(noiseConfig, biomeAccess, biomeRegistry, chunkGeneratorSettings.usesLegacyRandom(), heightContext, chunk, chunkNoiseSampler, chunkGeneratorSettings.surfaceRule());
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        ChunkPos chunkPos = region.getCenterPos();
        RegistryEntry<Biome> registryEntry = region.getBiome(chunkPos.getStartPos().withY(region.getTopYInclusive() - 1));
        ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(RandomSeed.getSeed()));
        chunkRandom.setPopulationSeed(region.getSeed(), chunkPos.getStartX(), chunkPos.getStartZ());
        SpawnHelper.populateEntities(region, registryEntry, chunkPos, chunkRandom);
    }

    @Override
    public int getWorldHeight() {
        return this.settings.value().generationShapeConfig().height();
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        GenerationShapeConfig generationShapeConfig = this.settings.value().generationShapeConfig().trimHeight(chunk.getHeightLimitView());
        int k = MathHelper.floorDiv(generationShapeConfig.height(), generationShapeConfig.verticalSize());
        if (k <= 0) {
            return CompletableFuture.completedFuture(chunk);
        }
        int x = (chunk.getPos().x << 4) + CONFIG.adjustXoffset;
        int z = (chunk.getPos().z << 4) + CONFIG.adjustZoffset;

        if (x < -16 || z < -16) return CompletableFuture.completedFuture(chunk);
        return CompletableFuture.supplyAsync(Util.debugSupplier(() -> this.populateNoise(chunk), () -> "terrarium_cgen"), Util.getMainWorkerExecutor());
    }

    private Chunk populateNoise(Chunk chunk) {
        Heightmap oceanHeightmap = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        Heightmap surfaceHeightmap = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
        ChunkPos chunkPos = chunk.getPos();
        int i = chunkPos.getStartX();
        int j = chunkPos.getStartZ();
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        BlockState defaultFluid = this.settings.value().defaultFluid();

        for (int ii = 0; ii < 16; ii++) {
            for(int jj = 0; jj < 16; jj++){
                int seaLevel = 64;
                int elevation = getFromMap(i + ii, j + jj);
                for(int yy = this.settings.value().generationShapeConfig().minimumY(); yy < this.settings.value().generationShapeConfig().height(); yy++){
                    mutable.set(i + ii, yy, j + jj);

                    BlockState state;

                        if (yy <= seaLevel && yy >= elevation) {
                            state = defaultFluid;
                        } else if (yy < elevation) {
                            state = this.settings.value().defaultBlock(); //getBlock(blockX, blockZ, blockY);
                        } else {
                            state = AIR;
                        }

                        chunk.setBlockState(mutable, state, 0);
                        surfaceHeightmap.trackUpdate((i + ii) & 0xF, yy, (j + jj) & 0xF, state);
                        oceanHeightmap.trackUpdate((i + ii) & 0xF, yy, (j + jj) & 0xF, state);
                        mutable.set((i + ii), yy, (j + jj));
                        chunk.markBlockForPostProcessing(mutable);
                }
            }
        }
        return chunk;
    }

    @Override
    public int getSeaLevel() {
        return this.settings.value().seaLevel();
    }

    public int getSeaLevel(int x, int z) {
        return this.settings.value().seaLevel();
    }

    @Override
    public int getMinimumY() {
        return this.settings.value().generationShapeConfig().minimumY();
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        return (
//                (heightmap == Heightmap.Type.OCEAN_FLOOR_WG || heightmap == Heightmap.Type.OCEAN_FLOOR)
//                        ? this.getFromMap(x, z, this.heightmap) :
//                Math.max(this.seaLevel,
                this.getFromMap(x, z)
//                )
        );
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        int elevation = this.getFromMap(x, z);
        int seaLevel = this.getSeaLevel(x, z);
        if (elevation < this.getMinimumY())
            return new VerticalBlockSample(world.getBottomY(), new BlockState[]{Blocks.AIR.getDefaultState()});
        if (elevation < seaLevel) {
            return new VerticalBlockSample(
                    this.settings.value().generationShapeConfig().minimumY(),
                    Stream.concat(
                            Stream.generate(() -> this.settings.value().defaultBlock()).limit(elevation - this.getMinimumY()),
                            Stream.generate(() -> this.settings.value().defaultFluid()).limit(seaLevel - elevation - this.getMinimumY())
                    ).toArray(BlockState[]::new));
        }
        return new VerticalBlockSample(
                this.settings.value().generationShapeConfig().minimumY(),
                Stream.generate(() -> this.settings.value().defaultBlock()).limit(elevation - this.getMinimumY() + 1).toArray(BlockState[]::new)

        );
    }

    @Override
    public void appendDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add(
                "[Terrarium] Elevation: " + getFromMap(pos.getX(), pos.getZ()) +
                        ", Cords: " + Arrays.toString(gridToLatLon(pos.getX() + CONFIG.adjustXoffset, pos.getZ() + CONFIG.adjustZoffset, size))
        );
    }


    private ChunkNoiseSampler createChunkNoiseSampler(Chunk chunk, StructureAccessor world, Blender blender, NoiseConfig noiseConfig) {
        return ChunkNoiseSampler.create(
                chunk,
                noiseConfig,
                StructureWeightSampler.createStructureWeightSampler(world, chunk.getPos()),
                this.settings.value(),
                (x, y, z) -> new AquiferSampler.FluidLevel(getSeaLevel(), settings.value().defaultFluid()),
                blender
        );
    }

}

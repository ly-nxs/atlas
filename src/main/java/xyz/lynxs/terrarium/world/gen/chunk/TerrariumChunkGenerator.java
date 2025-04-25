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
import net.minecraft.world.chunk.ChunkSection;
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
import java.util.function.Supplier;
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


        int minimumCellY = MathHelper.floorDiv(generationShapeConfig.minimumY(), generationShapeConfig.verticalCellBlockCount());
        int cellHeight = MathHelper.floorDiv(generationShapeConfig.height(), generationShapeConfig.verticalCellBlockCount());
        if (x < -16 || z < -16) return CompletableFuture.completedFuture(chunk);
        return CompletableFuture.supplyAsync(Util.debugSupplier(() -> this.populateNoise(chunk, structureAccessor, blender, noiseConfig, minimumCellY, cellHeight), () -> "wgen_fill_noise"), Util.getMainWorkerExecutor());
    }

    private Chunk populateNoise(Chunk chunk, StructureAccessor accessor, Blender blender, NoiseConfig noiseConfig, int minimumCellY, int cellHeight) {
        ChunkNoiseSampler chunkNoiseSampler = chunk.getOrCreateChunkNoiseSampler(chunk1 -> this.createChunkNoiseSampler(chunk, accessor, blender, noiseConfig));
        Heightmap oceanHeightmap = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        Heightmap surfaceHeightmap = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
        ChunkPos chunkPos = chunk.getPos();
        int i = chunkPos.getStartX();
        int j = chunkPos.getStartZ();
        chunkNoiseSampler.sampleStartDensity();
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        int k = chunkNoiseSampler.getHorizontalCellBlockCount();
        int l = chunkNoiseSampler.getVerticalCellBlockCount();
        int m = 16 / k;
        int n = 16 / k;


        BlockState defaultFluid = this.settings.value().defaultFluid();
        for (int o = 0; o < m; ++o) {
            chunkNoiseSampler.sampleEndDensity(o);
            for (int p = 0; p < n; ++p) {
                int q1 = chunk.countVerticalSections() - 1;
                ChunkSection chunkSection = chunk.getSection(q1);
                for (int q = cellHeight - 1; q >= 0; --q) {
                    chunkNoiseSampler.onSampledCellCorners(q, p);
                    for (int r = l - 1; r >= 0; --r) {
                        int s = (minimumCellY + q) * l + r;
                        int t = s & 0xF;
                        int u = chunk.getSectionIndex(s);
                        if (q1 != u) {
                            q1 = u;
                            chunkSection = chunk.getSection(u);
                        }
                        double d = (double) r / (double) l;
                        chunkNoiseSampler.interpolateY(s, d);
                        for (int v = 0; v < k; ++v) {
                            int w = i + o * k + v;
                            int x = w & 0xF;
                            double e = (double) v / (double) k;
                            chunkNoiseSampler.interpolateX(w, e);
                            for (int y = 0; y < k; ++y) {

                                int z = j + p * k + y;
                                int aa = z & 0xF;
                                double f = (double) y / (double) k;
                                chunkNoiseSampler.interpolateZ(z, f);
                                int blockX = chunkNoiseSampler.blockX();
                                int blockY = chunkNoiseSampler.blockY();
                                int blockZ = chunkNoiseSampler.blockZ();

                                mutable.set(blockX, blockY, blockZ);
                                int seaLevel = this.getSeaLevel(blockX, blockZ);
                                int elevation = getFromMap(blockX, blockZ);

                                BlockState state;
                                if (elevation - blockY <= 10) {
                                    if (blockY <= seaLevel && blockY >= elevation) {
                                        state = defaultFluid;
                                    } else if (blockY < elevation) {
                                        state = this.settings.value().defaultBlock(); //getBlock(blockX, blockZ, blockY);
                                    } else {
                                        state = AIR;
                                    }
                                    chunk.setBlockState(mutable, state, false);
                                    surfaceHeightmap.trackUpdate(blockX & 0xF, blockY, blockZ & 0xF, state);
                                    oceanHeightmap.trackUpdate(blockX & 0xF, blockY, blockZ & 0xF, state);
                                } else {
                                    state = chunkNoiseSampler.sampleBlockState();

                                    state = this.settings.value().defaultBlock();

                                    if ((SharedConstants.isOutsideGenerationArea(chunk.getPos())))
                                        continue;
                                    chunkSection.setBlockState(x, t, aa, state, false);
                                    oceanHeightmap.trackUpdate(x, s, aa, state);
                                    surfaceHeightmap.trackUpdate(x, s, aa, state);
                                }
                                mutable.set(w, s,z);
                                //if (!aquiferSampler.needsFluidTick() || state.getFluidState().isEmpty()) continue;
                                chunk.markBlockForPostProcessing(mutable);
                                //buildSurface(chunk, new HeightContext(this, chunk.getHeightLimitView()),noiseConfig, accessor, null, accessor.getRegistryManager().get(), blender);
                            }
                        }
                    }
                }
            }
            chunkNoiseSampler.swapBuffers();
        }
        chunkNoiseSampler.stopInterpolation();

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

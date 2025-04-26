package xyz.lynxs.terrarium.mixin;


import com.mojang.datafixers.DataFixer;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.ChunkStatusChangeListener;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.lynxs.terrarium.world.gen.chunk.TerrariumChunkGenerator;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * this mixin ensures that when the world is created, the noise samplers in the density functions get populated with
 * samplers generated from the world seed
 */
@Mixin(ThreadedAnvilChunkStorage.class)
public class ThreadedAnvilChunkStorageMixin {
    @Mutable
    @Shadow
    @Final
    private NoiseConfig noiseConfig;

    @Inject(method = "<init>",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/gen/chunk/ChunkGenerator;createStructurePlacementCalculator(Lnet/minecraft/registry/RegistryWrapper;Lnet/minecraft/world/gen/noise/NoiseConfig;J)Lnet/minecraft/world/gen/chunk/placement/StructurePlacementCalculator;",
                    shift = At.Shift.BEFORE)
    )
    private void terrarium_populateNoises(
            ServerWorld world,
            LevelStorage.Session session,
            DataFixer dataFixer,
            StructureTemplateManager structureTemplateManager,
            Executor executor,
            ThreadExecutor<Runnable> mainThreadExecutor,
            ChunkProvider chunkProvider,
            ChunkGenerator chunkGenerator,
            WorldGenerationProgressListener worldGenerationProgressListener,
            ChunkStatusChangeListener chunkStatusChangeListener,
            Supplier<PersistentStateManager> persistentStateManagerFactory,
            int viewDistance,
            boolean dsync,
            CallbackInfo ci
    ) {
        if (chunkGenerator instanceof TerrariumChunkGenerator terrariumChunkGenerator) {
            this.noiseConfig = NoiseConfig.create(
                    terrariumChunkGenerator.getSettings().value(),
                    world.getRegistryManager().getWrapperOrThrow(RegistryKeys.NOISE_PARAMETERS),
                    world.getSeed()
            );
        }
    }
}
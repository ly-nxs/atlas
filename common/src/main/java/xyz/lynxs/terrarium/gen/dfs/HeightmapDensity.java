package xyz.lynxs.terrarium.gen.dfs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;
import xyz.lynxs.terrarium.gen.HeightProvider;

import static xyz.lynxs.terrarium.Terrarium.CONFIG;

/**
 * Gets a density function from terrarium
 */
public record HeightmapDensity(int depth) implements DensityFunction.SimpleFunction {
    public static final KeyDispatchDataCodec<HeightmapDensity> CODEC = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                            Codec.INT.optionalFieldOf("depth", 5).forGetter(HeightmapDensity::depth))
                    .apply(instance, HeightmapDensity::new)));
    @Override
    public double compute(FunctionContext pos) {
        return pos.blockY() > HeightProvider.getElevation(pos.blockX(), pos.blockZ()) + CONFIG.startingY ? -1 : 1;
    }

    @Override
    public double minValue() {
        return -1;
    }

    @Override
    public double maxValue() {
        return 1;
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

}
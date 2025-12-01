package xyz.lynxs.terrarium.gen.dfs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

import static xyz.lynxs.terrarium.Terrarium.CONFIG;

/**
 * Populates depth values
 */
public record DepthDensity(int depth) implements DensityFunction.SimpleFunction {
    public static final KeyDispatchDataCodec<DepthDensity> CODEC = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                            Codec.INT.optionalFieldOf("depth", 30).forGetter(DepthDensity::depth))
                    .apply(instance, DepthDensity::new)));
    @Override
    public double compute(FunctionContext pos) {
           /*
        simple height based - centered at y=64
    */
        int centerY = 64;
        int maxHeight = CONFIG.worldHeight;
        int minHeight = -64; // or whatever your world's minimum is

        double normalizedHeight;
        if (pos.blockY() >= centerY) {
            // Above center: map to [0, 1]
            normalizedHeight = (double) (pos.blockY() - centerY) / (maxHeight - centerY);
        } else {
            // Below center: map to [-1, 0]
            normalizedHeight = (double) (pos.blockY() - centerY) / (centerY - minHeight);
        }

        return Math.clamp(normalizedHeight, -1.0, 1.0);
    }

    @Override
    public double minValue() {
        return -1.0;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

}
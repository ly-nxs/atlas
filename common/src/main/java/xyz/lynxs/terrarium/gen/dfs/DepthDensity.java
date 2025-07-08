package xyz.lynxs.terrarium.gen.dfs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

import static xyz.lynxs.terrarium.gen.HeightProvider.getElevation;

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
        min 0.0: Land biomes
        0.2-0.9: Caves
        1.0: Land biomes
        max 1.1: Deep Dark
        */
        short height = getElevation(pos.blockX(), pos.blockZ());
        if(pos.blockY() < height - depth){
            if(pos.blockY() < 0){
                return 1.1;
            }
            return 0.5;
        }
        return 0.0;
    }

    @Override
    public double minValue() {
        return 0;
    }

    @Override
    public double maxValue() {
        return 1.1;
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

}
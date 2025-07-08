package xyz.lynxs.terrarium.gen.dfs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

import static xyz.lynxs.terrarium.Terrarium.CONFIG;

/**
 * Populates Continentalness values
 */
public record ContinentalnessDensity(int depth) implements DensityFunction.SimpleFunction {
    public static final KeyDispatchDataCodec<ContinentalnessDensity> CODEC = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                            Codec.INT.optionalFieldOf("depth", 2).forGetter(ContinentalnessDensity::depth))
                    .apply(instance, ContinentalnessDensity::new)));
    @Override
    public double compute(FunctionContext pos) {
        /*
        Simple height based
        */
        if(Math.abs(pos.blockY() - 64) < depth){
            return -0.14;
        }
        if(pos.blockY() < 64){
            return (0.86 - ((double) pos.blockY() / 64)) * -1;
        }
        return (double) (pos.blockY() - 64) / (CONFIG.worldHeight - 64);

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
package xyz.lynxs.terrarium.gen.dfs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

import static xyz.lynxs.terrarium.gen.HeightProvider.*;

/**
 * Populates Continentalness values
 */
public record ContinentalnessDensity(double depth) implements DensityFunction.SimpleFunction {
    public static final KeyDispatchDataCodec<ContinentalnessDensity> CODEC = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                            Codec.DOUBLE.optionalFieldOf("depth", 20.0).forGetter(ContinentalnessDensity::depth))
                    .apply(instance, ContinentalnessDensity::new)));
    @Override
    public double compute(FunctionContext pos) {
        /*
            localized height
        */

        return Mth.clamp((getMax(pos.blockX(), pos.blockZ()) - (getElevation(pos.blockX(), pos.blockZ()) + depth)) * -1.0, depth * -1.0, depth) / depth;
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
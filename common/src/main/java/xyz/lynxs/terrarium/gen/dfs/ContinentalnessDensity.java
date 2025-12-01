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
                            Codec.DOUBLE.optionalFieldOf("depth", 16.0).forGetter(ContinentalnessDensity::depth))
                    .apply(instance, ContinentalnessDensity::new)));
    @Override
    public double compute(FunctionContext pos) {
        /*
            localized height
            70h 57min 86max  70-57 /
        */
        short[] maxMin = getMaxMin(pos.blockX(), pos.blockZ());
        int elevation = getElevation(pos.blockX(), pos.blockZ());
        int min = maxMin[1];
        int max = maxMin[0];

        // Avoid division by zero
        if (max == min) {
            return 0.0;
        }

        int range = max - min;

        // Normalize to [0, 1], then convert to [-1, 1]
        double normalized = (double) (elevation - min) / range;
        double centered = (normalized * 2.0) - 1.0;  // Now in [-1, 1]

        // Scale by terrain variation (normalize range to some baseline)
        // Adjust the divisor (e.g., 50.0) based on your typical height variations
        double variationScale = Math.min(range / depth, 1.0);

        double result = centered * variationScale;

        // Clamp to [-1, 1] (though variationScale should handle this)
        return Math.max(-1.0, Math.min(1.0, result));
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
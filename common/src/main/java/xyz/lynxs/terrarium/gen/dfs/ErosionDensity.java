package xyz.lynxs.terrarium.gen.dfs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

import static xyz.lynxs.terrarium.gen.HeightProvider.*;

/**
 * Populates Erosion values
 */
public record ErosionDensity(double coefficient) implements DensityFunction.SimpleFunction {
    public static final KeyDispatchDataCodec<ErosionDensity> CODEC = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                            Codec.DOUBLE.optionalFieldOf("coefficient", 1.25).forGetter(ErosionDensity::coefficient))
                    .apply(instance, ErosionDensity::new)));
    @Override
    public double compute(FunctionContext pos) {
        /*
            Steepness
        */

        return Mth.clamp(getSteepness(pos.blockX(), pos.blockZ()) * coefficient, 0, 2) - 1.0;

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
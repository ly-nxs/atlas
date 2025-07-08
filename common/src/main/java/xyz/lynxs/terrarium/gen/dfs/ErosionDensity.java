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
public record ErosionDensity(int depth) implements DensityFunction.SimpleFunction {
    public static final KeyDispatchDataCodec<ErosionDensity> CODEC = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                            Codec.INT.optionalFieldOf("depth", 5).forGetter(ErosionDensity::depth))
                    .apply(instance, ErosionDensity::new)));
    @Override
    public double compute(FunctionContext pos) {
        /*
        min -1.0 -0.933: Peaks / Middle
        -0.767 -0.567: Badlands / Slopes
        -0.4 -0.267: Plateau / Middle
        -0.05 0.05: Middle
        0.267 0.4: Middle
        0.567 0.767: Windswept / Shattered
        max 0.933 1.0: Swamps / Middle
        */

        short height = getElevation(pos.blockX(), pos.blockZ());

        if(pos.blockY() < height - depth * 6){
            return 0.0;
        }
        short[] arr = getMinMax(pos.blockX(), pos.blockZ());

        return -1 + (((double) (height - arr[0]) / (arr[1] - arr[0])) + Mth.clamp(getSteepness(pos.blockX(), pos.blockZ()), 0.0, 1.0));

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
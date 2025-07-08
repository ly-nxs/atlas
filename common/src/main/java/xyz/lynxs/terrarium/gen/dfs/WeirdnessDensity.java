package xyz.lynxs.terrarium.gen.dfs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

import static xyz.lynxs.terrarium.gen.HeightProvider.*;

/**
 * Populates Weirdness values
 */
public record WeirdnessDensity(int depth) implements DensityFunction.SimpleFunction {
    public static final KeyDispatchDataCodec<WeirdnessDensity> CODEC = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                            Codec.INT.optionalFieldOf("depth", 3).forGetter(WeirdnessDensity::depth))
                    .apply(instance, WeirdnessDensity::new)));
    @Override
    public double compute(FunctionContext pos) {
        /*
        min -1.0 -0.933: Mid-slice: Plateau + Shattered + Slope
        -0.933 -0.767: High-slice: Plateau + Shattered + Slope + Peaks + No Coastal
        -0.767 -0.567: Peaks-slice: Plateau + Shattered + Slope + Peaks + No Coastal
        -0.567 -0.4: High-slice: Plateau + Shattered + Slope + Peaks + No Coastal
        -0.4 -0.267: Mid-slice: Plateau + Shattered + Slope
        -0.267 -0.05: Low-slice: Middle + Swamp
        -0.05 0.05: Valley-slice: Rivers
        0.05 0.267: Low-slice: Middle + Swamp
        0.267 0.4: Mid-slice: Plateau + Shattered + Slope
        0.5 0.567: High-slice: Plateau + Shattered + Slope + Peaks + No Coastal
        0.567 0.767: Peaks-slice: Plateau + Shattered + Slope + Peaks + No Coastal
        0.767 0.933: High-slice: Plateau + Shattered + Slope + Peaks + No Coastal
        max 0.933 1.0: Mid-slice: Plateau + Shattered + Slope
        */
        if(Math.abs(pos.blockY() - 64) < depth){
            return 0.2;
        }

        short height = getElevation(pos.blockX(), pos.blockZ());

        if(pos.blockY() < height - depth * 6){
            return -0.3;
        }
        short[] arr = getMinMax(pos.blockX(), pos.blockZ());

        return (double) (height - arr[0]) / (arr[1] - arr[0]) * 0.5 + 0.25;
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
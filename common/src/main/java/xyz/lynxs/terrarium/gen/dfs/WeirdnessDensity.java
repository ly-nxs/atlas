package xyz.lynxs.terrarium.gen.dfs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

import static xyz.lynxs.terrarium.Terrarium.CONFIG;
import static xyz.lynxs.terrarium.gen.HeightProvider.*;

/**
 * Populates Weirdness values
 */
public record WeirdnessDensity(int depth) implements DensityFunction.SimpleFunction {
    public static final KeyDispatchDataCodec<WeirdnessDensity> CODEC = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                            Codec.INT.optionalFieldOf("depth", 2).forGetter(WeirdnessDensity::depth))
                    .apply(instance, WeirdnessDensity::new)));
    @Override
    public double compute(FunctionContext pos) {
    /*
        y compared to elevation - 0 when y=elevation
    */
        int elevation = getElevation(pos.blockX(), pos.blockZ());
        int y = pos.blockY();

        // Avoid division by zero
        if (elevation == 0) {
            elevation = 1;
        }

        // Normalize the difference
        double normalized = (double) (y - elevation) / Math.abs(elevation);

        return Mth.clamp(normalized, -1.0, 1.0);
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
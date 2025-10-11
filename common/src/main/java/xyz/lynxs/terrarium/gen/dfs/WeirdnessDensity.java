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
            y compared to elevation
        */


        return Mth.clamp((double) pos.blockY() / (getElevation(pos.blockX(), pos.blockZ()) + CONFIG.startingY), 0, 2) - 1.0;
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
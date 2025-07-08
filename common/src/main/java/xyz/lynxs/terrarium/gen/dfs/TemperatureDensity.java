package xyz.lynxs.terrarium.gen.dfs;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

import static xyz.lynxs.terrarium.Terrarium.CONFIG;
import static xyz.lynxs.terrarium.gen.BiomeProvider.getTemperature;

/**
 * Populates Temperature values
 */
public record TemperatureDensity(NoiseHolder noise) implements DensityFunction.SimpleFunction {
    public static final KeyDispatchDataCodec<TemperatureDensity> CODEC = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                            NoiseHolder.CODEC.fieldOf("noise").forGetter(TemperatureDensity::noise)
            ).apply(instance, TemperatureDensity::new)));
    @Override
    public double compute(FunctionContext pos) {
        /*
        Simple region based TODO: Add noise
        */
        return (getTemperature(pos.blockX(), pos.blockZ()) + noise.getValue(pos.blockX(), pos.blockY(), pos.blockZ()) / 4.0) - ((double) pos.blockY() / CONFIG.worldHeight) / 2.0;
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
    @Override
    public @NotNull DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new TemperatureDensity(
                noise
        ));
    }

}
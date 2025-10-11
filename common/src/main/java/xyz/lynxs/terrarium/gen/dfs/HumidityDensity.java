package xyz.lynxs.terrarium.gen.dfs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

import static xyz.lynxs.terrarium.gen.BiomeProvider.getHumidity;

/**
 * Populates Humidity values
 */
public record HumidityDensity(DensityFunction noise) implements DensityFunction.SimpleFunction {
    public static final KeyDispatchDataCodec<HumidityDensity> CODEC = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DensityFunction.DIRECT_CODEC.fieldOf("noise").forGetter(HumidityDensity::noise)
            ).apply(instance, HumidityDensity::new)));
    @Override
    public double compute(FunctionContext pos) {
        return  getHumidity(pos.blockX(), pos.blockZ()) + noise.compute(pos) / 5.0;
    }

    @Override
    public double minValue() {return -1.0;}

    @Override
    public double maxValue() {return 1.0;}

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    @Override
    public @NotNull DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new HumidityDensity(
                noise.mapAll(visitor)
        ));
    }
}
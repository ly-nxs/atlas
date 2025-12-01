package xyz.lynxs.terrarium.gen.dfs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

import static xyz.lynxs.terrarium.Terrarium.CONFIG;

/**
 * Populates depth values with a deterministic white noise dither/warp.
 */
public record DomainWarpDensity(DensityFunction input, int scale) implements DensityFunction.SimpleFunction {
    public static final KeyDispatchDataCodec<DomainWarpDensity> CODEC = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                            DensityFunction.DIRECT_CODEC.fieldOf("input").forGetter(DomainWarpDensity::input),
                            Codec.INT.optionalFieldOf("scale", 10).forGetter(DomainWarpDensity::scale))
                    .apply(instance, DomainWarpDensity::new)));

    @Override
    public double compute(FunctionContext pos) {
        if (CONFIG.smootherBiomes) {
            return input.compute(pos);
        }

        // Get integer coordinates
        int x = pos.blockX();
        int y = pos.blockY();
        int z = pos.blockZ();

        // Generate two different deterministic "random" values for X and Z offsets.
        // We use slightly different inputs to the hash to ensure the X and Z offsets are different.
        double xOffset = hash(x, y, z);
        double zOffset = hash(z, x, y); // Swizzle coords for a different Z value

        // Apply the dither warp
        return input.compute(new SinglePointContext(
                (int) (x + (scale * xOffset)),
                y,
                (int) (z + (scale * zOffset))
        ));
    }

    /**
     * A simple and fast integer hashing function.
     * It takes integer coordinates and produces a deterministic, pseudo-random float between -1.0 and 1.0.
     * @return A float value in the range [-1.0, 1.0].
     */
    private static float hash(int x, int y, int z) {
        // A common hashing technique using large prime numbers and XOR bitwise operations
        // to thoroughly mix the bits of the input coordinates.
        int h = x * 374761393 + y * 668265263 + z * 1619428543;
        h = (h ^ (h >> 13)) * 1274126177;
        h = h ^ (h >> 16);

        // Convert the final integer hash to a float in the desired range.
        // 2147483647.0f is Integer.MAX_VALUE as a float.
        return (h / 2147483647.0f) * 2.0f - 1.0f;
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
        return visitor.apply(new DomainWarpDensity(
                input.mapAll(visitor),
                scale
        ));
    }
}
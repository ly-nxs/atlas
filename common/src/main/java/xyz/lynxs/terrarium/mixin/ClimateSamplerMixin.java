package xyz.lynxs.terrarium.mixin;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.lynxs.terrarium.gen.dfs.HumidityDensity;
import xyz.lynxs.terrarium.gen.dfs.WeirdnessDensity;

import java.util.List;

@Mixin(Climate.Sampler.class)
public abstract class ClimateSamplerMixin {
    @Mutable
    @Shadow
    @Final
    private DensityFunction weirdness;
    @Mutable
    @Shadow
    @Final
    private DensityFunction humidity;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void replaceClimate(
            DensityFunction temperature,
            DensityFunction humidity,
            DensityFunction continentalness,
            DensityFunction erosion,
            DensityFunction depth,
            DensityFunction weirdness, // Original parameter
            List<Climate.ParameterPoint> spawnTarget,
            CallbackInfo ci
    ) {
        // Replace the field value after construction
        this.weirdness = new WeirdnessDensity(5);
        this.humidity = new HumidityDensity(humidity);
    }
}
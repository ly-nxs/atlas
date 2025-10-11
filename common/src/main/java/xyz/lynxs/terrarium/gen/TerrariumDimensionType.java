package xyz.lynxs.terrarium.gen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.Optional;
import java.util.function.Supplier;

import static xyz.lynxs.terrarium.Terrarium.CONFIG;
import static xyz.lynxs.terrarium.Terrarium.id;

/**
 * Utility class to dynamically create and register the DimensionType,
 * ensuring its height matches the configurable CONFIG.worldHeight.
 */
public class TerrariumDimensionType {

    public static final ResourceKey<DimensionType> TERRARIUM_DIMENSION_TYPE_KEY =
            ResourceKey.create(Registries.DIMENSION_TYPE, id("overworld"));


    /**
     * Creates a DimensionType instance dynamically based on the current config.
     */
    public static DimensionType create() {
        final int NOISE_HEIGHT = CONFIG.worldHeight;
        return new DimensionType(
            null,
                true,
                false,
                false,
                true,
                1.0,
                true,
                false,
                -64,
                NOISE_HEIGHT,
                NOISE_HEIGHT - 64,
                BlockTags.INFINIBURN_OVERWORLD,
                BuiltinDimensionTypes.OVERWORLD_EFFECTS,
                0.0F,
                Optional.of(NOISE_HEIGHT / 2),
                new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0)
        );
    }
    public static Supplier<DimensionType> getSupplier() {
        return TerrariumDimensionType::create;
    }
}

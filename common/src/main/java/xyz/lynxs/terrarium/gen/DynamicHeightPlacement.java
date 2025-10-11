package xyz.lynxs.terrarium.gen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

import static xyz.lynxs.terrarium.Terrarium.CONFIG;

// 1. Create a CUSTOM placement modifier type
public class DynamicHeightPlacement extends PlacementModifier {
    private final net.minecraft.world.level.levelgen.heightproviders.HeightProvider height;

    public static final MapCodec<DynamicHeightPlacement> CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(net.minecraft.world.level.levelgen.heightproviders.HeightProvider.CODEC.fieldOf("height").forGetter((heightRangePlacement) -> heightRangePlacement.height)).apply(instance, DynamicHeightPlacement::new));
    public DynamicHeightPlacement(net.minecraft.world.level.levelgen.heightproviders.HeightProvider height) {
        this.height = height;
    }
    public static DynamicHeightPlacement of(HeightProvider heightProvider) {
        return new DynamicHeightPlacement(heightProvider);
    }
    public static DynamicHeightPlacement uniform(VerticalAnchor verticalAnchor, VerticalAnchor verticalAnchor2) {
        return of(UniformHeight.of(verticalAnchor, verticalAnchor2));
    }

    public static DynamicHeightPlacement triangle(VerticalAnchor verticalAnchor, VerticalAnchor verticalAnchor2) {
        return of(TrapezoidHeight.of(verticalAnchor, verticalAnchor2));
    }

    @Override
    public @NotNull Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        // Add your dynamic logic here
        int y = this.height.sample(random, context);
        y = y <= 64 ? y : (y / (CONFIG.worldHeight + CONFIG.startingY)) * 320;
        return Stream.of(pos.atY(y));
    }

    @Override
    public @NotNull PlacementModifierType<?> type() {
        return PlacementModifierType.HEIGHT_RANGE; // Your custom type
    }

}

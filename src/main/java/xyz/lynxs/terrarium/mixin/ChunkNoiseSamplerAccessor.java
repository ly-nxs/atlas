package xyz.lynxs.terrarium.mixin;


import net.minecraft.block.BlockState;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkNoiseSampler.class)
public interface ChunkNoiseSamplerAccessor {
    @Invoker("getHorizontalCellBlockCount")
    int invokeCellWidth();
    @Invoker("getVerticalCellBlockCount")
    int invokeCellHeight();
    @Invoker("sampleBlockState")
    BlockState invokeGetInterpolatedState();
    @Accessor("beardifying")
    DensityFunctionTypes.Beardifying accessBeardifier();
    @Invoker("getActualDensityFunction")
    DensityFunction invokeWrap(DensityFunction densityFunction);
}
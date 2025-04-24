package xyz.lynxs.terrarium.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import xyz.lynxs.terrarium.Terrarium;


public class TerrariumRegistries {

    public static void register() {
        Registry.register(Registries.MATERIAL_CONDITION, Terrarium.id("terrarium"), AboveSurfaceMaterialCondition.CODEC.codec());
    }

    /**
     * reimplementation of the above_preliminary_surface rule that reads the terrarium.
     * @param depth how far below the surface the rule should extend
     */
    record AboveSurfaceMaterialCondition(int depth) implements MaterialRules.MaterialCondition {
        static final CodecHolder<TerrariumRegistries.AboveSurfaceMaterialCondition> CODEC = CodecHolder.of(
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                                Codec.INT.optionalFieldOf("depth", 5).forGetter(AboveSurfaceMaterialCondition::depth))
                        .apply(instance, TerrariumRegistries.AboveSurfaceMaterialCondition::new)));

        @Override
        public CodecHolder<? extends MaterialRules.MaterialCondition> codec() {
            return CODEC;
        }

        @Override
        public MaterialRules.BooleanSupplier apply(final MaterialRules.MaterialRuleContext materialRuleContext) {
            class AboveSurfacePredicate
                    extends MaterialRules.FullLazyAbstractPredicate {
                AboveSurfacePredicate() {
                    super(materialRuleContext);
                }

                @Override
                protected boolean test() {

                    return this.context.blockY > materialRuleContext.estimateSurfaceHeight() - AboveSurfaceMaterialCondition.this.depth;
                }
            }
            return new AboveSurfacePredicate();
        }
    }
}
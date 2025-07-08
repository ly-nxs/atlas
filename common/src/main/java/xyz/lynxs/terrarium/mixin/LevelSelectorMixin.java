
package xyz.lynxs.terrarium.mixin;

import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.core.registries.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.lynxs.terrarium.Terrarium;
import xyz.lynxs.terrarium.gui.TerrariumCustomizeScreen;
import net.minecraft.resources.ResourceKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


// why the fuck does mojang hardcode everything??

@Mixin(PresetEditor.class)
interface LevelSelectorMixin {

    // there has to be a better way to do this right?
    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;"
            ),
            remap = false
    )
    private static Map<Object, Object> of(Object k1, Object v1, Object k2, Object v2) {
        Map<Object, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(Optional.of(ResourceKey.create(Registries.WORLD_PRESET, Terrarium.id("terrarium"))), (PresetEditor) (screen, ctx) -> new TerrariumCustomizeScreen(screen));
        return map;
    }
}
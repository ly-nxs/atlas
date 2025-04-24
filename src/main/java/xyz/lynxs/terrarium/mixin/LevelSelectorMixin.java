
package xyz.lynxs.terrarium.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.gui.screen.world.LevelScreenProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.lynxs.terrarium.Terrarium;
import xyz.lynxs.terrarium.gui.terrariumCustomizeScreen;


// why the fuck does mojang hardcode everything??

@Mixin(LevelScreenProvider.class)
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
        map.put(Optional.of(RegistryKey.of(RegistryKeys.WORLD_PRESET, Terrarium.id("terrarium"))), (LevelScreenProvider) (screen, ctx) -> new terrariumCustomizeScreen(screen));
        return map;
    }
}
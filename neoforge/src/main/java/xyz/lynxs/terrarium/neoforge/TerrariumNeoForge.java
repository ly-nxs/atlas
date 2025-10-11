package xyz.lynxs.terrarium.neoforge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import xyz.lynxs.terrarium.Terrarium;
import net.neoforged.fml.common.Mod;
import xyz.lynxs.terrarium.gen.TerrariumDimensionType;
import xyz.lynxs.terrarium.gen.TerrariumRegistries;

@Mod(Terrarium.MOD_ID)
public final class TerrariumNeoForge {
    public TerrariumNeoForge(IEventBus modBus) {
        // Run our common setup.
        Terrarium.init();
        modBus.addListener(this::register);

    }
    @SubscribeEvent
    public void register(RegisterEvent event) {
        if (event.getRegistryKey().equals(BuiltInRegistries.DENSITY_FUNCTION_TYPE.key())) {
            TerrariumRegistries.register();
        }
        if (event.getRegistryKey().equals(Registries.DIMENSION_TYPE.registryKey())) {
            event.register(
                    Registries.DIMENSION_TYPE,
                    Terrarium.id("terrarium"),
                    TerrariumDimensionType.getSupplier() // Calls the common method
            );
        }
    }
}

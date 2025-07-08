package xyz.lynxs.terrarium.neoforge;

import xyz.lynxs.terrarium.Terrarium;
import net.neoforged.fml.common.Mod;

@Mod(Terrarium.MOD_ID)
public final class TerrariumNeoForge {
    public TerrariumNeoForge() {
        // Run our common setup.
        Terrarium.init();
    }
}

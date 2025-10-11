package xyz.lynxs.terrarium;

import com.mojang.brigadier.CommandDispatcher;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.lynxs.terrarium.gen.BiomeProvider;
import xyz.lynxs.terrarium.gen.HeightProvider;
import xyz.lynxs.terrarium.gen.TerrariumDimensionType;
import xyz.lynxs.terrarium.preset.PresetConfig;

import java.nio.file.Path;

import static xyz.lynxs.terrarium.TerrariumConfig.load;

public final class Terrarium {
    public static final String MOD_ID = "terrarium";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static PresetConfig CONFIG = new PresetConfig();
    public  static TerrariumConfig CONFIG1 = ConfigManager.register(TerrariumConfig.class, Path.of("terrarium.json"), newConfig -> CONFIG1 = newConfig);
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
    // Server-side world load
    public static void onServerWorldLoad(MinecraftServer server) {
        CONFIG = load(CONFIG.getClass(), server.getWorldPath(LevelResource.ROOT).resolve("terrarium.json"), false);
        LOGGER.info("Terrarium World Loaded!");
    }
    public static void init() {

        CommandRegistrationEvent.EVENT.register(((commandDispatcher, commandBuildContext, commandSelection) -> register(commandDispatcher)));
        LOGGER.info("Terrarium mod initialized");
        LifecycleEvent.SERVER_LEVEL_LOAD.register(k -> onServerWorldLoad(k.getServer()));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("ecoregion")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            if (source.getEntity() == null) {
                                source.sendFailure(Component.literal("This command can only be used by a player"));
                                return 0;
                            }

                            BlockPos pos = source.getEntity().blockPosition();
                            String region = String.valueOf(BiomeProvider.getClimate(pos.getX(), pos.getZ()));
                            source.sendSuccess(
                                    () -> Component.literal("Current Region: " + region),
                                    false
                            );
                            return 1;
                        })
        );
        dispatcher.register(Commands.literal("geotp")
                .requires(source -> source.hasPermission(2)) // Require OP level 2
                .then(Commands.argument("longitude", DoubleArgumentType.doubleArg(-180, 180))
                        .then(Commands.argument("latitude", DoubleArgumentType.doubleArg(-90, 90))
                                .executes(context -> teleportToCoordinates(
                                        context.getSource(),
                                        DoubleArgumentType.getDouble(context, "longitude"),
                                        DoubleArgumentType.getDouble(context, "latitude")
                                ))
                        )
                        .executes(context -> {
                            context.getSource().sendFailure(Component.literal("Missing latitude argument. Usage: /geotp <longitude> <latitude>"));
                            return 0;
                        })
                )
                .executes(context -> {
                    context.getSource().sendFailure(Component.literal("Usage: /geotp <longitude> <latitude> - Teleports to geographic coordinates"));
                    return 0;
                })
        );
    }
    private static int teleportToCoordinates(CommandSourceStack source, double longitude, double latitude) {
        if (source == null || !(source.getEntity() instanceof Player player)) {
            if (source != null) {
                source.sendFailure(Component.literal("Only players can use this command!"));
            }
            return 0;
        }

        try {
            // Convert lat/lon to Minecraft coordinates
            int x = Util.lonToX(longitude, (int) (Math.pow(2, CONFIG.zoom) * 256)) - CONFIG.adjustXoffset;
            int z = Util.latToZ(latitude, (int) (Math.pow(2, CONFIG.zoom) * 256)) - CONFIG.adjustZoffset;

            // Find surface height
            int y = HeightProvider.getElevation(x, z) + CONFIG.startingY;
            if (y < source.getLevel().getMinY()) {
                source.sendFailure(Component.literal("Could not find a safe location to teleport to!"));
                return 0;
            }
            y += 65; // Teleport to block above surface


            // Teleport player
            player.teleportTo(x + 0.5, y, z + 0.5);

            // Send confirmation
            int finalY = y;
            source.sendSuccess(() -> Component.literal(String.format(
                    "Teleported to %.6f° longitude, %.6f° latitude (Minecraft coordinates: %d, %d, %d)",
                    longitude, latitude, x, finalY, z
            )), false);

            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred during teleportation: " + e.getMessage()));
            return 0;
        }
    }

}


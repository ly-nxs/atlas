package xyz.lynxs.terrarium;

import com.mojang.brigadier.CommandDispatcher;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.lynxs.terrarium.gen.BiomeProvider;
import xyz.lynxs.terrarium.gen.HeightProvider;
import xyz.lynxs.terrarium.gen.TerrariumRegistries;
import xyz.lynxs.terrarium.preset.PresetConfig;

import java.io.IOException;
import java.nio.file.Path;

public final class Terrarium {
    public static final String MOD_ID = "terrarium";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static PresetConfig CONFIG = new PresetConfig();
    public  static TerrariumConfig CONFIG1 = ConfigManager.register(TerrariumConfig.class, Path.of("terrarium.json"), newConfig -> CONFIG1 = newConfig);
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void init() {
        BiomeProvider.init();
        TerrariumRegistries.register();
        CommandRegistrationEvent.EVENT.register(((commandDispatcher, commandBuildContext, commandSelection) -> register(commandDispatcher)));
        LOGGER.info("Terrarium mod initialized");
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
                .then(Commands.argument("longitude", DoubleArgumentType.doubleArg(-180, 180))
                        .then(Commands.argument("latitude", DoubleArgumentType.doubleArg(-90, 90))
                                .executes(context -> {
                                    try {
                                        return teleportToCoordinates(
                                                context.getSource(),
                                                DoubleArgumentType.getDouble(context, "longitude"),
                                                DoubleArgumentType.getDouble(context, "latitude")
                                        );
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                        )
                        .executes(context -> {
                            context.getSource().sendFailure(Component.literal("Usage: /geotp <longitude> <latitude>"));
                            return 0;
                        })));
    }
    private static int teleportToCoordinates(CommandSourceStack source, double longitude, double latitude) throws IOException {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("Only players can use this command!"));
            return 0;
        }

        // Convert lat/lon to Minecraft coordinates
        int x = Util.lonToX(longitude, (int) (Math.pow(2, CONFIG.zoom) * 256));
        int z = Util.latToZ(latitude, (int) (Math.pow(2, CONFIG.zoom) * 256));

        // Find surface height
        int y = HeightProvider.getElevation(x, z) + 1;

        // Teleport player
        player.teleportTo(x, y, z);

        // Send confirmation
        source.sendSuccess(() -> Component.literal(String.format(
                "Teleported to %.6f° longitude, %.6f° latitude (Minecraft coordinates: %d, %d, %d)",
                longitude, latitude, x, y, z
        )), false);

        return 1;
    }

}


package xyz.lynxs.terrarium.world.gen.biome;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import java.awt.*;
import java.io.InputStreamReader;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static xyz.lynxs.terrarium.Terrarium.LOGGER;

public class BiomeColorRegistry {
    // Record for individual biome color mappings
    public record BiomeColorMapping(
            Identifier tagId,
            int color,
            Optional<String> comment
    ) {
        public static final Codec<BiomeColorMapping> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Identifier.CODEC.fieldOf("tag").forGetter(BiomeColorMapping::tagId),
                        Codec.STRING.fieldOf("color").xmap(
                                BiomeColorRegistry::parseHexColor,
                                color -> String.format("#%06X", color)
                        ).forGetter(BiomeColorMapping::color),
                        Codec.STRING.optionalFieldOf("comment").forGetter(BiomeColorMapping::comment)
                ).apply(instance, BiomeColorMapping::new)
        );

        public TagKey<Biome> getBiomeTag() {
            return TagKey.of(RegistryKeys.BIOME, tagId);
        }
    }
    private static int parseHexColor(String hex) {
        return Color.decode(hex).getRGB(); // Force positive
    }

    // Main registry record
    public record BiomeColorConfig(
            List<BiomeColorMapping> mappings,
            Optional<String> defaultBiome
    ) {
        public static final Codec<BiomeColorConfig> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        BiomeColorMapping.CODEC.listOf().fieldOf("mappings").forGetter(BiomeColorConfig::mappings),
                        Codec.STRING.optionalFieldOf("default_biome").forGetter(BiomeColorConfig::defaultBiome)
                ).apply(instance, BiomeColorConfig::new)
        );
    }

    // Runtime storage
    private static final Map<TagKey<Biome>, BiomeColorMapping> TAG_TO_MAPPING = new ConcurrentHashMap<>();
    private static final Map<Integer, TagKey<Biome>> COLOR_TO_TAG = new ConcurrentHashMap<>();
    private static MinecraftServer biomeRegistry;
    private static Identifier defaultBiomeId = BiomeKeys.PLAINS.getValue();

    public static void load(ResourceManager manager, MinecraftServer world) {
        TAG_TO_MAPPING.clear();
        COLOR_TO_TAG.clear();
        biomeRegistry = world;
        manager.findResources("worldgen/biome_colors", id -> id.getPath().endsWith("json"))
                .forEach((id, resource) -> {
                    try (var input = resource.getInputStream()) {
                        var json = BiomeColorConfig.CODEC.decode(JsonOps.INSTANCE, JsonParser.parseReader(new InputStreamReader(input)));
                        var config = json.getOrThrow(true, System.out::println).getFirst();

                        // Store mappings
                        for (var mapping : config.mappings()) {
                            TAG_TO_MAPPING.put(mapping.getBiomeTag(), mapping);
                            COLOR_TO_TAG.put(mapping.color(), mapping.getBiomeTag());
                        }

                        // Set default biome if specified
                        config.defaultBiome().ifPresent(defaultId ->
                                defaultBiomeId = Identifier.of("minecraft",defaultId));
                    } catch (Exception e) {
                        LOGGER.error("Failed to load biome color config: " + id, e);
                    }
                });
    }

    public static BiomeColorMapping getMappingForTag(TagKey<Biome> tag) {
        return Optional.of(TAG_TO_MAPPING.get(tag)).orElse(null);
    }

    public static TagKey<Biome> getTagForColor(int rgb) {
        return COLOR_TO_TAG.getOrDefault(rgb, BiomeTags.IS_HILL);
    }

    public static RegistryEntryList<Biome> getBiomesForTag(TagKey<Biome> tag) {
        return biomeRegistry.getCombinedDynamicRegistries().getCombinedRegistryManager().createRegistryLookup().getOrThrow(RegistryKeys.BIOME).getOrThrow(tag);
    }

    public static RegistryEntry<Biome> getDefaultBiome() {
        return biomeRegistry.getCombinedDynamicRegistries().getCombinedRegistryManager().createRegistryLookup().getOrThrow(RegistryKeys.BIOME).getOrThrow(BiomeKeys.PLAINS);
    }

    public static Collection<TagKey<Biome>> getAllColorTags() {
        return Collections.unmodifiableSet(TAG_TO_MAPPING.keySet());
    }
}
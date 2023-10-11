package hunternif.mc.impl.atlas.client;

import dev.architectury.injectables.annotations.ExpectPlatform;
import hunternif.mc.impl.atlas.AntiqueAtlasMod;
import hunternif.mc.impl.atlas.client.texture.ITexture;
import hunternif.mc.impl.atlas.core.scaning.TileHeightType;
import hunternif.mc.impl.atlas.util.Log;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.feature.PlacedFeature;


import java.util.*;
import java.util.Map.Entry;

import static net.minecraft.world.biome.BiomeKeys.*;

/**
 * Maps biome IDs (or pseudo IDs) to textures. <i>Not thread-safe!</i>
 * <p>If several textures are set for one ID, one will be chosen at random when
 * putting tile into Atlas.</p>
 *
 * @author Hunternif
 */
@Environment(EnvType.CLIENT)
public class TileTextureMap {
    private static final TileTextureMap INSTANCE = new TileTextureMap();

    public static final Identifier DEFAULT_TEXTURE = AntiqueAtlasMod.id("test");

    public static TileTextureMap instance() {
        return INSTANCE;
    }

    /**
     * This map stores the pseudo biome texture mappings, any biome with ID <0 is assumed to be a pseudo biome
     */
    private final Map<Identifier, TextureSet> textureMap = new HashMap<>();

    /**
     * Assign texture set to pseudo biome
     */
    public void setTexture(Identifier tileId, TextureSet textureSet) {
        if (tileId == null) return;

        if (textureSet == null) {
            if (textureMap.remove(tileId) != null) {
                Log.warn("Removing old texture for %d", tileId);
            }
            return;
        }

        textureMap.put(tileId, textureSet);
    }

    /**
     * Assign the same texture set to all height variations of the tileId
     */
    public void setAllTextures(Identifier tileId, TextureSet textureSet) {
        setTexture(tileId, textureSet);

        for (TileHeightType layer : TileHeightType.values()) {
            setTexture(Identifier.tryParse(tileId + "_" + layer), textureSet);
        }
    }

    public TextureSet getDefaultTexture() {
        return TextureSetMap.instance().getByName(DEFAULT_TEXTURE);
    }

    /**
     * Find the most appropriate standard texture set depending on
     * BiomeDictionary types.
     */
    public void autoRegister(Identifier id, RegistryKey<Biome> biome) {
        if (biome == null || id == null) {
            Log.error("Given biome is null. Cannot autodetect a suitable texture set for that.");
            return;
        }

        Optional<Identifier> texture_set = guessFittingTextureSet(biome);

        if (texture_set.isPresent()) {
            setAllTextures(id, TextureSetMap.instance().getByName(texture_set.get()));
            Log.info("Auto-registered standard texture set for biome %s: %s", id, texture_set.get());
        } else {
            Log.error("Failed to auto-register a standard texture set for the biome '%s'. This is most likely caused by errors in the TextureSet configurations, check your resource packs first before reporting it as an issue!", id.toString());
            setAllTextures(id, getDefaultTexture());
        }
    }

    @ExpectPlatform
    static private Optional<Identifier> guessFittingTextureSet(RegistryKey<Biome> biome) {
        throw new AssertionError("Not implemented");
    }

    //TODO: This is not implemented, because I do not want to do this.
    static public Optional<Identifier> guessFittingTextureSetFallback(RegistryEntry.Reference<Biome> biome) {
        AntiqueAtlasMod.LOG.error("FALLBACK WAS CALLED");
        Identifier texture_set = switch (biome.getType().toString()) {
            case "SWAMP" -> AntiqueAtlasMod.id("swamp");
            case "OCEAN", "RIVER" ->
                    biome.value().getTemperature() < 0.15f ? AntiqueAtlasMod.id("ice") : AntiqueAtlasMod.id("water");
            case "BEACH" -> AntiqueAtlasMod.id("shore");
            case "JUNGLE" -> AntiqueAtlasMod.id("jungle");
            case "SAVANNA" -> AntiqueAtlasMod.id("savanna");
            case "BADLANDS", "ERODED_BADLANDS", "WOODED_BADLANDS" -> AntiqueAtlasMod.id("plateau_mesa");
            case "FOREST" ->
                    biome.value().getTemperature() < 0.15f ? AntiqueAtlasMod.id("snow_pines") : AntiqueAtlasMod.id("forest");
            case "PLAINS" ->
                    biome.value().getTemperature() < 0.15f ? AntiqueAtlasMod.id("snow") : AntiqueAtlasMod.id("plains");
            case "ICE_SPIKES" -> AntiqueAtlasMod.id("ice_spikes");
            case "DESERT" -> AntiqueAtlasMod.id("desert");
            case "TAIGA" -> AntiqueAtlasMod.id("snow");
            case "WINDSWEPT_GRAVELLY_HILLS", "WINDSWEPT_HILLS" -> AntiqueAtlasMod.id("hills");
            case "CHERRY_GROVE", "MEADOW", "GROVE", "SNOWY_SLOPES", "JAGGED_PEAKS", "FROZEN_PEAKS", "STONY_PEAKS" -> AntiqueAtlasMod.id("mountains");
            case "THE_END" -> {
                List<RegistryEntryList<PlacedFeature>> features = biome.value().getGenerationSettings().getFeatures();
                PlacedFeature chorus_plant_feature = ((Registry<PlacedFeature>)RegistryKeys.PLACED_FEATURE.getValue()).get(new Identifier("chorus_plant"));
                assert chorus_plant_feature != null;
                boolean has_chorus_plant = features.stream().anyMatch(entries -> entries.stream().anyMatch(feature -> feature.value() == chorus_plant_feature));
                if (has_chorus_plant) {
                    yield AntiqueAtlasMod.id("end_island_plants");
                } else {
                    yield AntiqueAtlasMod.id("end_island");
                }
            }
            case "MUSHROOM_FIELDS" -> AntiqueAtlasMod.id("mushroom");
            case "SOUL_SAND_VALLEY" -> AntiqueAtlasMod.id("soul_sand_valley");
            case "THE_VOID" -> AntiqueAtlasMod.id("end_void");
            //case BiomeKeys.UNDERGROUND -> {
            //    Log.warn("Underground biomes aren't supported yet.");
            //    yield null;
            default -> biome.value().getTemperature() < 0.15f ? AntiqueAtlasMod.id("snow_pines") : AntiqueAtlasMod.id("forest");
        };

        return Optional.ofNullable(null);
    }

    public boolean isRegistered(Identifier id) {
        return textureMap.containsKey(id);
    }

    /**
     * If unknown biome, auto-registers a texture set. If null, returns default set.
     */
    public TextureSet getTextureSet(Identifier tile) {
        if (tile == null) {
            return getDefaultTexture();
        }

        return textureMap.getOrDefault(tile, getDefaultTexture());
    }

    public ITexture getTexture(SubTile subTile) {
        return getTextureSet(subTile.tile).getTexture(subTile.variationNumber);
    }

    public List<Identifier> getAllTextures() {
        List<Identifier> list = new ArrayList<>();

        for (Entry<Identifier, TextureSet> entry : textureMap.entrySet()) {
            Arrays.stream(entry.getValue().textures).forEach(iTexture -> list.add(iTexture.getTexture()));
        }

        return list;
    }
}

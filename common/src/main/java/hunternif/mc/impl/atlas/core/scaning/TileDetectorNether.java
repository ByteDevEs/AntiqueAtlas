package hunternif.mc.impl.atlas.core.scaning;

import hunternif.mc.impl.atlas.core.TileIdMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Detects seas of lava, cave ground and cave walls in the Nether.
 *
 * @author Hunternif
 */
public class TileDetectorNether extends TileDetectorBase implements ITileDetector {
    /**
     * The Nether will be checked for air/ground at this level.
     */
    private static final int airProbeLevel = 50;
    /**
     * The Nether will be checked for lava at this level.
     */
    private static final int lavaSeaLevel = 31;

    /**
     * Increment the counter for lava biomes by this much during iteration.
     * This is done so that rivers are more likely to be connected.
     */
    private static final int priorityLava = 1;

    @Override
    public Identifier getBiomeID(World world, Chunk chunk) {
        var BIOME_REGISTRY = world.getRegistryManager().get(RegistryKeys.BIOME);
        Map<Identifier, Integer> biomeOccurrences = new HashMap<>(BIOME_REGISTRY.getIds().size());

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                RegistryEntry<Biome> registryEntryBiome = chunk.getBiomeForNoiseGen(x, lavaSeaLevel, z);
                Biome biome = registryEntryBiome.value();
                if (registryEntryBiome.isIn(BiomeTags.IS_NETHER)) {
                    // The Nether!
                    Block seaLevelBlock = chunk.getBlockState(new BlockPos(x, lavaSeaLevel, z)).getBlock();
                    if (seaLevelBlock == Blocks.LAVA) {
                        updateOccurrencesMap(biomeOccurrences, TileIdMap.TILE_LAVA, priorityLava);
                    } else {
                        BlockState airProbeBlock = chunk.getBlockState(new BlockPos(x, airProbeLevel, z));
                        if (airProbeBlock.isAir()) {
                            updateOccurrencesMap(biomeOccurrences, TileIdMap.TILE_LAVA_SHORE, 1);
                        } else {
                            // cave walls
                            updateOccurrencesMap(biomeOccurrences, getBiomeIdentifier(world,biome), 2);
                        }
                    }
                } else {
                    // In case there are custom biomes "modded in":
                    updateOccurrencesMap(biomeOccurrences, getBiomeIdentifier(world,biome), priorityForBiome(registryEntryBiome));
                }
            }
        }

        if (biomeOccurrences.isEmpty()) return null;

        Map.Entry<Identifier, Integer> meanBiome = Collections.max(biomeOccurrences.entrySet(), Comparator
                .comparingInt(Map.Entry::getValue));

        return meanBiome.getKey();
    }

    @Override
    public int getScanRadius() {
        return Math.min(super.getScanRadius(), 6);
    }
}

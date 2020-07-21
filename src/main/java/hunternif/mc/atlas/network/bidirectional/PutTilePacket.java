package hunternif.mc.atlas.network.bidirectional;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.api.AtlasAPI;
import hunternif.mc.atlas.core.AtlasData;
import hunternif.mc.atlas.core.TileKind;
import hunternif.mc.atlas.core.TileKindFactory;
import hunternif.mc.atlas.network.AbstractMessage;
import hunternif.mc.atlas.util.Log;
import net.fabricmc.api.EnvType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.dimension.DimensionType;

/**
 * Puts biome tile into one atlas. When sent to server, forwards it to every
 * client that has this atlas' data synced.
 * @author Hunternif
 */
public class PutTilePacket extends AbstractMessage<PutTilePacket> {
	private int atlasID, x, z;
	private RegistryKey<DimensionType> dimension;
	private TileKind kind;

	public PutTilePacket() {}

	public PutTilePacket(int atlasID, RegistryKey<DimensionType> dimension, int x, int z, TileKind kind) {
		this.atlasID = atlasID;
		this.dimension = dimension;
		this.x = x;
		this.z = z;
		this.kind = kind;
	}
	
	@Override
	protected void read(PacketByteBuf buffer) {
		atlasID = buffer.readVarInt();
		dimension = RegistryKey.of(Registry.DIMENSION_TYPE_KEY, buffer.readIdentifier());
		x = buffer.readVarInt();
		z = buffer.readVarInt();
		kind = TileKindFactory.get(buffer.readVarInt());
	}

	@Override
	protected void write(PacketByteBuf buffer) {
		buffer.writeVarInt(atlasID);
		buffer.writeIdentifier(dimension.getValue());
		buffer.writeVarInt(x);
		buffer.writeVarInt(z);
		buffer.writeVarInt(kind.getId());
	}

	@Override
	protected void process(PlayerEntity player, EnvType side) {
		if (side == EnvType.SERVER) {
			// Make sure it's this player's atlas :^)
			if (AntiqueAtlasMod.CONFIG.gameplay.itemNeeded && !AtlasAPI.getPlayerAtlases(player).contains(atlasID)) {
				Log.warn("Player %s attempted to modify someone else's Atlas #%d",
						player.getCommandSource().getName(), atlasID);
				return;
			}
			if (kind.getId() >= 0) {
				AtlasAPI.tiles.putBiomeTile(player.getEntityWorld(), atlasID, kind.getBiome(), x, z);
			} else {
				AtlasAPI.tiles.putCustomTile(player.getEntityWorld(), atlasID, kind.getExtTile(), x, z);
			}
		} else {
			AtlasData data = AntiqueAtlasMod.atlasData.getAtlasData(atlasID, player.getEntityWorld());
			data.setTile(dimension, x, z, kind);
		}
	}

}

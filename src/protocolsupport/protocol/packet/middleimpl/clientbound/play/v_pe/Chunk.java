package protocolsupport.protocol.packet.middleimpl.clientbound.play.v_pe;

import org.bukkit.Bukkit;
import protocolsupport.api.ProtocolVersion;
import protocolsupport.listeners.InternalPluginMessageRequest;
import protocolsupport.listeners.internal.ChunkUpdateRequest;
import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.middle.clientbound.play.MiddleChunk;
import protocolsupport.protocol.packet.middleimpl.ClientBoundPacketData;
import protocolsupport.protocol.serializer.ArraySerializer;
import protocolsupport.protocol.serializer.ItemStackSerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.protocol.storage.netcache.MovementCache;
import protocolsupport.protocol.serializer.PositionSerializer;
import protocolsupport.protocol.typeremapper.basic.TileEntityRemapper;
import protocolsupport.protocol.typeremapper.block.LegacyBlockData;
import protocolsupport.protocol.typeremapper.chunk.ChunkTransformerBB;
import protocolsupport.protocol.typeremapper.chunk.ChunkTransformerPE;
import protocolsupport.protocol.typeremapper.chunk.EmptyChunk;
import protocolsupport.protocol.typeremapper.pe.PEDataValues;
import protocolsupport.protocol.typeremapper.pe.PEPacketIDs;
import protocolsupport.utils.recyclable.RecyclableArrayList;
import protocolsupport.protocol.utils.types.ChunkCoord;
import protocolsupport.protocol.utils.types.TileEntity;
import protocolsupport.utils.recyclable.RecyclableCollection;
import protocolsupport.utils.recyclable.RecyclableEmptyList;

public class Chunk extends MiddleChunk {

	public Chunk(ConnectionImpl connection) {
		super(connection);
	}

	private final ChunkTransformerBB transformer = new ChunkTransformerPE(
		LegacyBlockData.REGISTRY.getTable(connection.getVersion()),
		TileEntityRemapper.getRemapper(connection.getVersion()),
		connection.getCache().getTileCache(),
		PEDataValues.BIOME.getTable(connection.getVersion())
	);

	@Override
	public RecyclableCollection<ClientBoundPacketData> toData() {
		if (full || (bitmask == 0xFFFF)) { //Only send full or 'full' chunks to PE.
			MovementCache movecache = cache.getMovementCache();
			RecyclableArrayList<ClientBoundPacketData> packets = RecyclableArrayList.create();
			ProtocolVersion version = connection.getVersion();
			cache.getPEChunkMapCache().markSent(chunk);
			transformer.loadData(chunk, data, bitmask, cache.getAttributesCache().hasSkyLightInCurrentDimension(), full, tiles);
			ClientBoundPacketData serializer = ClientBoundPacketData.create(PEPacketIDs.CHUNK_DATA);
			PositionSerializer.writePEChunkCoord(serializer, chunk);
			ArraySerializer.writeVarIntByteArray(serializer, chunkdata -> {
				transformer.writeLegacyData(chunkdata);
				chunkdata.writeByte(0); //borders
				for (TileEntity tile : transformer.remapAndGetTiles()) {
//					System.out.println("LOADING CHUNK TILE: " + tile.toString());
					ItemStackSerializer.writeTag(chunkdata, true, version, tile.getNBT());
				}
			});
			packets.add(serializer);
			if (version.isAfterOrEq(ProtocolVersion.MINECRAFT_PE_1_8)) {
				ClientBoundPacketData networkChunkUpdate = ClientBoundPacketData.create(PEPacketIDs.NETWORK_CHUNK_PUBLISHER_UPDATE_PACKET);
				VarNumberSerializer.writeSVarInt(networkChunkUpdate, (int) movecache.getPEClientX());
				VarNumberSerializer.writeVarInt(networkChunkUpdate, (int) movecache.getPEClientY());
				VarNumberSerializer.writeSVarInt(networkChunkUpdate, (int) movecache.getPEClientZ());
				VarNumberSerializer.writeVarInt(networkChunkUpdate, Bukkit.getViewDistance() << 4); //TODO: client view distance yo
				packets.add(networkChunkUpdate);
			}
			return packets;
		} else { //Request a full chunk.
			InternalPluginMessageRequest.receivePluginMessageRequest(connection, new ChunkUpdateRequest(chunk));
			return RecyclableEmptyList.get();
		}
	}

	public static ClientBoundPacketData createEmptyChunk(ProtocolVersion version, ChunkCoord chunk) {
		ClientBoundPacketData serializer = ClientBoundPacketData.create(PEPacketIDs.CHUNK_DATA);
		PositionSerializer.writePEChunkCoord(serializer, chunk);
		serializer.writeBytes(EmptyChunk.getPEChunkData());
		return serializer;
	}

}
package protocolsupport.protocol.packet.middleimpl.clientbound.play.v_pe;

import protocolsupport.api.ProtocolVersion;
import protocolsupport.protocol.packet.middle.clientbound.play.MiddleEntityEquipment;
import protocolsupport.protocol.packet.middleimpl.ClientBoundPacketData;
import protocolsupport.protocol.serializer.ItemStackSerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.protocol.typeremapper.pe.PEPacketIDs;
import protocolsupport.protocol.utils.types.NetworkEntity;
import protocolsupport.protocol.utils.types.NetworkEntity.DataCache;
import protocolsupport.protocol.utils.types.NetworkEntityType;
import protocolsupport.utils.recyclable.RecyclableCollection;
import protocolsupport.utils.recyclable.RecyclableEmptyList;
import protocolsupport.utils.recyclable.RecyclableSingletonList;
import protocolsupport.zplatform.itemstack.ItemStackWrapper;

public class EntityEquipment extends MiddleEntityEquipment {

	@Override
	public RecyclableCollection<ClientBoundPacketData> toData() {
		System.out.println("dddd");
		ProtocolVersion version = connection.getVersion();
		String locale = cache.getAttributesCache().getLocale();
		NetworkEntity entity = cache.getWatchedEntityCache().getWatchedEntity(entityId);
		if (entity == null) {
			return RecyclableEmptyList.get();
		}
		if (entity.isOfType(NetworkEntityType.BASE_HORSE)) {
			System.out.println("HORSEY ARMOUR: slot - " + slot);
		}
		DataCache dataCache = entity.getDataCache();
		if (slot > 1) {
			// Armor update
			switch (slot) {
				case (2): {
					dataCache.setBoots(itemstack);
					break;
				}
				case (3): {
					dataCache.setLeggings(itemstack);
					break;
				}
				case (4): {
					dataCache.setChestplate(itemstack);
					break;
				}
				case (5): {
					dataCache.setHelmet(itemstack);
					break;
				}
			}
			return RecyclableSingletonList.create(create(version, locale, entityId, dataCache.getHelmet(), dataCache.getChestplate(), dataCache.getLeggings(), dataCache.getBoots()));
		}
		if (slot == 1) {
			dataCache.setHand(itemstack);
		} else {
			dataCache.setOffHand(itemstack);
		}
		return RecyclableSingletonList.create(createUpdateHand(version, locale, entityId, itemstack, 0, slot == 1));
	}

	public static ClientBoundPacketData create(ProtocolVersion version, String locale, long entityId, ItemStackWrapper helmet, ItemStackWrapper chestplate, ItemStackWrapper leggings, ItemStackWrapper boots) {
		ClientBoundPacketData serializer = ClientBoundPacketData.create(PEPacketIDs.MOB_ARMOR_EQUIPMENT, version);
		VarNumberSerializer.writeVarLong(serializer, entityId);
		ItemStackSerializer.writeItemStack(serializer, version, locale, helmet, true);
		ItemStackSerializer.writeItemStack(serializer, version, locale, chestplate, true);
		ItemStackSerializer.writeItemStack(serializer, version, locale, leggings, true);
		ItemStackSerializer.writeItemStack(serializer, version, locale, boots, true);
		return serializer;
	}

	public static ClientBoundPacketData createUpdateHand(ProtocolVersion version, String locale, int entityId, ItemStackWrapper itemstack, int slot, boolean isMainHand) {
		ClientBoundPacketData serializer = ClientBoundPacketData.create(PEPacketIDs.MOB_EQUIPMENT, version);
		VarNumberSerializer.writeVarLong(serializer, entityId);
		ItemStackSerializer.writeItemStack(serializer, version, locale, itemstack, true);
		serializer.writeByte(slot);
		serializer.writeByte(slot);
		serializer.writeByte(isMainHand ? 119 : 0);
		return serializer;
	}

}

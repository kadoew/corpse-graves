package com.kador.corpsegraves.grave;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GraveSavedData extends SavedData {
	public static final Codec<GraveSavedData> CODEC = GraveRecord.CODEC.listOf()
		.xmap(GraveSavedData::new, data -> List.copyOf(data.graves.values()));

	public static final SavedDataType<GraveSavedData> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath("corpsegraves", "graves"),
		GraveSavedData::new,
		CODEC,
		DataFixTypes.SAVED_DATA_MAP_DATA
	);

	private final Map<UUID, GraveRecord> graves = new LinkedHashMap<>();

	public GraveSavedData() {
	}

	public GraveSavedData(List<GraveRecord> records) {
		for (GraveRecord record : records) {
			graves.put(record.graveId(), record);
		}
	}

	public Collection<GraveRecord> all() {
		return graves.values();
	}

	public List<GraveRecord> snapshot() {
		return new ArrayList<>(graves.values());
	}

	public GraveRecord get(UUID id) {
		return id == null ? null : graves.get(id);
	}

	public void add(GraveRecord record) {
		graves.put(record.graveId(), record);
		setDirty();
	}

	public void remove(GraveRecord record) {
		if (record != null) {
			graves.remove(record.graveId());
			setDirty();
		}
	}

	public void rekey(UUID oldKey, GraveRecord record) {
		graves.remove(oldKey);
		graves.put(record.graveId(), record);
		setDirty();
	}

	public boolean contains(UUID id) {
		return graves.containsKey(id);
	}

	public int countFor(UUID owner) {
		int count = 0;
		for (GraveRecord record : graves.values()) {
			if (record.ownerId().equals(owner)) {
				count++;
			}
		}
		return count;
	}

	public GraveRecord findByVisual(UUID entityId) {
		if (entityId == null) {
			return null;
		}
		for (GraveRecord record : graves.values()) {
			if (entityId.equals(record.graveId()) || entityId.equals(record.hologramId()) || entityId.equals(record.hitboxId())) {
				return record;
			}
		}
		return null;
	}
}

package com.kador.corpsegraves.grave;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * All data for one grave. Kept in {@link GraveSavedData} so it survives
 * restarts even if the visual entities are removed from the world.
 */
public final class GraveRecord {
	public static final Codec<StoredItem> ITEM_CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.fieldOf("slot").forGetter(StoredItem::slot),
		ItemStack.CODEC.fieldOf("item").forGetter(StoredItem::stack)
	).apply(i, StoredItem::new));

	public static final Codec<GraveRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
		UUIDUtil.CODEC.fieldOf("grave").forGetter(r -> r.graveId),
		UUIDUtil.CODEC.fieldOf("owner").forGetter(r -> r.ownerId),
		Codec.STRING.fieldOf("owner_name").forGetter(r -> r.ownerName),
		ResolvableProfile.CODEC.fieldOf("profile").forGetter(r -> r.profile),
		Codec.STRING.fieldOf("dimension").forGetter(r -> r.dimension),
		Vec3.CODEC.fieldOf("pos").forGetter(r -> r.pos),
		Codec.FLOAT.fieldOf("yaw").forGetter(r -> r.yaw),
		Codec.STRING.fieldOf("pose").forGetter(r -> r.pose),
		ITEM_CODEC.listOf().fieldOf("items").forGetter(r -> r.items),
		Codec.INT.fieldOf("xp").forGetter(r -> r.xp),
		Codec.INT.fieldOf("unlock_ticks").forGetter(r -> r.unlockTicksLeft),
		Codec.INT.fieldOf("despawn_ticks").forGetter(r -> r.despawnTicksLeft),
		Codec.LONG.fieldOf("created").forGetter(r -> r.createdAt),
		Codec.STRING.fieldOf("visual_ids").forGetter(r -> r.visualIds)
	).apply(i, GraveRecord::new));

	private final UUID graveId;
	private final UUID ownerId;
	private final String ownerName;
	private final ResolvableProfile profile;
	private final String dimension;
	private final Vec3 pos;
	private final float yaw;
	private final String pose;
	private final List<StoredItem> items;
	private final int xp;
	private final long createdAt;

	public int unlockTicksLeft;
	public int despawnTicksLeft;
	private String visualIds;

	public GraveRecord(UUID graveId, UUID ownerId, String ownerName, ResolvableProfile profile, String dimension,
					   Vec3 pos, float yaw, String pose, List<StoredItem> items, int xp,
					   int unlockTicksLeft, int despawnTicksLeft, long createdAt, String visualIds) {
		this.graveId = graveId;
		this.ownerId = ownerId;
		this.ownerName = ownerName;
		this.profile = profile;
		this.dimension = dimension;
		this.pos = pos;
		this.yaw = yaw;
		this.pose = pose;
		this.items = new ArrayList<>(items);
		this.xp = xp;
		this.unlockTicksLeft = unlockTicksLeft;
		this.despawnTicksLeft = despawnTicksLeft;
		this.createdAt = createdAt;
		this.visualIds = visualIds;
	}

	public UUID graveId() { return graveId; }
	public UUID ownerId() { return ownerId; }
	public String ownerName() { return ownerName; }
	public ResolvableProfile profile() { return profile; }
	public String dimension() { return dimension; }
	public Vec3 pos() { return pos; }
	public float yaw() { return yaw; }
	public String pose() { return pose; }
	public List<StoredItem> items() { return items; }
	public int xp() { return xp; }
	public long createdAt() { return createdAt; }

	public String visualIds() { return visualIds; }

	public void setVisualIds(UUID mannequin, UUID hologram, UUID hitbox) {
		this.visualIds = mannequin + "," + hologram + "," + hitbox;
	}

	public UUID maybeVisualId(int index) {
		try {
			String[] parts = visualIds.split(",");
			return parts.length > index ? UUID.fromString(parts[index]) : null;
		} catch (Exception e) {
			return null;
		}
	}

	public UUID hologramId() { return maybeVisualId(1); }
	public UUID hitboxId() { return maybeVisualId(2); }

	public boolean isEmpty() {
		for (StoredItem item : items) {
			if (!item.stack().isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public int itemCount() {
		int count = 0;
		for (StoredItem item : items) {
			if (!item.stack().isEmpty()) {
				count += item.stack().getCount();
			}
		}
		return count;
	}

	public String poseOrDefault(String fallback) {
		return pose == null || pose.isBlank() ? fallback : pose;
	}

	public static ResolvableProfile resolveProfile(GameProfile gameProfile) {
		try {
			return ResolvableProfile.createResolved(gameProfile);
		} catch (Exception e) {
			return ResolvableProfile.createUnresolved(gameProfile.name());
		}
	}

	public record StoredItem(int slot, ItemStack stack) {
	}
}

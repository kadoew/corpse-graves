package com.kador.corpsegraves.grave;

import com.kador.corpsegraves.config.GraveConfig;
import com.mojang.math.Transformation;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Brightness;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;
import java.util.UUID;

public class CorpseEntity extends LivingEntity implements Container {
	public static final int SLOTS = 41;
	private static final EntityDataAccessor<String> DATA_OWNER_NAME =
		SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> DATA_OWNER_UUID =
		SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.STRING);

	private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private UUID ownerId = new UUID(0L, 0L);
	private String ownerName = "";
	private int xp;
	public int unlockTicksLeft;
	public int despawnTicksLeft;
	private UUID hologramId;
	private UUID hitboxId;
	private int tickCounter;
	private boolean finished;

	public CorpseEntity(EntityType<? extends CorpseEntity> type, Level level) {
		super(type, level);
		this.setNoGravity(true);
		this.setInvulnerable(true);
		this.setSilent(true);
	}

	public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
		return LivingEntity.createLivingAttributes();
	}

	@Override
	public HumanoidArm getMainArm() {
		return HumanoidArm.RIGHT;
	}

	@Override
	public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
	}

	@Override
	public ItemStack getItemBySlot(EquipmentSlot slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public Iterable<ItemStack> getArmorSlots() {
		return List.of();
	}

	@Override
	public Iterable<ItemStack> getHandSlots() {
		return List.of(ItemStack.EMPTY, ItemStack.EMPTY);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_OWNER_NAME, "");
		this.entityData.define(DATA_OWNER_UUID, "");
	}

	public void initialize(UUID ownerId, String ownerName, int xp, int unlockTicks, int despawnTicks) {
		this.ownerId = ownerId;
		this.ownerName = ownerName == null ? "" : ownerName;
		this.xp = xp;
		this.unlockTicksLeft = unlockTicks;
		this.despawnTicksLeft = despawnTicks;
		this.entityData.set(DATA_OWNER_NAME, this.ownerName);
		this.entityData.set(DATA_OWNER_UUID, ownerId.toString());
		this.setCustomName(Component.literal(this.ownerName));
		this.setCustomNameVisible(false);
	}

	public UUID ownerId() {
		String value = this.entityData.get(DATA_OWNER_UUID);
		if (value != null && !value.isEmpty()) {
			try {
				return UUID.fromString(value);
			} catch (IllegalArgumentException ignored) {
			}
		}
		return ownerId;
	}

	public String ownerName() {
		String value = this.entityData.get(DATA_OWNER_NAME);
		return value == null || value.isEmpty() ? ownerName : value;
	}

	public int xp() {
		return xp;
	}

	public NonNullList<ItemStack> items() {
		return items;
	}

	public UUID hologramId() {
		return hologramId;
	}

	public UUID hitboxId() {
		return hitboxId;
	}

	public boolean isLockedFor(Player player) {
		return GraveManager.isLockedFor(this, player);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			return;
		}
		ServerLevel level = (ServerLevel) this.level();
		GraveConfig config = GraveConfig.get();
		this.tickCounter++;
		if (this.tickCounter % 20 == 0) {
			this.ensureVisuals(level);
			this.updateHologram(level);
			if (this.isEmpty() && config.removeWhenEmpty) {
				this.finish(null);
				return;
			}
		}
		if (this.unlockTicksLeft > 0) {
			this.unlockTicksLeft--;
		}
		boolean ownerOnline = level.getServer() != null && level.getServer().getPlayerList().getPlayer(this.ownerId) != null;
		if (this.despawnTicksLeft > 0 && !(config.pauseDespawnWhileOwnerOffline && !ownerOnline)) {
			this.despawnTicksLeft--;
		}
		if (this.despawnTicksLeft == 0) {
			this.dropAndDiscard(config.dropLootOnDespawn);
			return;
		}
		if (config.corpse.particles && this.tickCounter % config.corpse.particleIntervalTicks == 0) {
			level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 0.3, this.getZ(),
				1, 0.35, 0.15, 0.35, 0.005);
		}
	}

	private void ensureVisuals(ServerLevel level) {
		if (this.hologramId == null || level.getEntity(this.hologramId) == null) {
			this.spawnHologram(level);
		}
		if (this.hitboxId == null || level.getEntity(this.hitboxId) == null) {
			this.spawnHitbox(level);
		}
	}

	private void spawnHologram(ServerLevel level) {
		GraveConfig.Hologram cfg = GraveConfig.get().hologram;
		if (!cfg.enabled) {
			return;
		}
		Display.TextDisplay display = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
		display.moveTo(this.getX(), this.getY() + cfg.height, this.getZ(), 0.0F, 0.0F);
		display.setBillboardConstraints(Display.BillboardConstraints.CENTER);
		display.setText(hologramText(cfg));
		display.setLineWidth(cfg.lineWidth);
		display.setBackgroundColor(GraveConfig.parseArgb(cfg.background, 0x66000000));
		display.setViewRange((float) cfg.viewRange);
		display.setTransformation(new Transformation(new Matrix4f().scaling((float) cfg.scale)));
		display.setBrightnessOverride(Brightness.FULL_BRIGHT);
		byte flags = 0;
		if (cfg.shadow) flags |= Display.TextDisplay.FLAG_SHADOW;
		if (cfg.seeThrough) flags |= Display.TextDisplay.FLAG_SEE_THROUGH;
		display.setFlags(flags);
		display.setNoGravity(true);
		display.setInvulnerable(true);
		display.setSilent(true);
		level.addFreshEntity(display);
		this.hologramId = display.getUUID();
	}

	private void spawnHitbox(ServerLevel level) {
		Interaction hitbox = new Interaction(EntityType.INTERACTION, level);
		hitbox.moveTo(this.getX(), this.getY() + 0.05, this.getZ(), 0.0F, 0.0F);
		hitbox.setWidth(3.2F);
		hitbox.setHeight(1.4F);
		hitbox.setResponse(true);
		hitbox.setNoGravity(true);
		hitbox.setInvulnerable(true);
		hitbox.setSilent(true);
		level.addFreshEntity(hitbox);
		this.hitboxId = hitbox.getUUID();
	}

	private void updateHologram(ServerLevel level) {
		GraveConfig.Hologram cfg = GraveConfig.get().hologram;
		if (!cfg.enabled || this.hologramId == null) {
			return;
		}
		Entity entity = level.getEntity(this.hologramId);
		if (entity instanceof Display.TextDisplay display) {
			display.setText(hologramText(cfg));
		}
	}

	private Component hologramText(GraveConfig.Hologram cfg) {
		MutableComponent text = Component.empty();
		boolean first = true;
		if (cfg.showName) {
			text.append(Component.literal(this.ownerName)
				.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(GraveConfig.parseRgb(cfg.nameColor, 0xFFAA00))).withBold(true)));
			first = false;
		}
		if (cfg.showDespawn) {
			if (!first) text.append(Component.literal("\n"));
			first = false;
			Component time = this.despawnTicksLeft < 0
				? Component.translatable("corpsegraves.hologram.never")
				: Component.literal(GraveManager.formatTime((int) Math.ceil(this.despawnTicksLeft / 20.0)));
			text.append(Component.translatable("corpsegraves.hologram.despawns", time)
				.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(GraveConfig.parseRgb(cfg.despawnColor, 0x55FFFF)))));
		}
		if (cfg.showProtection) {
			if (!first) text.append(Component.literal("\n"));
			if (this.unlockTicksLeft == 0) {
				text.append(Component.translatable("corpsegraves.hologram.unlocked")
					.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(GraveConfig.parseRgb(cfg.unlockedColor, 0x55FF55)))));
			} else {
				Component time = this.unlockTicksLeft < 0
					? Component.translatable("corpsegraves.hologram.forever")
					: Component.literal(GraveManager.formatTime((int) Math.ceil(this.unlockTicksLeft / 20.0)));
				text.append(Component.translatable("corpsegraves.hologram.protected", time)
					.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(GraveConfig.parseRgb(cfg.protectedColor, 0xFF5555)))));
			}
		}
		return text;
	}

	public void finish(ServerPlayer recipient) {
		if (this.finished) {
			return;
		}
		this.finished = true;
		ServerLevel level = (ServerLevel) this.level();
		int reward = Math.max(0, (int) Math.round(this.xp * (GraveConfig.get().xpRecoveryPercent / 100.0)));
		if (reward > 0) {
			if (recipient != null) {
				recipient.giveExperiencePoints(reward);
				recipient.displayClientMessage(Component.translatable("corpsegraves.message.xp_recovered", reward), true);
			} else {
				ExperienceOrb.award(level, this.position().add(0.0, 0.5, 0.0), reward);
			}
		}
		this.discardVisuals();
		level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 0.5, this.getZ(),
			12, 0.4, 0.5, 0.4, 0.02);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.BLOCKS, 0.7F, 1.0F);
		this.discard();
	}

	public void dropAndDiscard(boolean drop) {
		if (this.finished) {
			return;
		}
		this.finished = true;
		ServerLevel level = (ServerLevel) this.level();
		if (drop) {
			for (ItemStack stack : this.items) {
				if (!stack.isEmpty()) {
					ItemEntity item = new ItemEntity(level, this.getX(), this.getY() + 0.35, this.getZ(), stack.copy());
					item.setPickUpDelay(20);
					item.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.12, 0.1, (level.random.nextDouble() - 0.5) * 0.12);
					level.addFreshEntity(item);
				}
			}
			int reward = Math.max(0, (int) Math.round(this.xp * (GraveConfig.get().xpRecoveryPercent / 100.0)));
			if (reward > 0) {
				ExperienceOrb.award(level, this.position().add(0.0, 0.5, 0.0), reward);
			}
		}
		this.items.clear();
		this.discardVisuals();
		this.discard();
	}

	private void discardVisuals() {
		if (this.level() instanceof ServerLevel level) {
			if (this.hologramId != null) {
				Entity e = level.getEntity(this.hologramId);
				if (e != null) e.discard();
				this.hologramId = null;
			}
			if (this.hitboxId != null) {
				Entity e = level.getEntity(this.hitboxId);
				if (e != null) e.discard();
				this.hitboxId = null;
			}
		}
	}

	@Override
	public void remove(RemovalReason reason) {
		this.discardVisuals();
		super.remove(reason);
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
		return true;
	}

	@Override
	public boolean shouldShowName() {
		return false;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putUUID("Owner", this.ownerId);
		tag.putString("OwnerName", this.ownerName);
		tag.putInt("GraveXp", this.xp);
		tag.putInt("UnlockTicks", this.unlockTicksLeft);
		tag.putInt("DespawnTicks", this.despawnTicksLeft);
		if (this.hologramId != null) tag.putUUID("Hologram", this.hologramId);
		if (this.hitboxId != null) tag.putUUID("Hitbox", this.hitboxId);
		ContainerHelper.saveAllItems(tag, this.items);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.hasUUID("Owner")) this.ownerId = tag.getUUID("Owner");
		this.ownerName = tag.getString("OwnerName");
		this.xp = tag.getInt("GraveXp");
		this.unlockTicksLeft = tag.getInt("UnlockTicks");
		this.despawnTicksLeft = tag.getInt("DespawnTicks");
		if (tag.hasUUID("Hologram")) this.hologramId = tag.getUUID("Hologram");
		if (tag.hasUUID("Hitbox")) this.hitboxId = tag.getUUID("Hitbox");
		ContainerHelper.loadAllItems(tag, this.items);
		this.entityData.set(DATA_OWNER_NAME, this.ownerName);
		this.entityData.set(DATA_OWNER_UUID, this.ownerId.toString());
		this.setCustomName(Component.literal(this.ownerName));
		this.setCustomNameVisible(false);
	}

	@Override
	public int getContainerSize() {
		return SLOTS;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : this.items) if (!stack.isEmpty()) return false;
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return slot >= 0 && slot < SLOTS ? this.items.get(slot) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack stack = slot >= 0 && slot < SLOTS ? this.items.get(slot) : ItemStack.EMPTY;
		if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
		ItemStack split = stack.split(amount);
		if (stack.isEmpty()) this.items.set(slot, ItemStack.EMPTY);
		this.setChanged();
		return split;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		if (slot < 0 || slot >= SLOTS) return ItemStack.EMPTY;
		ItemStack stack = this.items.get(slot);
		this.items.set(slot, ItemStack.EMPTY);
		return stack;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (slot >= 0 && slot < SLOTS) {
			this.items.set(slot, stack);
			this.setChanged();
		}
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(Player player) {
		return !this.isRemoved();
	}

	@Override
	public void clearContent() {
		this.items.clear();
	}
}

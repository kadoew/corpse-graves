package com.kador.corpsegraves.grave;

import com.mojang.authlib.GameProfile;
import com.mojang.math.Transformation;
import com.mojang.serialization.DataResult;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Brightness;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.kador.corpsegraves.CorpseGraves;
import com.kador.corpsegraves.config.GraveConfig;
import com.kador.corpsegraves.network.GraveProtocol;

public final class GraveManager {
	private static final Map<UUID, Long> LAST_DEATH_TICK = new HashMap<>();
	private static final Map<UUID, Integer> TICK_COUNTERS = new HashMap<>();
	private static final Set<UUID> UNLOCK_ANNOUNCED = new HashSet<>();

	private GraveManager() {
	}

	// ------------------------------------------------------------------
	// Death handling
	// ------------------------------------------------------------------

	public static void handleDeath(ServerPlayer player, ServerLevel level) {
		GraveConfig config = GraveConfig.get();
		if (!config.enabled || player instanceof FakePlayer) {
			return;
		}
		if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
			return;
		}
		long gameTime = level.getGameTime();
		Long previous = LAST_DEATH_TICK.put(player.getUUID(), gameTime);
		if (previous != null && previous == gameTime) {
			return;
		}

		Inventory inventory = player.getInventory();
		List<GraveRecord.StoredItem> stored = new ArrayList<>();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			ItemStack copy = stack.copy();
			if (config.respectCurseOfVanishing
				&& EnchantmentHelper.has(copy, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
				continue;
			}
			stored.add(new GraveRecord.StoredItem(slot, copy));
		}

		int xp = 0;
		if (config.storeXp) {
			xp = totalExperience(player);
		}

		if (stored.isEmpty() && xp <= 0) {
			if (config.storeXp) {
				player.experienceLevel = 0;
				player.experienceProgress = 0.0F;
				player.totalExperience = 0;
			}
			return;
		}

		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			inventory.setItem(slot, ItemStack.EMPTY);
		}
		if (config.storeXp) {
			player.experienceLevel = 0;
			player.experienceProgress = 0.0F;
			player.totalExperience = 0;
		}

		GraveSavedData data = getData(level.getServer());
		enforceLimit(level.getServer(), data, player.getUUID(), config.maxGravesPerPlayer);

		Spot spot = findSpot(level, player.position(), player.getYRot() + config.corpse.yawOffset);
		GameProfile gameProfile = player.getGameProfile();
		ResolvableProfile profile = GraveRecord.resolveProfile(gameProfile);
		GraveRecord record = new GraveRecord(
			UUID.randomUUID(),
			player.getUUID(),
			player.getName().getString(),
			profile,
			level.dimension().identifier().toString(),
			spot.pos(),
			spot.yaw(),
			config.corpse.pose,
			stored,
			xp,
			config.unlockSeconds * 20,
			config.despawnSeconds * 20,
			level.getGameTime(),
			""
		);
		if (config.unlockSeconds < 0) {
			record.unlockTicksLeft = -1;
		}
		if (config.despawnSeconds < 0) {
			record.despawnTicksLeft = -1;
		}

		if (!spawnAll(level, record)) {
			CorpseGraves.LOGGER.error("Failed to spawn grave for {}, returning loot to the world", player.getName().getString());
			dropContentsDirect(level, spot.pos(), record.items());
			if (record.xp() > 0) {
				ExperienceOrb.award(level, spot.pos(), xpReward(record, config));
			}
			return;
		}
		data.add(record);
		try {
			syncGear(record, level);
			updateHologram(record, level, true);
		} catch (Throwable t) {
			CorpseGraves.LOGGER.error("Could not finish grave visuals for {}", player.getName().getString(), t);
		}

		if (config.corpse.particles) {
			level.sendParticles(ParticleTypes.SOUL, spot.pos().x, spot.pos().y + 0.4, spot.pos().z,
				8, 0.5, 0.4, 0.5, 0.01);
		}
		if (config.announceCoordinates) {
			player.sendSystemMessage(Component.translatable("corpsegraves.message.created",
				(int) Math.floor(spot.pos().x), (int) Math.floor(spot.pos().y), (int) Math.floor(spot.pos().z))
				.withStyle(style -> style.withColor(ChatFormatting.GRAY)));
		}
		CorpseGraves.LOGGER.info("Created grave for {} with {} item stacks at {} {} {}",
			player.getName().getString(), stored.size(), spot.pos().x, spot.pos().y, spot.pos().z);
	}

	private static void enforceLimit(MinecraftServer server, GraveSavedData data, UUID owner, int max) {
		List<GraveRecord> owned = new ArrayList<>();
		for (GraveRecord record : data.snapshot()) {
			if (record.ownerId().equals(owner)) {
				owned.add(record);
			}
		}
		owned.sort(Comparator.comparingLong(GraveRecord::createdAt));
		while (owned.size() >= max) {
			GraveRecord oldest = owned.remove(0);
			ServerLevel level = levelOf(server, oldest);
			if (level != null) {
				dropContents(level, oldest);
			}
			discardVisuals(server, oldest);
			data.remove(oldest);
		}
	}

	// ------------------------------------------------------------------
	// Per tick
	// ------------------------------------------------------------------

	public static void tick(MinecraftServer server) {
		GraveConfig config = GraveConfig.get();
		if (!config.enabled) {
			return;
		}
		GraveSavedData data = getData(server);
		if (data.all().isEmpty()) {
			return;
		}
		for (GraveRecord record : data.snapshot()) {
			try {
				tickGrave(server, data, record, config);
			} catch (Throwable t) {
				CorpseGraves.LOGGER.error("Error while ticking grave of {}", record.ownerName(), t);
			}
		}
		if (TICK_COUNTERS.size() > 512) {
			TICK_COUNTERS.clear();
		}
	}

	private static void tickGrave(MinecraftServer server, GraveSavedData data, GraveRecord record, GraveConfig config) {
		ServerLevel level = levelOf(server, record);
		if (level == null) {
			return;
		}
		BlockPos pos = BlockPos.containing(record.pos());
		boolean loaded = level.isLoaded(pos);
		if (!loaded && config.pauseDespawnInUnloadedChunks) {
			return;
		}

		boolean ownerOnline = server.getPlayerList().getPlayer(record.ownerId()) != null;
		boolean pauseDespawn = !ownerOnline && config.pauseDespawnWhileOwnerOffline;

		if (record.unlockTicksLeft > 0) {
			record.unlockTicksLeft--;
			if (record.unlockTicksLeft == 0 && !UNLOCK_ANNOUNCED.contains(record.graveId())) {
				UNLOCK_ANNOUNCED.add(record.graveId());
				level.playSound(null, record.pos().x, record.pos().y, record.pos().z,
					SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
			}
			data.setDirty();
		}

		if (record.despawnTicksLeft > 0 && !pauseDespawn) {
			record.despawnTicksLeft--;
			data.setDirty();
		}

		if (!loaded) {
			return;
		}

		if (record.despawnTicksLeft == 0) {
			despawn(server, data, record, config);
			return;
		}

		if (!ensureVisuals(level, record)) {
			return;
		}

		int counter = TICK_COUNTERS.merge(record.graveId(), 1, Integer::sum);
		if (counter % 20 == 0) {
			syncGear(record, level);
			if (record.isEmpty() && config.removeWhenEmpty) {
				finish(server, data, record, null);
				return;
			}
		}
		if (counter % 20 == 0) {
			updateHologram(record, level, false);
		}
		if (config.corpse.particles && counter % config.corpse.particleIntervalTicks == 0) {
			level.sendParticles(ParticleTypes.SOUL, record.pos().x, record.pos().y + 0.3, record.pos().z,
				1, 0.35, 0.15, 0.35, 0.005);
		}
	}

	// ------------------------------------------------------------------
	// Visual entities (vanilla Mannequin + TextDisplay + Interaction)
	// ------------------------------------------------------------------

	private static boolean spawnAll(ServerLevel level, GraveRecord record) {
		Mannequin mannequin = spawnMannequin(level, record);
		if (mannequin == null) {
			return false;
		}
		UUID hologramId = spawnHologram(level, record);
		UUID hitboxId = spawnHitbox(level, record);
		record.setVisualIds(mannequin.getUUID(), hologramId, hitboxId);
		return true;
	}

	private static boolean ensureVisuals(ServerLevel level, GraveRecord record) {
		Entity existing = level.getEntity(record.graveId());
		if (!(existing instanceof Mannequin mannequin) || existing.isRemoved()) {
			discardVisuals(level.getServer(), record);
			return spawnAll(level, record);
		}
		mannequin.setPose(parsePose(record.pose(), GraveConfig.get().corpse.pose));
		mannequin.setNoGravity(true);
		if (record.maybeVisualId(1) == null || level.getEntity(record.hologramId()) == null) {
			UUID id = spawnHologram(level, record);
			record.setVisualIds(record.graveId(), id, record.hitboxId());
		}
		if (record.maybeVisualId(2) == null || level.getEntity(record.hitboxId()) == null) {
			UUID id = spawnHitbox(level, record);
			record.setVisualIds(record.graveId(), record.hologramId(), id);
		}
		return true;
	}

	private static Vec3 mannequinPos(GraveRecord record) {
		GraveConfig.Corpse cfg = GraveConfig.get().corpse;
		Vec3 forward = forward(record.yaw());
		return record.pos().add(forward.scale(cfg.forwardOffset)).add(0.0, cfg.yOffset, 0.0);
	}

	private static Mannequin spawnMannequin(ServerLevel level, GraveRecord record) {
		Mannequin mannequin = EntityTypes.MANNEQUIN.create(level, EntitySpawnReason.COMMAND);
		if (mannequin == null) {
			return null;
		}
		mannequin.setUUID(record.graveId());
		applyProfile(level, mannequin, record.profile());
		mannequin.setPose(parsePose(record.pose(), GraveConfig.get().corpse.pose));
		Vec3 p = mannequinPos(record);
		mannequin.setPos(p.x, p.y, p.z);
		mannequin.setYRot(record.yaw());
		mannequin.setYBodyRot(record.yaw());
		mannequin.setYHeadRot(record.yaw());
		mannequin.setNoGravity(true);
		mannequin.setSilent(true);
		if (!level.addFreshEntity(mannequin)) {
			return null;
		}
		return mannequin;
	}

	private static void applyProfile(ServerLevel level, Mannequin mannequin, ResolvableProfile profile) {
		try {
			CompoundTag tag = new CompoundTag();
			DataResult<Tag> encoded = ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, profile);
			if (encoded.result().isPresent()) {
				tag.put("profile", encoded.result().get());
			}
			tag.putBoolean("immovable", true);
			tag.putBoolean("hide_description", true);
			tag.putBoolean("Invulnerable", true);
			mannequin.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
		} catch (Exception e) {
			CorpseGraves.LOGGER.warn("Could not apply grave profile", e);
		}
	}

	private static UUID spawnHologram(ServerLevel level, GraveRecord record) {
		GraveConfig.Hologram cfg = GraveConfig.get().hologram;
		Display.TextDisplay display = EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.COMMAND);
		if (display == null) {
			return null;
		}
		UUID id = record.hologramId();
		if (id == null) {
			id = UUID.randomUUID();
		}
		display.setUUID(id);
		Vec3 forward = forward(record.yaw());
		Vec3 p = record.pos().add(forward.scale(cfg.forwardOffset)).add(0.0, cfg.height, 0.0);
		display.setPos(p.x, p.y, p.z);
		display.setBillboardConstraints(Display.BillboardConstraints.CENTER);
		display.setLineWidth(cfg.lineWidth);
		display.setBackgroundColor(GraveConfig.parseArgb(cfg.background, 0xA0000000));
		display.setViewRange((float) cfg.viewRange);
		display.setTransformation(new Transformation(new Matrix4f().scaling((float) cfg.scale)));
		display.setBrightnessOverride(Brightness.FULL_BRIGHT);
		byte flags = 0;
		if (cfg.shadow) flags |= Display.TextDisplay.FLAG_SHADOW;
		if (cfg.seeThrough) flags |= Display.TextDisplay.FLAG_SEE_THROUGH;
		display.setFlags(flags);
		display.setNoGravity(true);
		display.setSilent(true);
		display.setText(hologramText(record));
		if (!level.addFreshEntity(display)) {
			return null;
		}
		return display.getUUID();
	}

	private static UUID spawnHitbox(ServerLevel level, GraveRecord record) {
		GraveConfig.Corpse cfg = GraveConfig.get().corpse;
		Interaction hitbox = EntityTypes.INTERACTION.create(level, EntitySpawnReason.COMMAND);
		if (hitbox == null) {
			return null;
		}
		UUID id = record.hitboxId();
		if (id == null) {
			id = UUID.randomUUID();
		}
		hitbox.setUUID(id);
		Vec3 forward = forward(record.yaw());
		Vec3 p = record.pos().add(forward.scale(cfg.hitboxForwardOffset)).add(0.0, 0.05, 0.0);
		hitbox.setPos(p.x, p.y, p.z);
		hitbox.setWidth(cfg.hitboxWidth);
		hitbox.setHeight(cfg.hitboxHeight);
		hitbox.setResponse(true);
		hitbox.setNoGravity(true);
		hitbox.setSilent(true);
		if (!level.addFreshEntity(hitbox)) {
			return null;
		}
		return hitbox.getUUID();
	}

	private static void syncGear(GraveRecord record, ServerLevel level) {
		Entity entity = level.getEntity(record.graveId());
		if (!(entity instanceof Mannequin mannequin)) {
			return;
		}
		GraveConfig.Corpse cfg = GraveConfig.get().corpse;
		setGear(mannequin, record, 36, EquipmentSlot.FEET, cfg.displayArmor);
		setGear(mannequin, record, 37, EquipmentSlot.LEGS, cfg.displayArmor);
		setGear(mannequin, record, 38, EquipmentSlot.CHEST, cfg.displayArmor);
		setGear(mannequin, record, 39, EquipmentSlot.HEAD, cfg.displayArmor);
		setGear(mannequin, record, 40, EquipmentSlot.OFFHAND, cfg.displayOffhand);
	}

	private static void setGear(Mannequin mannequin, GraveRecord record, int slot, EquipmentSlot equipmentSlot, boolean enabled) {
		ItemStack wanted = ItemStack.EMPTY;
		if (enabled) {
			for (GraveRecord.StoredItem item : record.items()) {
				if (item.slot() == slot && !item.stack().isEmpty()) {
					wanted = item.stack().copy();
					break;
				}
			}
		}
		ItemStack current = mannequin.getItemBySlot(equipmentSlot);
		if (!ItemStack.matches(current, wanted)) {
			mannequin.setItemSlot(equipmentSlot, wanted);
		}
	}

	private static void discardVisuals(MinecraftServer server, GraveRecord record) {
		ServerLevel level = levelOf(server, record);
		if (level == null) {
			return;
		}
		Entity mannequin = level.getEntity(record.graveId());
		if (mannequin instanceof Mannequin m) {
			m.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
			m.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
			m.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
			m.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
			m.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
			m.discard();
		}
		Entity hologram = record.hologramId() == null ? null : level.getEntity(record.hologramId());
		if (hologram != null) {
			hologram.discard();
		}
		Entity hitbox = record.hitboxId() == null ? null : level.getEntity(record.hitboxId());
		if (hitbox != null) {
			hitbox.discard();
		}
	}

	// ------------------------------------------------------------------
	// Hologram
	// ------------------------------------------------------------------

	private static MutableComponent hologramText(GraveRecord record) {
		GraveConfig.Hologram cfg = GraveConfig.get().hologram;
		MutableComponent text = Component.empty();
		boolean first = true;
		if (cfg.showName) {
			text.append(Component.literal(record.ownerName())
				.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(GraveConfig.parseRgb(cfg.nameColor, 0xFFAA00)))
					.withBold(true)));
			first = false;
		}
		if (cfg.showDespawn) {
			if (!first) text.append(Component.literal("\n"));
			first = false;
			MutableComponent line = Component.translatable("corpsegraves.hologram.despawns", timeText(record.despawnTicksLeft, false));
			text.append(line.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(GraveConfig.parseRgb(cfg.despawnColor, 0x55FFFF)))));
		}
		if (cfg.showProtection) {
			if (!first) text.append(Component.literal("\n"));
			MutableComponent line;
			if (record.unlockTicksLeft == 0) {
				line = Component.translatable("corpsegraves.hologram.unlocked");
				text.append(line.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(GraveConfig.parseRgb(cfg.unlockedColor, 0x55FF55)))));
			} else {
				line = Component.translatable("corpsegraves.hologram.protected", timeText(record.unlockTicksLeft, true));
				text.append(line.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(GraveConfig.parseRgb(cfg.protectedColor, 0xFF5555)))));
			}
		}
		return text;
	}

	private static Component timeText(int ticks, boolean foreverAllowed) {
		if (ticks < 0 && foreverAllowed) {
			return Component.translatable("corpsegraves.hologram.forever");
		}
		if (ticks < 0) {
			return Component.translatable("corpsegraves.hologram.never");
		}
		return Component.literal(formatTime((int) Math.ceil(ticks / 20.0)));
	}

	public static String formatTime(int seconds) {
		seconds = Math.max(0, seconds);
		int hours = seconds / 3600;
		int minutes = (seconds % 3600) / 60;
		int secs = seconds % 60;
		if (hours > 0) {
			return String.format("%d:%02d:%02d", hours, minutes, secs);
		}
		return String.format("%02d:%02d", minutes, secs);
	}

	private static void updateHologram(GraveRecord record, ServerLevel level, boolean force) {
		GraveConfig.Hologram cfg = GraveConfig.get().hologram;
		if (!cfg.enabled) {
			return;
		}
		Entity entity = record.hologramId() == null ? null : level.getEntity(record.hologramId());
		if (entity instanceof Display.TextDisplay display) {
			Component text = hologramText(record);
			if (force || !text.equals(display.getText())) {
				display.setText(text);
			}
		}
	}

	public static void refreshHolograms(MinecraftServer server) {
		for (GraveRecord record : getData(server).snapshot()) {
			ServerLevel level = levelOf(server, record);
			if (level != null) {
				updateHologram(record, level, true);
			}
		}
	}

	// ------------------------------------------------------------------
	// Interaction
	// ------------------------------------------------------------------

	public static InteractionResult onUse(Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hitResult) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.PASS;
		}
		GraveRecord record = getData(serverLevel.getServer()).findByVisual(entity.getUUID());
		if (record == null) {
			return InteractionResult.PASS;
		}
		if (entity instanceof Display) {
			return InteractionResult.PASS;
		}
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.SUCCESS;
		}
		if (!canAccess(serverPlayer, record)) {
			sendLockedMessage(serverPlayer, record);
			return InteractionResult.SUCCESS;
		}
		if (serverPlayer.isShiftKeyDown()) {
			quickRestore(serverPlayer, record);
		} else if (GraveProtocol.isModded(serverPlayer)) {
			openGrave(serverPlayer, record);
		} else if (GraveConfig.get().vanillaClientQuickLoot) {
			takeAll(serverPlayer, record);
			serverPlayer.sendSystemMessage(Component.translatable("corpsegraves.message.vanilla_client")
				.withStyle(style -> style.withColor(ChatFormatting.GRAY)), true);
		} else {
			serverPlayer.sendSystemMessage(Component.translatable("corpsegraves.message.install_client")
				.withStyle(style -> style.withColor(ChatFormatting.YELLOW)), true);
		}
		return InteractionResult.SUCCESS;
	}

	public static InteractionResult onAttack(Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hitResult) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.PASS;
		}
		GraveRecord record = getData(serverLevel.getServer()).findByVisual(entity.getUUID());
		if (record == null) {
			return InteractionResult.PASS;
		}
		if (!canAccess(serverPlayer, record)) {
			sendLockedMessage(serverPlayer, record);
		}
		return InteractionResult.SUCCESS;
	}

	public static boolean canAccess(Player player, GraveRecord record) {
		GraveConfig config = GraveConfig.get();
		if (config.ownedGravesAlwaysAccessible && player.getUUID().equals(record.ownerId())) {
			return true;
		}
		if (config.operatorsBypassProtection && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
			return true;
		}
		return record.unlockTicksLeft == 0;
	}

	private static void sendLockedMessage(ServerPlayer player, GraveRecord record) {
		MutableComponent message;
		if (record.unlockTicksLeft < 0) {
			message = Component.translatable("corpsegraves.message.locked_forever");
		} else {
			message = Component.translatable("corpsegraves.message.locked",
				formatTime((int) Math.ceil(record.unlockTicksLeft / 20.0)));
		}
		player.sendSystemMessage(message.withStyle(style -> style.withColor(ChatFormatting.RED)), true);
	}

	public static void openGrave(ServerPlayer player, GraveRecord record) {
		if (player.containerMenu != player.inventoryMenu) {
			return;
		}
		Component title = Component.translatable("corpsegraves.gui.title", record.ownerName());
		player.openMenu(new SimpleMenuProvider((syncId, inventory, p) -> new GraveMenu(syncId, inventory, record), title));
		player.playSound(SoundEvents.ENDER_CHEST_OPEN, 0.7F, 1.2F);
	}

	public static void quickRestore(ServerPlayer player, GraveRecord record) {
		Inventory inventory = player.getInventory();
		List<GraveRecord.StoredItem> leftovers = new ArrayList<>();
		List<GraveRecord.StoredItem> ordered = new ArrayList<>(record.items());
		ordered.sort(Comparator.comparingInt(item -> item.slot() >= 36 ? 0 : 1));
		int moved = 0;
		for (GraveRecord.StoredItem item : ordered) {
			ItemStack stack = item.stack().copy();
			int slot = item.slot();
			ItemStack current = inventory.getItem(slot);
			if (current.isEmpty()) {
				inventory.setItem(slot, stack);
				moved++;
				continue;
			}
			if (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() < current.getMaxStackSize()) {
				int space = current.getMaxStackSize() - current.getCount();
				int amount = Math.min(space, stack.getCount());
				current.grow(amount);
				stack.shrink(amount);
				inventory.setItem(slot, current);
				moved++;
			}
			if (!stack.isEmpty()) {
				inventory.add(stack);
				if (!stack.isEmpty()) {
					leftovers.add(new GraveRecord.StoredItem(slot, stack));
					continue;
				}
				moved++;
			}
		}
		record.items().clear();
		record.items().addAll(leftovers);
		GraveSavedData data = getData(player.level().getServer());
		data.setDirty();
		syncGear(record, player.level());
		if (leftovers.isEmpty()) {
			finish(player.level().getServer(), data, record, player);
		} else {
			player.sendSystemMessage(Component.translatable("corpsegraves.message.inventory_full", leftovers.size())
				.withStyle(style -> style.withColor(ChatFormatting.YELLOW)), true);
			player.playSound(SoundEvents.ITEM_PICKUP, 0.8F, 1.1F);
		}
	}

	public static void takeAll(ServerPlayer player, GraveRecord record) {
		Inventory inventory = player.getInventory();
		List<GraveRecord.StoredItem> leftovers = new ArrayList<>();
		for (GraveRecord.StoredItem item : new ArrayList<>(record.items())) {
			ItemStack stack = item.stack().copy();
			inventory.add(stack);
			if (!stack.isEmpty()) {
				leftovers.add(new GraveRecord.StoredItem(item.slot(), stack));
			}
		}
		record.items().clear();
		record.items().addAll(leftovers);
		GraveSavedData data = getData(player.level().getServer());
		data.setDirty();
		syncGear(record, player.level());
		if (leftovers.isEmpty()) {
			finish(player.level().getServer(), data, record, player);
		} else {
			player.sendSystemMessage(Component.translatable("corpsegraves.message.inventory_full", leftovers.size())
				.withStyle(style -> style.withColor(ChatFormatting.YELLOW)), true);
			player.playSound(SoundEvents.ITEM_PICKUP, 0.8F, 1.1F);
		}
	}

	// ------------------------------------------------------------------
	// Removal
	// ------------------------------------------------------------------

	public static void finish(MinecraftServer server, GraveSavedData data, GraveRecord record, ServerPlayer recipient) {
		if (!data.contains(record.graveId())) {
			return;
		}
		data.remove(record);
		discardVisuals(server, record);
		ServerLevel level = levelOf(server, record);
		int xp = xpReward(record, GraveConfig.get());
		if (xp > 0) {
			if (recipient != null) {
				recipient.giveExperiencePoints(xp);
				recipient.sendSystemMessage(Component.translatable("corpsegraves.message.xp_recovered", xp)
					.withStyle(style -> style.withColor(ChatFormatting.AQUA)), true);
			} else if (level != null) {
				ExperienceOrb.award(level, record.pos().add(0.0, 0.5, 0.0), xp);
			}
		}
		if (level != null) {
			level.sendParticles(ParticleTypes.SOUL, record.pos().x, record.pos().y + 0.5, record.pos().z,
				12, 0.4, 0.5, 0.4, 0.02);
			level.playSound(null, record.pos().x, record.pos().y, record.pos().z,
				SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 0.7F, 1.0F);
		}
		UNLOCK_ANNOUNCED.remove(record.graveId());
		TICK_COUNTERS.remove(record.graveId());
	}

	private static void despawn(MinecraftServer server, GraveSavedData data, GraveRecord record, GraveConfig config) {
		ServerLevel level = levelOf(server, record);
		data.remove(record);
		discardVisuals(server, record);
		if (level != null) {
			if (config.dropLootOnDespawn) {
				dropContents(level, record);
				int xp = xpReward(record, config);
				if (xp > 0) {
					ExperienceOrb.award(level, record.pos().add(0.0, 0.5, 0.0), xp);
				}
			}
			level.sendParticles(ParticleTypes.SMOKE, record.pos().x, record.pos().y + 0.3, record.pos().z,
				12, 0.4, 0.3, 0.4, 0.01);
		}
		CorpseGraves.LOGGER.info("Grave of {} despawned", record.ownerName());
		UNLOCK_ANNOUNCED.remove(record.graveId());
		TICK_COUNTERS.remove(record.graveId());
	}

	public static void removeAll(MinecraftServer server, UUID owner, boolean drop) {
		GraveSavedData data = getData(server);
		for (GraveRecord record : data.snapshot()) {
			if (record.ownerId().equals(owner)) {
				ServerLevel level = levelOf(server, record);
				if (drop && level != null) {
					dropContents(level, record);
				}
				data.remove(record);
				discardVisuals(server, record);
				TICK_COUNTERS.remove(record.graveId());
			}
		}
	}

	public static void unlockAll(MinecraftServer server, UUID owner) {
		GraveSavedData data = getData(server);
		for (GraveRecord record : data.snapshot()) {
			if (record.ownerId().equals(owner)) {
				record.unlockTicksLeft = 0;
				ServerLevel level = levelOf(server, record);
				if (level != null) {
					updateHologram(record, level, true);
				}
			}
		}
		data.setDirty();
	}

	private static void dropContents(ServerLevel level, GraveRecord record) {
		dropContentsDirect(level, record.pos(), record.items());
		record.items().clear();
	}

	private static void dropContentsDirect(ServerLevel level, Vec3 pos, List<GraveRecord.StoredItem> items) {
		for (GraveRecord.StoredItem item : items) {
			if (item.stack().isEmpty()) {
				continue;
			}
			ItemEntity entity = new ItemEntity(level, pos.x, pos.y + 0.35, pos.z, item.stack().copy());
			entity.setPickUpDelay(20);
			entity.setDeltaMovement((level.getRandom().nextDouble() - 0.5) * 0.12, 0.1, (level.getRandom().nextDouble() - 0.5) * 0.12);
			level.addFreshEntity(entity);
		}
	}

	private static int xpReward(GraveRecord record, GraveConfig config) {
		if (!config.storeXp || record.xp() <= 0) {
			return 0;
		}
		return Math.max(0, (int) Math.round(record.xp() * (config.xpRecoveryPercent / 100.0)));
	}

	public static int recoverableXp(GraveRecord record) {
		return xpReward(record, GraveConfig.get());
	}

	private static int totalExperience(Player player) {
		int fromLevels = xpToReachLevel(player.experienceLevel)
			+ Math.round(player.experienceProgress * xpNeededForNextLevel(player.experienceLevel));
		return Math.max(player.totalExperience, fromLevels);
	}

	private static int xpNeededForNextLevel(int level) {
		if (level >= 30) {
			return 112 + (level - 30) * 9;
		}
		if (level >= 15) {
			return 37 + (level - 15) * 5;
		}
		return 7 + level * 2;
	}

	private static int xpToReachLevel(int level) {
		int total = 0;
		for (int i = 0; i < level; i++) {
			total += xpNeededForNextLevel(i);
		}
		return total;
	}

	// ------------------------------------------------------------------
	// Placement
	// ------------------------------------------------------------------

	private record Spot(Vec3 pos, float yaw) {
	}

	private static Spot findSpot(ServerLevel level, Vec3 death, float yaw) {
		GraveConfig.VoidRescue voidCfg = GraveConfig.get().voidRescue;
		if (voidCfg.enabled && death.y < voidCfg.triggerBelowY) {
			Vec3 rescued = findSurface(level, death, voidCfg.searchRadius);
			if (rescued == null) {
				rescued = new Vec3(death.x, level.getMinY() + 5.0, death.z);
			}
			return new Spot(rescued, yaw);
		}
		Vec3 ground = findGroundNear(level, death, 3);
		if (ground == null) {
			ground = new Vec3(death.x, Math.max(death.y, level.getMinY() + 1.0), death.z);
		}
		return new Spot(ground, yaw);
	}

	private static Vec3 findGroundNear(ServerLevel level, Vec3 death, int radius) {
		BlockPos center = BlockPos.containing(death);
		Vec3 best = null;
		double bestDist = Double.MAX_VALUE;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				BlockPos column = new BlockPos(center.getX() + dx, 0, center.getZ() + dz);
				int top = Math.min(level.getMaxY() - 2, center.getY() + 2);
				int bottom = Math.max(level.getMinY() + 1, center.getY() - 10);
				for (int y = top; y >= bottom; y--) {
					BlockPos ground = new BlockPos(column.getX(), y, column.getZ());
					if (isSafeGround(level, ground)) {
						double dist = ground.distSqr(center);
						if (dist < bestDist) {
							bestDist = dist;
							best = new Vec3(ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5);
						}
						break;
					}
				}
			}
		}
		return best;
	}

	private static Vec3 findSurface(ServerLevel level, Vec3 death, int radius) {
		BlockPos center = BlockPos.containing(death);
		Vec3 best = null;
		double bestDist = Double.MAX_VALUE;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				BlockPos column = new BlockPos(center.getX() + dx, 0, center.getZ() + dz);
				if (!level.isLoaded(column)) {
					continue;
				}
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
				BlockPos ground = new BlockPos(column.getX(), y - 1, column.getZ());
				if (isSafeGround(level, ground)) {
					double dist = ground.distSqr(center);
					if (dist < bestDist) {
						bestDist = dist;
						best = new Vec3(ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5);
					}
				}
			}
		}
		return best;
	}

	private static boolean isSafeGround(ServerLevel level, BlockPos ground) {
		BlockState state = level.getBlockState(ground);
		if (state.isAir() || !state.isSolid() || !state.getFluidState().isEmpty()) {
			return false;
		}
		if (state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.MAGMA_BLOCK)) {
			return false;
		}
		BlockPos above = ground.above();
		BlockPos above2 = above.above();
		return level.getBlockState(above).isAir() && level.getBlockState(above2).isAir();
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	public static Vec3 forward(float yaw) {
		double radians = Math.toRadians(yaw);
		return new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
	}

	public static Pose parsePose(String name, String fallback) {
		String value = name == null || name.isBlank() ? fallback : name;
		if (value != null) {
			switch (value.trim().toUpperCase()) {
				case "SLEEPING" -> {
					return Pose.SLEEPING;
				}
				case "SWIMMING" -> {
					return Pose.SWIMMING;
				}
				case "CROUCHING", "SNEAKING" -> {
					return Pose.CROUCHING;
				}
				case "FALL_FLYING", "ELYTRA" -> {
					return Pose.FALL_FLYING;
				}
				default -> {
					return Pose.STANDING;
				}
			}
		}
		return Pose.STANDING;
	}

	public static GraveSavedData getData(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(GraveSavedData.TYPE);
	}

	public static ServerLevel levelOf(MinecraftServer server, GraveRecord record) {
		try {
			Identifier id = Identifier.parse(record.dimension());
			return server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
		} catch (Exception e) {
			return null;
		}
	}

	public static List<GraveRecord> gravesOf(MinecraftServer server, UUID owner) {
		List<GraveRecord> result = new ArrayList<>();
		for (GraveRecord record : getData(server).snapshot()) {
			if (record.ownerId().equals(owner)) {
				result.add(record);
			}
		}
		result.sort(Comparator.comparingLong(GraveRecord::createdAt));
		return result;
	}

	public static GraveRecord findNearest(MinecraftServer server, ServerPlayer player) {
		GraveRecord best = null;
		double bestDistance = Double.MAX_VALUE;
		for (GraveRecord record : gravesOf(server, player.getUUID())) {
			if (!record.dimension().equals(player.level().dimension().identifier().toString())) {
				continue;
			}
			double distance = record.pos().distanceToSqr(player.position());
			if (distance < bestDistance) {
				bestDistance = distance;
				best = record;
			}
		}
		return best;
	}

	public static boolean isModded(ServerPlayer player) {
		return GraveProtocol.isModded(player);
	}

	public static void clearAnnouncements(GraveRecord record) {
		UNLOCK_ANNOUNCED.remove(record.graveId());
	}
}

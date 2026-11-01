package com.kador.corpsegraves.grave;

import com.kador.corpsegraves.CorpseGraves;
import com.kador.corpsegraves.config.GraveConfig;
import com.kador.corpsegraves.network.GraveProtocol;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class GraveManager {
	private GraveManager() {
	}

	public static void handleDeath(ServerPlayer player, ServerLevel level) {
		GraveConfig config = GraveConfig.get();
		if (!config.enabled || player instanceof FakePlayer || player.isSpectator()) {
			return;
		}
		if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
			return;
		}
		Inventory inventory = player.getInventory();
		List<ItemStack> stored = new ArrayList<>();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty()) continue;
			if (config.respectCurseOfVanishing
				&& EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
				continue;
			}
			stored.add(stack.copy());
		}
		int xp = config.storeXp ? totalExperience(player) : 0;
		if (stored.isEmpty() && xp <= 0) {
			if (config.storeXp) zeroExperience(player);
			return;
		}
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			inventory.setItem(slot, ItemStack.EMPTY);
		}
		if (config.storeXp) zeroExperience(player);

		Vec3 pos = findSpot(level, player.position());
		CorpseEntity corpse = new CorpseEntity(CorpseGraves.CORPSE, level);
		if (corpse == null) {
			dropDirect(level, pos, stored);
			return;
		}
		corpse.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0F);
		corpse.initialize(player.getUUID(), player.getName().getString(), xp,
			config.unlockSeconds < 0 ? -1 : config.unlockSeconds * 20,
			config.despawnSeconds < 0 ? -1 : config.despawnSeconds * 20);
		for (int i = 0; i < stored.size(); i++) {
			corpse.items().set(i, stored.get(i));
		}
		level.addFreshEntity(corpse);
		if (config.announceCoordinates) {
			player.sendSystemMessage(Component.translatable("corpsegraves.message.created",
				(int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z))
				.withStyle(style -> style.withColor(ChatFormatting.GRAY)));
		}
		CorpseGraves.LOGGER.info("Created grave for {} with {} stacks", player.getName().getString(), stored.size());
	}

	public static boolean isLockedFor(CorpseEntity corpse, Player player) {
		GraveConfig config = GraveConfig.get();
		if (config.ownedGravesAlwaysAccessible && player.getUUID().equals(corpse.ownerId())) {
			return false;
		}
		if (config.operatorsBypassProtection && player.hasPermissions(2)) {
			return false;
		}
		return corpse.unlockTicksLeft != 0;
	}

	public static void sendLockedMessage(ServerPlayer player, CorpseEntity corpse) {
		MutableComponent message;
		if (corpse.unlockTicksLeft < 0) {
			message = Component.translatable("corpsegraves.message.locked_forever");
		} else {
			message = Component.translatable("corpsegraves.message.locked",
				formatTime((int) Math.ceil(corpse.unlockTicksLeft / 20.0)));
		}
		player.displayClientMessage(message.withStyle(style -> style.withColor(ChatFormatting.RED)), true);
	}

	public static void openMenu(ServerPlayer player, CorpseEntity corpse) {
		if (player.containerMenu != player.inventoryMenu) {
			return;
		}
		player.openMenu(new SimpleMenuProvider(
			(syncId, inventory, p) -> new GraveMenu(syncId, inventory, corpse),
			Component.translatable("corpsegraves.gui.title", corpse.ownerName())
		));
	}

	public static InteractionResult onUse(Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hitResult) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.PASS;
		}
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.SUCCESS;
		}
		CorpseEntity corpse = resolveCorpse(serverLevel, entity);
		if (corpse == null) {
			return InteractionResult.PASS;
		}
		if (isLockedFor(corpse, serverPlayer)) {
			sendLockedMessage(serverPlayer, corpse);
			return InteractionResult.SUCCESS;
		}
		if (serverPlayer.isShiftKeyDown()) {
			quickRestore(serverPlayer, corpse);
		} else if (GraveProtocol.isModded(serverPlayer)) {
			openMenu(serverPlayer, corpse);
		} else if (GraveConfig.get().vanillaClientQuickLoot) {
			takeAll(serverPlayer, corpse);
			serverPlayer.displayClientMessage(Component.translatable("corpsegraves.message.vanilla_client"), true);
		} else {
			serverPlayer.displayClientMessage(Component.translatable("corpsegraves.message.install_client"), true);
		}
		return InteractionResult.SUCCESS;
	}

	public static InteractionResult onAttack(Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hitResult) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.PASS;
		}
		CorpseEntity corpse = resolveCorpse(serverLevel, entity);
		if (corpse == null) {
			return InteractionResult.PASS;
		}
		if (isLockedFor(corpse, serverPlayer)) {
			sendLockedMessage(serverPlayer, corpse);
		}
		return InteractionResult.SUCCESS;
	}

	private static CorpseEntity resolveCorpse(ServerLevel level, Entity entity) {
		if (entity instanceof CorpseEntity corpse) {
			return corpse;
		}
		if (entity instanceof Interaction interaction) {
			for (ServerLevel candidateLevel : level.getServer().getAllLevels()) {
				for (Entity candidate : candidateLevel.getAllEntities()) {
					if (candidate instanceof CorpseEntity corpse
						&& interaction.getUUID().equals(corpse.hitboxId())) {
						return corpse;
					}
				}
			}
		}
		return null;
	}

	public static void quickRestore(ServerPlayer player, CorpseEntity corpse) {
		Inventory inventory = player.getInventory();
		boolean changed = false;
		for (int slot = 0; slot < corpse.getContainerSize(); slot++) {
			ItemStack stack = corpse.getItem(slot);
			if (stack.isEmpty()) continue;
			ItemStack current = inventory.getItem(slot);
			if (current.isEmpty()) {
				inventory.setItem(slot, stack.copy());
				corpse.setItem(slot, ItemStack.EMPTY);
				changed = true;
				continue;
			}
			if (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() < current.getMaxStackSize()) {
				int amount = Math.min(current.getMaxStackSize() - current.getCount(), stack.getCount());
				current.grow(amount);
				stack.shrink(amount);
				inventory.setItem(slot, current);
				corpse.setItem(slot, stack);
				changed = true;
			}
		}
		for (int slot = 0; slot < corpse.getContainerSize(); slot++) {
			ItemStack stack = corpse.getItem(slot);
			if (stack.isEmpty()) continue;
			ItemStack copy = stack.copy();
			inventory.add(copy);
			corpse.setItem(slot, copy.isEmpty() ? ItemStack.EMPTY : copy);
			changed = true;
		}
		player.containerMenu.broadcastChanges();
		if (corpse.isEmpty()) {
			corpse.finish(player);
		} else if (changed) {
			player.displayClientMessage(Component.translatable("corpsegraves.message.inventory_full", count(corpse)), true);
		}
	}

	public static void takeAll(ServerPlayer player, CorpseEntity corpse) {
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < corpse.getContainerSize(); slot++) {
			ItemStack stack = corpse.getItem(slot);
			if (stack.isEmpty()) continue;
			ItemStack copy = stack.copy();
			inventory.add(copy);
			corpse.setItem(slot, copy.isEmpty() ? ItemStack.EMPTY : copy);
		}
		player.containerMenu.broadcastChanges();
		if (corpse.isEmpty()) {
			corpse.finish(player);
		} else {
			player.displayClientMessage(Component.translatable("corpsegraves.message.inventory_full", count(corpse)), true);
		}
	}

	private static int count(CorpseEntity corpse) {
		int total = 0;
		for (int i = 0; i < corpse.getContainerSize(); i++) total += corpse.getItem(i).getCount();
		return total;
	}

	public static void removeCorpse(ServerPlayer player, CorpseEntity corpse, boolean drop) {
		if (drop) {
			corpse.dropAndDiscard(true);
		} else {
			corpse.dropAndDiscard(false);
		}
	}

	public static String formatTime(int seconds) {
		seconds = Math.max(0, seconds);
		int hours = seconds / 3600;
		int minutes = (seconds % 3600) / 60;
		int secs = seconds % 60;
		return hours > 0 ? String.format("%d:%02d:%02d", hours, minutes, secs)
			: String.format("%02d:%02d", minutes, secs);
	}

	public static int recoverableXp(CorpseEntity corpse) {
		GraveConfig config = GraveConfig.get();
		return Math.max(0, (int) Math.round(corpse.xp() * (config.xpRecoveryPercent / 100.0)));
	}

	private static int totalExperience(ServerPlayer player) {
		int level = player.experienceLevel;
		int needed = 0;
		for (int i = 0; i < level; i++) {
			needed += i >= 30 ? 112 + (i - 30) * 9 : i >= 15 ? 37 + (i - 15) * 5 : 7 + i * 2;
		}
		needed += Math.round(player.experienceProgress * (level >= 30 ? 112 + (level - 30) * 9 : level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2));
		return Math.max(player.totalExperience, needed);
	}

	private static void zeroExperience(ServerPlayer player) {
		player.experienceLevel = 0;
		player.experienceProgress = 0.0F;
		player.totalExperience = 0;
	}

	private static Vec3 findSpot(ServerLevel level, Vec3 death) {
		GraveConfig.VoidRescue cfg = GraveConfig.get().voidRescue;
		if (cfg.enabled && death.y < cfg.triggerBelowY) {
			Vec3 rescued = findSurface(level, death, cfg.searchRadius);
			return rescued != null ? rescued : new Vec3(death.x, level.getMinBuildHeight() + 5.0, death.z);
		}
		Vec3 ground = findGround(level, death, 3);
		return ground != null ? ground : new Vec3(death.x, Math.max(death.y, level.getMinBuildHeight() + 1.0), death.z);
	}

	private static Vec3 findGround(ServerLevel level, Vec3 death, int radius) {
		BlockPos center = BlockPos.containing(death);
		Vec3 best = null;
		double bestDist = Double.MAX_VALUE;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				BlockPos column = new BlockPos(center.getX() + dx, 0, center.getZ() + dz);
				int top = Math.min(level.getMaxBuildHeight() - 2, center.getY() + 2);
				int bottom = Math.max(level.getMinBuildHeight() + 1, center.getY() - 10);
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
				if (!level.isLoaded(column)) continue;
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
		if (state.isAir() || !state.isSolid() || !state.getFluidState().isEmpty()) return false;
		if (state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.MAGMA_BLOCK)) return false;
		return level.getBlockState(ground.above()).isAir() && level.getBlockState(ground.above(2)).isAir();
	}

	private static void dropDirect(ServerLevel level, Vec3 pos, List<ItemStack> items) {
		for (ItemStack stack : items) {
			if (stack.isEmpty()) continue;
			ItemEntity item = new ItemEntity(level, pos.x, pos.y + 0.35, pos.z, stack.copy());
			item.setPickUpDelay(20);
			level.addFreshEntity(item);
		}
	}

	public static List<CorpseEntity> allCorpses(MinecraftServer server) {
		List<CorpseEntity> result = new ArrayList<>();
		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (entity instanceof CorpseEntity corpse) {
					result.add(corpse);
				}
			}
		}
		return result;
	}

	public static List<CorpseEntity> corpsesOf(MinecraftServer server, UUID owner) {
		List<CorpseEntity> result = new ArrayList<>();
		for (CorpseEntity corpse : allCorpses(server)) {
			if (corpse.ownerId().equals(owner)) result.add(corpse);
		}
		return result;
	}

	public static CorpseEntity nearest(MinecraftServer server, ServerPlayer player) {
		CorpseEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (CorpseEntity corpse : corpsesOf(server, player.getUUID())) {
			if (corpse.level() != player.level()) continue;
			double dist = corpse.distanceToSqr(player);
			if (dist < bestDistance) {
				bestDistance = dist;
				best = corpse;
			}
		}
		return best;
	}
}

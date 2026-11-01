package com.kador.corpsegraves.grave;

import com.kador.corpsegraves.config.GraveConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class GraveMenu extends AbstractContainerMenu {
	public static final MenuType<GraveMenu> TYPE = new MenuType<>(GraveMenu::new, FeatureFlags.VANILLA_SET);
	private static final int[] SPECIAL = {39, 38, 37, 36, 40};
	private final @Nullable CorpseEntity corpse;
	private final @Nullable ServerPlayer viewer;
	private final SimpleContainerData data = new SimpleContainerData(6);

	public GraveMenu(int syncId, Inventory inventory) {
		this(syncId, inventory, null, null);
	}

	public GraveMenu(int syncId, Inventory inventory, CorpseEntity corpse) {
		this(syncId, inventory, corpse, inventory.player instanceof ServerPlayer serverPlayer ? serverPlayer : null);
	}

	private GraveMenu(int syncId, Inventory inventory, @Nullable CorpseEntity corpse, @Nullable ServerPlayer viewer) {
		super(TYPE, syncId);
		this.corpse = corpse;
		this.viewer = viewer;
		net.minecraft.world.Container container = corpse == null ? new SimpleContainer(CorpseEntity.SLOTS) : corpse;
		for (int i = 0; i < 5; i++) {
			final int storage = SPECIAL[i];
			addSlot(new Slot(container, storage, 8 + i * 18, 26) {
				@Override public boolean mayPlace(ItemStack stack) { return false; }
			});
		}
		for (int i = 0; i < 36; i++) {
			final int storage = i;
			addSlot(new Slot(container, storage, 8 + (i % 9) * 18, 68 + (i / 9) * 18) {
				@Override public boolean mayPlace(ItemStack stack) { return false; }
			});
		}
		for (int i = 0; i < 27; i++) {
			addSlot(new Slot(inventory, 9 + i, 8 + (i % 9) * 18, 158 + (i / 9) * 18));
		}
		for (int i = 0; i < 9; i++) {
			addSlot(new Slot(inventory, i, 8 + i * 18, 216));
		}
		addDataSlots(data);
	}

	@Override
	public void broadcastChanges() {
		if (corpse != null) {
			boolean locked = corpse.unlockTicksLeft != 0;
			data.set(0, locked ? 1 : 0);
			data.set(1, corpse.unlockTicksLeft < 0 ? -1 : (int) Math.ceil(corpse.unlockTicksLeft / 20.0));
			data.set(2, corpse.despawnTicksLeft < 0 ? -1 : (int) Math.ceil(corpse.despawnTicksLeft / 20.0));
			data.set(3, GraveManager.recoverableXp(corpse));
			data.set(4, itemCount());
			data.set(5, corpse.unlockTicksLeft < 0 ? 1 : 0);
			if (corpse.isEmpty() && GraveConfig.get().removeWhenEmpty && !corpse.isRemoved()) {
				corpse.finish(viewer);
			}
		}
		super.broadcastChanges();
	}

	private int itemCount() {
		if (corpse == null) return 0;
		int count = 0;
		for (int i = 0; i < corpse.getContainerSize(); i++) count += corpse.getItem(i).getCount();
		return count;
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (corpse == null || !(player instanceof ServerPlayer serverPlayer) || corpse.isRemoved()) {
			return false;
		}
		switch (buttonId) {
			case 0 -> GraveManager.takeAll(serverPlayer, corpse);
			case 1 -> GraveManager.quickRestore(serverPlayer, corpse);
			default -> {
				return false;
			}
		}
		if (corpse.isRemoved()) {
			serverPlayer.closeContainer();
		}
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack result = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack stack = slot.getItem();
			result = stack.copy();
			if (index < 41) {
				if (!this.moveItemStackTo(stack, 41, 41 + 36, true)) {
					return ItemStack.EMPTY;
				}
			} else {
				return ItemStack.EMPTY;
			}
			if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
			else slot.setChanged();
		}
		return result;
	}

	@Override
	public boolean stillValid(Player player) {
		if (corpse == null) return true;
		return !corpse.isRemoved() && corpse.distanceToSqr(player) <= 64.0;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
	}

	public boolean locked() { return data.get(0) != 0; }
	public int protectionSeconds() { return data.get(1); }
	public int despawnSeconds() { return data.get(2); }
	public int storedXp() { return data.get(3); }
	public int storedItemCount() { return data.get(4); }
	public boolean protectedForever() { return data.get(5) != 0; }
}

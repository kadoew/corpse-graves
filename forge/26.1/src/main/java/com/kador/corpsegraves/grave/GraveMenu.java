package com.kador.corpsegraves.grave;

import com.kador.corpsegraves.config.GraveConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class GraveMenu extends AbstractContainerMenu {
	public static final int GRAVE_SLOTS = 43;
	public static final int PLAYER_SLOTS = 36;
	public static final MenuType<GraveMenu> TYPE = new MenuType<>(GraveMenu::new, FeatureFlags.VANILLA_SET);

	private static final int[] STORAGE_ORDER = {
		39, 38, 37, 36, 40, 41, 42,
		0, 1, 2, 3, 4, 5, 6, 7, 8,
		9, 10, 11, 12, 13, 14, 15, 16, 17,
		18, 19, 20, 21, 22, 23, 24, 25, 26,
		27, 28, 29, 30, 31, 32, 33, 34, 35
	};

	private final @Nullable GraveRecord record;
	private final @Nullable ServerPlayer viewer;
	private final SimpleContainerData syncData = new SimpleContainerData(6);

	public GraveMenu(int syncId, Inventory playerInventory) {
		this(syncId, playerInventory, null, null);
	}

	public GraveMenu(int syncId, Inventory playerInventory, GraveRecord record) {
		this(syncId, playerInventory, record, playerInventory.player instanceof ServerPlayer serverPlayer ? serverPlayer : null);
	}

	private GraveMenu(int syncId, Inventory playerInventory, @Nullable GraveRecord record, @Nullable ServerPlayer viewer) {
		super(TYPE, syncId);
		this.record = record;
		this.viewer = viewer;
		Container container = record == null ? new net.minecraft.world.SimpleContainer(GRAVE_SLOTS) : new GraveContainer(record);

		for (int i = 0; i < 7; i++) {
			addSlot(new GraveSlot(container, STORAGE_ORDER[i], 8 + i * 18, 26));
		}
		for (int i = 0; i < 36; i++) {
			int col = i % 9;
			int row = i / 9;
			addSlot(new GraveSlot(container, STORAGE_ORDER[7 + i], 8 + col * 18, 68 + row * 18));
		}
		for (int i = 0; i < 27; i++) {
			int col = i % 9;
			int row = i / 9;
			addSlot(new Slot(playerInventory, 9 + i, 8 + col * 18, 158 + row * 18));
		}
		for (int i = 0; i < 9; i++) {
			addSlot(new Slot(playerInventory, i, 8 + i * 18, 216));
		}
		addDataSlots(syncData);
	}

	@Override
	public void broadcastChanges() {
		if (record != null) {
			serverSync();
		}
		super.broadcastChanges();
	}

	private void serverSync() {
		boolean locked = record.unlockTicksLeft != 0;
		int unlockSeconds = record.unlockTicksLeft < 0 ? -1 : (int) Math.ceil(record.unlockTicksLeft / 20.0);
		int despawnSeconds = record.despawnTicksLeft < 0 ? -1 : (int) Math.ceil(record.despawnTicksLeft / 20.0);
		syncData.set(0, locked ? 1 : 0);
		syncData.set(1, unlockSeconds);
		syncData.set(2, despawnSeconds);
		syncData.set(3, GraveManager.recoverableXp(record));
		syncData.set(4, record.itemCount());
		syncData.set(5, record.unlockTicksLeft < 0 ? 1 : 0);
		if (viewer != null && viewer.level().getServer() != null) {
			GraveSavedData data = GraveManager.getData(viewer.level().getServer());
			if (!data.contains(record.graveId())) {
				return;
			}
			data.setDirty();
			if (record.isEmpty() && GraveConfig.get().removeWhenEmpty) {
				GraveManager.finish(viewer.level().getServer(), data, record, viewer);
			}
		}
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (record == null || !(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		if (!stillValid(player)) {
			return false;
		}
		handleClientAction(serverPlayer, buttonId);
		return true;
	}

	public void handleClientAction(ServerPlayer player, int action) {
		if (record == null || !stillValid(player)) {
			return;
		}
		switch (action) {
			case 0 -> GraveManager.takeAll(player, record);
			case 1 -> GraveManager.quickRestore(player, record);
			default -> {
				return;
			}
		}
		if (player.level().getServer() != null
			&& !GraveManager.getData(player.level().getServer()).contains(record.graveId())) {
			player.closeContainer();
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack result = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack stack = slot.getItem();
			result = stack.copy();
			if (index < GRAVE_SLOTS) {
				if (!this.moveItemStackTo(stack, GRAVE_SLOTS, GRAVE_SLOTS + PLAYER_SLOTS, true)) {
					return ItemStack.EMPTY;
				}
			} else {
				return ItemStack.EMPTY;
			}
			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}
		return result;
	}

	@Override
	public boolean stillValid(Player player) {
		if (record == null) {
			return true;
		}
		if (!player.level().dimension().identifier().toString().equals(record.dimension())) {
			return false;
		}
		if (player.level().getServer() != null
			&& !GraveManager.getData(player.level().getServer()).contains(record.graveId())) {
			return false;
		}
		return record.pos().distanceToSqr(player.position()) <= 64.0;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		if (record != null && player.level().getServer() != null) {
			GraveManager.getData(player.level().getServer()).setDirty();
		}
	}

	public boolean locked() {
		return syncData.get(0) != 0;
	}

	public int protectionSeconds() {
		return syncData.get(1);
	}

	public int despawnSeconds() {
		return syncData.get(2);
	}

	public int storedXp() {
		return syncData.get(3);
	}

	public int storedItemCount() {
		return syncData.get(4);
	}

	public boolean protectedForever() {
		return syncData.get(5) != 0;
	}

	public @Nullable GraveRecord record() {
		return record;
	}

	private static final class GraveSlot extends Slot {
		private GraveSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}
	}

	/**
	 * Live view over the persisted record; the record remains the single
	 * source of truth so nothing is lost when the menu closes.
	 */
	private static final class GraveContainer implements Container {
		private final GraveRecord record;

		private GraveContainer(GraveRecord record) {
			this.record = record;
		}

		@Override
		public int getContainerSize() {
			return GRAVE_SLOTS;
		}

		@Override
		public boolean isEmpty() {
			return record.isEmpty();
		}

		@Override
		public ItemStack getItem(int slot) {
			for (GraveRecord.StoredItem item : record.items()) {
				if (item.slot() == slot) {
					return item.stack();
				}
			}
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack removeItem(int slot, int amount) {
			ItemStack current = getItem(slot);
			if (current.isEmpty() || amount <= 0) {
				return ItemStack.EMPTY;
			}
			ItemStack split = current.split(amount);
			if (current.isEmpty()) {
				removeItemNoUpdate(slot);
			}
			setChanged();
			return split;
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			for (int i = 0; i < record.items().size(); i++) {
				GraveRecord.StoredItem item = record.items().get(i);
				if (item.slot() == slot) {
					record.items().remove(i);
					return item.stack();
				}
			}
			return ItemStack.EMPTY;
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			if (stack.isEmpty()) {
				removeItemNoUpdate(slot);
				return;
			}
			for (int i = 0; i < record.items().size(); i++) {
				if (record.items().get(i).slot() == slot) {
					record.items().set(i, new GraveRecord.StoredItem(slot, stack));
					setChanged();
					return;
				}
			}
			record.items().add(new GraveRecord.StoredItem(slot, stack));
			setChanged();
		}

		@Override
		public void setChanged() {
		}

		@Override
		public boolean stillValid(Player player) {
			return true;
		}

		@Override
		public void clearContent() {
			record.items().clear();
			setChanged();
		}
	}
}

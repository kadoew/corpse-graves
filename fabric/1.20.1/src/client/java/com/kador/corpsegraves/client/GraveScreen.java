package com.kador.corpsegraves.client;

import com.kador.corpsegraves.CorpseGraves;
import com.kador.corpsegraves.grave.GraveManager;
import com.kador.corpsegraves.grave.GraveMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GraveScreen extends AbstractContainerScreen<GraveMenu> {
	private static final ResourceLocation TEXTURE = new ResourceLocation(CorpseGraves.MOD_ID, "textures/gui/grave.png");
	private static final int PANEL_HEIGHT = 240;
	private static final int COLOR_GOLD = 0xFFFFC75A;
	private static final int COLOR_MUTED = 0xFF94A0B5;
	private static final int COLOR_LOCKED = 0xFFFF6B6B;
	private static final int COLOR_UNLOCKED = 0xFF6BFF8E;

	public GraveScreen(GraveMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = PANEL_HEIGHT;
		this.inventoryLabelY = 9999;
	}

	private final class GraveButton extends Button {
		private GraveButton(int x, int y, int width, int height, Component label, OnPress press) {
			super(x, y, width, height, label, press, narration -> narration.get());
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			boolean hovered = this.isHoveredOrFocused();
			int top = hovered ? 0xFF4A3E63 : 0xFF2E2A3B;
			int bottom = hovered ? 0xFF332B45 : 0xFF211E2C;
			graphics.fillGradient(this.getX() + 1, this.getY() + 1,
				this.getX() + this.getWidth() - 1, this.getY() + this.getHeight() - 1, top, bottom);
			graphics.renderOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(),
				hovered ? 0xFFD7A94B : 0xFF5A516E);
			Component label = this.getMessage();
			int textX = this.getX() + (this.getWidth() - GraveScreen.this.font.width(label)) / 2;
			int textY = this.getY() + (this.getHeight() - 8) / 2 + 1;
			graphics.drawString(GraveScreen.this.font, label, textX, textY,
				hovered ? 0xFFFFE6A8 : 0xFFD9C7A5, false);
		}
	}

	@Override
	protected void init() {
		super.init();
		this.addRenderableWidget(new GraveButton(this.leftPos + 8, this.topPos + 48, 76, 18,
			Component.translatable("corpsegraves.gui.take_all"), button -> click(0)));
		this.addRenderableWidget(new GraveButton(this.leftPos + 92, this.topPos + 48, 76, 18,
			Component.translatable("corpsegraves.gui.restore"), button -> click(1)));
	}

	private void click(int id) {
		if (this.minecraft != null && this.minecraft.gameMode != null) {
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
		}
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, this.title, 10, 7, COLOR_GOLD, false);

		int despawn = this.menu.despawnSeconds();
		Component despawnText = despawn < 0
			? Component.translatable("corpsegraves.gui.despawns_never")
			: Component.translatable("corpsegraves.gui.despawns", formatSeconds(despawn));
		graphics.drawString(this.font, despawnText, this.imageWidth - 8 - this.font.width(despawnText), 17, 0xFF6FE4E8, false);

		Component xp = Component.translatable("corpsegraves.gui.xp", this.menu.storedXp());
		graphics.drawString(this.font, xp, 8, 146, COLOR_MUTED, false);

		Component status;
		int statusColor;
		if (this.menu.locked()) {
			if (this.menu.protectedForever()) {
				status = Component.translatable("corpsegraves.gui.owner_only");
			} else {
				status = Component.translatable("corpsegraves.gui.protected", formatSeconds(this.menu.protectionSeconds()));
			}
			statusColor = COLOR_LOCKED;
		} else {
			status = Component.translatable("corpsegraves.gui.unlocked");
			statusColor = COLOR_UNLOCKED;
		}
		graphics.drawString(this.font, status, this.imageWidth - 8 - this.font.width(status), 146, statusColor, false);
	}

	private static String formatSeconds(int seconds) {
		return GraveManager.formatTime(Math.max(0, seconds));
	}
}

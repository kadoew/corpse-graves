package com.kador.corpsegraves.client;

import com.kador.corpsegraves.CorpseGraves;
import com.kador.corpsegraves.grave.GraveManager;
import com.kador.corpsegraves.grave.GraveMenu;
import com.kador.corpsegraves.network.GraveActionC2S;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.GuiPipeline;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class GraveScreen extends AbstractContainerScreen<GraveMenu> {
	private static final Identifier TEXTURE = CorpseGraves.id("textures/gui/grave.png");
	private static final int PANEL_HEIGHT = 240;
	private static final int COLOR_GOLD = 0xFFFFC75A;
	private static final int COLOR_MUTED = 0xFF94A0B5;
	private static final int COLOR_LOCKED = 0xFFFF6B6B;
	private static final int COLOR_UNLOCKED = 0xFF6BFF8E;

	private static final int BTN_TAKE_X = 8;
	private static final int BTN_RESTORE_X = 92;
	private static final int BTN_Y = 48;
	private static final int BTN_W = 76;
	private static final int BTN_H = 18;

	public GraveScreen(GraveMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, PANEL_HEIGHT);
		this.inventoryLabelY = 9999;
	}

	@Override
	protected void init() {
		super.init();
		this.addRenderableWidget(new GraveButton(this.leftPos + BTN_TAKE_X, this.topPos + BTN_Y, BTN_W, BTN_H,
			Component.translatable("corpsegraves.gui.take_all"), b -> sendAction(0)));
		this.addRenderableWidget(new GraveButton(this.leftPos + BTN_RESTORE_X, this.topPos + BTN_Y, BTN_W, BTN_H,
			Component.translatable("corpsegraves.gui.restore"), b -> sendAction(1)));
	}

	private void sendAction(int action) {
		ClientPlayNetworking.send(new GraveActionC2S(this.menu.containerId, action));
	}

	private final class GraveButton extends Button {
		private GraveButton(int x, int y, int width, int height, Component label, OnPress press) {
			super(x, y, width, height, label, press, narration -> narration.get());
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics) {
			boolean hovered = this.isHoveredOrFocused();
			int top = hovered ? 0xFF4A3E63 : 0xFF2E2A3B;
			int bottom = hovered ? 0xFF332B45 : 0xFF211E2C;
			graphics.fillGradient(this.getX() + 1, this.getY() + 1,
				this.getX() + this.getWidth() - 1, this.getY() + this.getHeight() - 1, top, bottom);
			graphics.outline(this.getX(), this.getY(), this.getWidth(), this.getHeight(),
				hovered ? 0xFFD7A94B : 0xFF5A516E);
			Component label = this.getMessage();
			int textX = this.getX() + (this.getWidth() - GraveScreen.this.font.width(label)) / 2;
			int textY = this.getY() + (this.getHeight() - 8) / 2 + 1;
			graphics.text(GraveScreen.this.font, label, textX, textY,
				hovered ? 0xFFFFE6A8 : 0xFFD9C7A5, false);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics) {
		super.extractBackground(graphics);
		graphics.blit(GuiPipeline.TEXTURED, TEXTURE, this.leftPos, this.topPos,
			0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics) {
		int despawn = this.menu.despawnSeconds();
		Component despawnText = despawn < 0
			? Component.translatable("corpsegraves.gui.despawns_never")
			: Component.translatable("corpsegraves.gui.despawns", formatSeconds(despawn));
		int despawnWidth = this.font.width(despawnText);
		Component title = this.title;
		int maxTitleWidth = Math.max(0, this.imageWidth - 18 - despawnWidth - 6);
		if (this.font.width(title) > maxTitleWidth) {
			String text = title.getString();
			while (!text.isEmpty() && this.font.width(Component.literal(text + "...")) > maxTitleWidth) {
				text = text.substring(0, text.length() - 1);
			}
			title = Component.literal(text + "...");
		}
		graphics.text(this.font, title, 10, 7, COLOR_GOLD, false);
		graphics.text(this.font, despawnText, this.imageWidth - 8 - despawnWidth, 7, 0xFF6FE4E8, false);

		Component xp = Component.translatable("corpsegraves.gui.xp", this.menu.storedXp());
		graphics.text(this.font, xp, 8, 146, COLOR_MUTED, false);

		Component status;
		int statusColor;
		if (this.menu.locked()) {
			if (this.menu.protectedForever()) {
				status = Component.translatable("corpsegraves.gui.owner_only");
			} else {
				status = Component.translatable("corpsegraves.gui.protected",
					formatSeconds(this.menu.protectionSeconds()));
			}
			statusColor = COLOR_LOCKED;
		} else {
			status = Component.translatable("corpsegraves.gui.unlocked");
			statusColor = COLOR_UNLOCKED;
		}
		graphics.text(this.font, status, this.imageWidth - 8 - this.font.width(status), 146, statusColor, false);
	}

	private static String formatSeconds(int seconds) {
		return GraveManager.formatTime(Math.max(0, seconds));
	}
}

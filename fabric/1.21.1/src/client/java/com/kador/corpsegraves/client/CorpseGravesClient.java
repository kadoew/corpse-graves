package com.kador.corpsegraves.client;

import com.kador.corpsegraves.CorpseGraves;
import com.kador.corpsegraves.grave.GraveMenu;
import com.kador.corpsegraves.network.GraveProtocol;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public class CorpseGravesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(CorpseGraves.CORPSE, CorpseEntityRenderer::new);
		MenuScreens.register(GraveMenu.TYPE, GraveScreen::new);
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
			ClientPlayNetworking.send(GraveProtocol.HelloC2S.INSTANCE));
	}
}

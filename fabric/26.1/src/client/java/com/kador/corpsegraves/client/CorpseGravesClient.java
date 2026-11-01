package com.kador.corpsegraves.client;

import com.kador.corpsegraves.grave.GraveMenu;
import com.kador.corpsegraves.network.GraveProtocol;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;

public class CorpseGravesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(GraveMenu.TYPE, GraveScreen::new);
		ClientPlayNetworking.registerGlobalReceiver(GraveProtocol.HelloS2C.TYPE, (payload, context) -> {
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
			ClientPlayNetworking.send(new GraveProtocol.HelloC2S()));
	}
}

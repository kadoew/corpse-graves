package com.kador.corpsegraves.client;

import com.kador.corpsegraves.CorpseGraves;
import com.kador.corpsegraves.grave.GraveMenu;
import com.kador.corpsegraves.network.GraveProtocol;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

public final class CorpseGravesClient {
	private CorpseGravesClient() {
	}

	public static void register(IEventBus modBus) {
		modBus.addListener(CorpseGravesClient::screens);
		NeoForge.EVENT_BUS.addListener(CorpseGravesClient::login);
	}

	private static void screens(RegisterMenuScreensEvent event) {
		event.register(GraveMenu.TYPE, GraveScreen::new);
	}

	private static void login(ClientPlayerNetworkEvent.LoggingIn event) {
		ClientPacketDistributor.sendToServer(new GraveProtocol.HelloC2S());
	}
}

package com.kador.corpsegraves.client;

import com.kador.corpsegraves.CorpseGraves;
import com.kador.corpsegraves.network.GraveProtocol;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CorpseGravesClient {
	private CorpseGravesClient() {
	}

	@EventBusSubscriber(modid = CorpseGraves.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static final class ModEvents {
		@SubscribeEvent
		public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
			event.registerEntityRenderer(CorpseGraves.CORPSE.get(), CorpseEntityRenderer::new);
		}

		@SubscribeEvent
		public static void screens(RegisterMenuScreensEvent event) {
			event.register(CorpseGraves.MENU.get(), GraveScreen::new);
		}
	}

	@EventBusSubscriber(modid = CorpseGraves.MOD_ID, value = Dist.CLIENT)
	public static final class GameEvents {
		@SubscribeEvent
		public static void login(ClientPlayerNetworkEvent.LoggingIn event) {
			PacketDistributor.sendToServer(GraveProtocol.HelloC2S.INSTANCE);
		}
	}
}

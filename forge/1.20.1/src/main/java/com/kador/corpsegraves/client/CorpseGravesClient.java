package com.kador.corpsegraves.client;

import com.kador.corpsegraves.CorpseGraves;
import com.kador.corpsegraves.network.GraveProtocol;
import com.kador.corpsegraves.network.HelloC2S;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CorpseGraves.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CorpseGravesClient {
	private CorpseGravesClient() {
	}

	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(CorpseGraves.CORPSE.get(), CorpseEntityRenderer::new);
	}

	@SubscribeEvent
	public static void registerScreens(FMLClientSetupEvent event) {
		event.enqueueWork(() -> MenuScreens.register(CorpseGraves.MENU.get(), GraveScreen::new));
	}

	@Mod.EventBusSubscriber(modid = CorpseGraves.MOD_ID, value = Dist.CLIENT)
	public static final class GameEvents {
		@SubscribeEvent
		static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
			GraveProtocol.CHANNEL.sendToServer(new HelloC2S());
		}
	}
}

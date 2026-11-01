package com.kador.corpsegraves.client;

import com.kador.corpsegraves.CorpseGraves;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CorpseGraves.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CorpseGravesClient {
	private CorpseGravesClient() {
	}

	@SubscribeEvent
	public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(CorpseGraves.CORPSE.get(), CorpseEntityRenderer::new);
	}

	@SubscribeEvent
	public static void screens(FMLClientSetupEvent event) {
		event.enqueueWork(() -> MenuScreens.register(CorpseGraves.MENU.get(), GraveScreen::new));
	}
}

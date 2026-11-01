package com.kador.corpsegraves.client;

import com.kador.corpsegraves.CorpseGraves;
import com.kador.corpsegraves.grave.GraveMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CorpseGraves.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CorpseGravesClient {
	private CorpseGravesClient() {
	}

	@SubscribeEvent
	public static void setup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> MenuScreens.register(GraveMenu.TYPE, GraveScreen::new));
	}
}

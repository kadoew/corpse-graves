package com.kador.corpsegraves;

import com.kador.corpsegraves.command.GraveCommands;
import com.kador.corpsegraves.config.GraveConfig;
import com.kador.corpsegraves.grave.GraveManager;
import com.kador.corpsegraves.grave.GraveMenu;
import com.kador.corpsegraves.network.GraveProtocol;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorpseGraves implements ModInitializer {
	public static final String MOD_ID = "corpsegraves";
	public static final String VERSION = "1.1.0";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		GraveConfig.load();
		Registry.register(BuiltInRegistries.MENU, id("grave"), GraveMenu.TYPE);
		GraveProtocol.registerCommon();

		UseEntityCallback.EVENT.register(GraveManager::onUse);
		AttackEntityCallback.EVENT.register(GraveManager::onAttack);
		ServerTickEvents.END_SERVER_TICK.register(GraveManager::tick);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			GraveConfig.load();
			GraveManager.refreshHolograms(server);
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> GraveProtocol.remove(handler.player));
		GraveCommands.register();

		LOGGER.info("Corpse Graves {} initialized for Minecraft 26.3", VERSION);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}

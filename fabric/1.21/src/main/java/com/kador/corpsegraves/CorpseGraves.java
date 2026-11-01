package com.kador.corpsegraves;

import com.kador.corpsegraves.command.GraveCommands;
import com.kador.corpsegraves.config.GraveConfig;
import com.kador.corpsegraves.grave.CorpseEntity;
import com.kador.corpsegraves.grave.GraveManager;
import com.kador.corpsegraves.grave.GraveMenu;
import com.kador.corpsegraves.network.GraveProtocol;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorpseGraves implements ModInitializer {
	public static final String MOD_ID = "corpsegraves";
	public static final String VERSION = "1.1.0";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final EntityType<CorpseEntity> CORPSE = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		id("corpse"),
		EntityType.Builder.of(CorpseEntity::new, MobCategory.MISC)
			.sized(0.6F, 0.2F)
			.eyeHeight(0.1F)
			.clientTrackingRange(10)
			.updateInterval(2)
			.build("corpsegraves:corpse")
	);

	public static final MenuType<GraveMenu> MENU = Registry.register(
		BuiltInRegistries.MENU,
		id("grave"),
		GraveMenu.TYPE
	);

	@Override
	public void onInitialize() {
		GraveConfig.load();
		GraveProtocol.registerCommon();
		FabricDefaultAttributeRegistry.register(CORPSE, CorpseEntity.createAttributes());
		UseEntityCallback.EVENT.register(GraveManager::onUse);
		AttackEntityCallback.EVENT.register(GraveManager::onAttack);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> GraveConfig.load());
		GraveCommands.register();
		LOGGER.info("Corpse Graves {} initialized for Minecraft 1.21", VERSION);
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}

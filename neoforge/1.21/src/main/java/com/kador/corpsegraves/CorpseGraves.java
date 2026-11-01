package com.kador.corpsegraves;

import com.kador.corpsegraves.command.GraveCommands;
import com.kador.corpsegraves.config.GraveConfig;
import com.kador.corpsegraves.grave.CorpseEntity;
import com.kador.corpsegraves.grave.GraveManager;
import com.kador.corpsegraves.grave.GraveMenu;
import com.kador.corpsegraves.network.GraveProtocol;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CorpseGraves.MOD_ID)
public class CorpseGraves {
	public static final String MOD_ID = "corpsegraves";
	public static final String VERSION = "1.1.0";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);

	public static final DeferredHolder<EntityType<?>, EntityType<CorpseEntity>> CORPSE = ENTITIES.register("corpse",
		() -> EntityType.Builder.of(CorpseEntity::new, MobCategory.MISC)
			.sized(0.6F, 0.2F)
			.eyeHeight(0.1F)
			.clientTrackingRange(10)
			.updateInterval(2)
			.build("corpsegraves:corpse"));

	public static final DeferredHolder<MenuType<?>, MenuType<GraveMenu>> MENU = MENUS.register("grave",
		() -> new MenuType<>(GraveMenu::new, FeatureFlags.VANILLA_SET));

	public CorpseGraves(IEventBus modBus, ModContainer container) {
		GraveConfig.load();
		ENTITIES.register(modBus);
		MENUS.register(modBus);
		modBus.addListener(this::attributes);
		modBus.addListener(GraveProtocol::register);
		NeoForge.EVENT_BUS.addListener(this::death);
		NeoForge.EVENT_BUS.addListener(this::interact);
		NeoForge.EVENT_BUS.addListener(this::attack);
		NeoForge.EVENT_BUS.addListener(this::commands);
		NeoForge.EVENT_BUS.addListener(this::serverStarted);
		LOGGER.info("Corpse Graves {} initialized for NeoForge 1.21", VERSION);
	}

	private void attributes(EntityAttributeCreationEvent event) {
		event.put(CORPSE.get(), CorpseEntity.createAttributes().build());
	}

	private void death(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
			GraveManager.handleDeath(player, level);
		}
	}

	private void interact(PlayerInteractEvent.EntityInteract event) {
		InteractionResult result = GraveManager.onUse(event.getEntity(), event.getLevel(), event.getHand(), event.getTarget(), null);
		if (result != InteractionResult.PASS) {
			event.setCancellationResult(result);
			event.setCanceled(true);
		}
	}

	private void attack(AttackEntityEvent event) {
		InteractionResult result = GraveManager.onAttack(event.getEntity(), event.getEntity().level(), InteractionHand.MAIN_HAND, event.getTarget(), null);
		if (result != InteractionResult.PASS) {
			event.setCanceled(true);
		}
	}

	private void commands(RegisterCommandsEvent event) {
		GraveCommands.register(event.getDispatcher());
	}

	private void serverStarted(ServerStartedEvent event) {
		GraveConfig.load();
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}

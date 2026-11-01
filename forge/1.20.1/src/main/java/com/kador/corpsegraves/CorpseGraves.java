package com.kador.corpsegraves;

import com.kador.corpsegraves.command.GraveCommands;
import com.kador.corpsegraves.config.GraveConfig;
import com.kador.corpsegraves.grave.CorpseEntity;
import com.kador.corpsegraves.grave.GraveManager;
import com.kador.corpsegraves.grave.GraveMenu;
import com.kador.corpsegraves.network.GraveProtocol;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CorpseGraves.MOD_ID)
public class CorpseGraves {
	public static final String MOD_ID = "corpsegraves";
	public static final String VERSION = "1.1.0";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
		DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
	public static final DeferredRegister<MenuType<?>> MENU_TYPES =
		DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);

	public static final RegistryObject<EntityType<CorpseEntity>> CORPSE = ENTITY_TYPES.register("corpse",
		() -> EntityType.Builder.<CorpseEntity>of(CorpseEntity::new, MobCategory.MISC)
			.sized(0.6F, 0.2F)
			.clientTrackingRange(10)
			.updateInterval(2)
			.build(id("corpse").toString()));

	public static final RegistryObject<MenuType<GraveMenu>> MENU = MENU_TYPES.register("grave",
		() -> new MenuType<>(GraveMenu::new, FeatureFlags.VANILLA_SET));

	public CorpseGraves() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		ENTITY_TYPES.register(modBus);
		MENU_TYPES.register(modBus);
		modBus.addListener(this::onAttributes);
		MinecraftForge.EVENT_BUS.register(this);
		GraveProtocol.init();
		GraveConfig.load();
		LOGGER.info("Corpse Graves {} initialized for Minecraft 1.20.1 (Forge)", VERSION);
	}

	private void onAttributes(EntityAttributeCreationEvent event) {
		event.put(CORPSE.get(), CorpseEntity.createAttributes().build());
	}

	@SubscribeEvent
	public void onLivingDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
			try {
				GraveManager.handleDeath(player, level);
			} catch (Throwable t) {
				LOGGER.error("Grave death handling failed for {}", player.getName().getString(), t);
			}
		}
	}

	@SubscribeEvent
	public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
			if (GraveManager.handleUse(player, level, event.getHand(), event.getTarget())) {
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.SUCCESS);
			}
		}
	}

	@SubscribeEvent
	public void onEntityAttack(AttackEntityEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
			if (GraveManager.handleAttack(player, level, event.getTarget())) {
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void onRegisterCommands(RegisterCommandsEvent event) {
		GraveCommands.register(event.getDispatcher());
	}

	@SubscribeEvent
	public void onServerStarted(ServerStartedEvent event) {
		GraveConfig.load();
	}

	@SubscribeEvent
	public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		GraveProtocol.forget(event.getEntity().getUUID());
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}
}

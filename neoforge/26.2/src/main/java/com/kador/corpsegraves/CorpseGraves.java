package com.kador.corpsegraves;

import com.kador.corpsegraves.command.GraveCommands;
import com.kador.corpsegraves.config.GraveConfig;
import com.kador.corpsegraves.grave.GraveManager;
import com.kador.corpsegraves.grave.GraveMenu;
import com.kador.corpsegraves.network.GraveProtocol;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CorpseGraves.MOD_ID)
public class CorpseGraves {
	public static final String MOD_ID = "corpsegraves";
	public static final String VERSION = "1.1.0+26.2-neoforge";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);
	public static final DeferredHolder<MenuType<?>, MenuType<GraveMenu>> MENU = MENUS.register("grave", () -> GraveMenu.TYPE);

	public CorpseGraves(IEventBus modBus, ModContainer container) {
		GraveConfig.load();
		MENUS.register(modBus);
		modBus.addListener(GraveProtocol::register);
		NeoForge.EVENT_BUS.addListener(this::interact);
		NeoForge.EVENT_BUS.addListener(this::attack);
		NeoForge.EVENT_BUS.addListener(this::tick);
		NeoForge.EVENT_BUS.addListener(this::commands);
		NeoForge.EVENT_BUS.addListener(this::serverStarted);
		NeoForge.EVENT_BUS.addListener(this::logout);
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			com.kador.corpsegraves.client.CorpseGravesClient.register(modBus);
		}
		LOGGER.info("Corpse Graves {} initialized for NeoForge 26.2", VERSION);
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

	private void tick(ServerTickEvent.Post event) {
		GraveManager.tick(event.getServer());
	}

	private void commands(RegisterCommandsEvent event) {
		GraveCommands.register(event.getDispatcher());
	}

	private void serverStarted(ServerStartedEvent event) {
		GraveConfig.load();
		GraveManager.refreshHolograms(event.getServer());
	}

	private void logout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			GraveProtocol.remove(player);
		}
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}

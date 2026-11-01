package com.kador.corpsegraves;

import com.kador.corpsegraves.command.GraveCommands;
import com.kador.corpsegraves.config.GraveConfig;
import com.kador.corpsegraves.grave.GraveManager;
import com.kador.corpsegraves.grave.GraveMenu;
import com.kador.corpsegraves.network.GraveProtocol;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CorpseGraves.MOD_ID)
public class CorpseGraves {
	public static final String MOD_ID = "corpsegraves";
	public static final String VERSION = "1.1.0+26.1-forge";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);
	public static final RegistryObject<MenuType<GraveMenu>> MENU = MENUS.register("grave", () -> GraveMenu.TYPE);

	public CorpseGraves(FMLJavaModLoadingContext context) {
		var modBusGroup = context.getModBusGroup();
		GraveConfig.load();
		MENUS.register(modBusGroup);
		GraveProtocol.register();
		LivingDeathEvent.BUS.addListener(this::death);
		PlayerInteractEvent.EntityInteractSpecific.BUS.addListener(event -> {
			InteractionResult result = GraveManager.onUse(event.getEntity(), event.getLevel(), event.getHand(), event.getTarget(), null);
			return result != InteractionResult.PASS;
		});
		AttackEntityEvent.BUS.addListener(event -> {
			InteractionResult result = GraveManager.onAttack(event.getEntity(), event.getEntity().level(), InteractionHand.MAIN_HAND, event.getTarget(), null);
			return result != InteractionResult.PASS;
		});
		TickEvent.ServerTickEvent.Post.BUS.addListener(this::tick);
		RegisterCommandsEvent.BUS.addListener(this::commands);
		ServerStartedEvent.BUS.addListener(this::serverStarted);
		PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(this::logout);
		LOGGER.info("Corpse Graves {} initialized for Forge 26.1", VERSION);
	}

	private void death(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
			GraveManager.handleDeath(player, level);
		}
	}


	private void tick(TickEvent.ServerTickEvent.Post event) {
		GraveManager.tick(event.server());
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

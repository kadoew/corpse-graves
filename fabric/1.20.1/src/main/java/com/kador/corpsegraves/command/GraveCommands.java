package com.kador.corpsegraves.command;

import com.kador.corpsegraves.config.GraveConfig;
import com.kador.corpsegraves.grave.CorpseEntity;
import com.kador.corpsegraves.grave.GraveManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class GraveCommands {
	private GraveCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) ->
			dispatcher.register(Commands.literal("corpsegraves")
				.executes(ctx -> help(ctx.getSource()))
				.then(Commands.literal("help").executes(ctx -> help(ctx.getSource())))
				.then(Commands.literal("locate").executes(ctx -> locate(ctx.getSource())))
				.then(Commands.literal("reload")
					.requires(source -> source.hasPermission(2))
					.executes(ctx -> {
						GraveConfig.reload();
						ctx.getSource().sendSuccess(() -> Component.translatable("corpsegraves.command.reloaded"), true);
						return 1;
					}))
				.then(Commands.literal("unlock")
					.requires(source -> source.hasPermission(2))
					.then(Commands.argument("player", StringArgumentType.word())
						.executes(ctx -> admin(ctx.getSource(), StringArgumentType.getString(ctx, "player"), 0))))
				.then(Commands.literal("remove")
					.requires(source -> source.hasPermission(2))
					.then(Commands.argument("player", StringArgumentType.word())
						.executes(ctx -> admin(ctx.getSource(), StringArgumentType.getString(ctx, "player"), 1))))
			));
	}

	private static int help(CommandSourceStack source) {
		source.sendSuccess(() -> Component.translatable("corpsegraves.command.help"), false);
		return 1;
	}

	private static int locate(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.translatable("corpsegraves.command.players_only"));
			return 0;
		}
		CorpseEntity grave = GraveManager.nearest(source.getServer(), player);
		if (grave == null) {
			source.sendSuccess(() -> Component.translatable("corpsegraves.command.list_empty"), false);
			return 0;
		}
		source.sendSuccess(() -> Component.translatable("corpsegraves.command.locate",
			Math.floor(grave.getX()), Math.floor(grave.getY()), Math.floor(grave.getZ())), false);
		return 1;
	}

	private static int admin(CommandSourceStack source, String name, int action) {
		ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(name);
		if (target == null) {
			source.sendFailure(Component.translatable("corpsegraves.command.player_offline", name));
			return 0;
		}
		List<CorpseEntity> graves = GraveManager.corpsesOf(source.getServer(), target.getUUID());
		for (CorpseEntity grave : graves) {
			if (action == 0) {
				grave.unlockTicksLeft = 0;
			} else {
				grave.dropAndDiscard(true);
			}
		}
		source.sendSuccess(() -> Component.translatable(action == 0 ? "corpsegraves.command.unlocked" : "corpsegraves.command.removed", name), true);
		return Math.max(1, graves.size());
	}
}

package com.kador.corpsegraves.command;

import com.kador.corpsegraves.config.GraveConfig;
import com.kador.corpsegraves.grave.GraveManager;
import com.kador.corpsegraves.grave.GraveRecord;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.List;

public final class GraveCommands {
	private GraveCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("corpsegraves")
				.executes(context -> help(context.getSource()))
				.then(Commands.literal("help")
					.executes(context -> help(context.getSource())))
				.then(Commands.literal("list")
					.executes(context -> list(context.getSource())))
				.then(Commands.literal("locate")
					.executes(context -> locate(context.getSource())))
				.then(Commands.literal("reload")
					.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
					.executes(context -> reload(context.getSource())))
				.then(Commands.literal("unlock")
					.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
					.then(Commands.argument("player", StringArgumentType.word())
						.executes(context -> unlock(context.getSource(), StringArgumentType.getString(context, "player")))))
				.then(Commands.literal("remove")
					.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
					.then(Commands.argument("player", StringArgumentType.word())
						.executes(context -> remove(context.getSource(), StringArgumentType.getString(context, "player")))))
				.then(Commands.literal("open")
					.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
					.then(Commands.argument("player", StringArgumentType.word())
						.executes(context -> open(context.getSource(), StringArgumentType.getString(context, "player")))))
				.then(Commands.literal("restore")
					.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
					.then(Commands.argument("player", StringArgumentType.word())
						.executes(context -> restore(context.getSource(), StringArgumentType.getString(context, "player")))))
		);
	}

	private static int help(CommandSourceStack source) {
		source.sendSuccess(() -> Component.translatable("corpsegraves.command.help"), false);
		return 1;
	}

	private static int list(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.translatable("corpsegraves.command.players_only"));
			return 0;
		}
		MinecraftServer server = source.getServer();
		List<GraveRecord> graves = GraveManager.gravesOf(server, player.getUUID());
		if (graves.isEmpty()) {
			source.sendSuccess(() -> Component.translatable("corpsegraves.command.list_empty"), false);
			return 1;
		}
		source.sendSuccess(() -> Component.translatable("corpsegraves.command.list_header", graves.size()), false);
		for (GraveRecord grave : graves) {
			String coords = String.format("%.0f %.0f %.0f", grave.pos().x, grave.pos().y, grave.pos().z);
			String dimension = grave.dimension();
			String time = grave.despawnTicksLeft < 0 ? "-" : GraveManager.formatTime((int) Math.ceil(grave.despawnTicksLeft / 20.0));
			source.sendSuccess(() -> Component.translatable("corpsegraves.command.list_entry", dimension, coords, time), false);
		}
		return graves.size();
	}

	private static int locate(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.translatable("corpsegraves.command.players_only"));
			return 0;
		}
		GraveRecord grave = GraveManager.findNearest(source.getServer(), player);
		if (grave == null) {
			source.sendSuccess(() -> Component.translatable("corpsegraves.command.list_empty"), false);
			return 0;
		}
		source.sendSuccess(() -> Component.translatable("corpsegraves.command.locate",
			Math.floor(grave.pos().x), Math.floor(grave.pos().y), Math.floor(grave.pos().z)), false);
		return 1;
	}

	private static int reload(CommandSourceStack source) {
		GraveConfig.reload();
		GraveManager.refreshHolograms(source.getServer());
		source.sendSuccess(() -> Component.translatable("corpsegraves.command.reloaded"), true);
		return 1;
	}

	private static int unlock(CommandSourceStack source, String name) {
		ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(name);
		if (target == null) {
			source.sendFailure(Component.translatable("corpsegraves.command.player_offline", name));
			return 0;
		}
		GraveManager.unlockAll(source.getServer(), target.getUUID());
		source.sendSuccess(() -> Component.translatable("corpsegraves.command.unlocked", name), true);
		return 1;
	}

	private static int remove(CommandSourceStack source, String name) {
		ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(name);
		if (target == null) {
			source.sendFailure(Component.translatable("corpsegraves.command.player_offline", name));
			return 0;
		}
		GraveManager.removeAll(source.getServer(), target.getUUID(), true);
		source.sendSuccess(() -> Component.translatable("corpsegraves.command.removed", name), true);
		return 1;
	}

	private static int open(CommandSourceStack source, String name) {
		ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(name);
		if (target == null) {
			source.sendFailure(Component.translatable("corpsegraves.command.player_offline", name));
			return 0;
		}
		GraveRecord grave = GraveManager.findNearest(source.getServer(), target);
		if (grave == null) {
			source.sendFailure(Component.translatable("corpsegraves.command.list_empty"));
			return 0;
		}
		GraveManager.openGrave(target, grave);
		source.sendSuccess(() -> Component.translatable("corpsegraves.command.opened", name), true);
		return 1;
	}

	private static int restore(CommandSourceStack source, String name) {
		ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(name);
		if (target == null) {
			source.sendFailure(Component.translatable("corpsegraves.command.player_offline", name));
			return 0;
		}
		GraveRecord grave = GraveManager.findNearest(source.getServer(), target);
		if (grave == null) {
			source.sendFailure(Component.translatable("corpsegraves.command.list_empty"));
			return 0;
		}
		GraveManager.quickRestore(target, grave);
		source.sendSuccess(() -> Component.translatable("corpsegraves.command.restored", name), true);
		return 1;
	}
}

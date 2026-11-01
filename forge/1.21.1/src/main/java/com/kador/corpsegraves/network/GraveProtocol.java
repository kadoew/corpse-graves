package com.kador.corpsegraves.network;

import com.kador.corpsegraves.CorpseGraves;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

public final class GraveProtocol {
	public static final SimpleChannel CHANNEL = ChannelBuilder.named(CorpseGraves.id("main"))
		.networkProtocolVersion(1)
		.optional()
		.simpleChannel()
		.build();

	private GraveProtocol() {
	}

	public static void register() {
	}

	public static boolean isModded(ServerPlayer player) {
		return CHANNEL.isRemotePresent(player.connection.getConnection());
	}
}

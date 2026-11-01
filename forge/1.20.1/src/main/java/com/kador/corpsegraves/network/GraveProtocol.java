package com.kador.corpsegraves.network;

import com.kador.corpsegraves.CorpseGraves;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GraveProtocol {
	public static final ResourceLocation CHANNEL_NAME = new ResourceLocation(CorpseGraves.MOD_ID, "main");
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
		CHANNEL_NAME, () -> "1", GraveProtocol::acceptsVersion, GraveProtocol::acceptsVersion);

	private static boolean acceptsVersion(String version) {
		return "1".equals(version) || NetworkRegistry.ACCEPTVANILLA.equals(version);
	}

	private static final Set<UUID> MODDED = ConcurrentHashMap.newKeySet();
	private static int nextId;

	private GraveProtocol() {
	}

	public static void init() {
		CHANNEL.messageBuilder(HelloC2S.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
			.encoder((message, buffer) -> {
			})
			.decoder(buffer -> new HelloC2S())
			.consumerMainThread((message, context) -> {
				ServerPlayer sender = context.get().getSender();
				if (sender != null) {
					MODDED.add(sender.getUUID());
				}
				context.get().setPacketHandled(true);
			})
			.add();
	}

	public static boolean isModded(ServerPlayer player) {
		if (MODDED.contains(player.getUUID())) {
			return true;
		}
		return CHANNEL.isRemotePresent(player.connection.connection);
	}

	public static void forget(UUID playerId) {
		MODDED.remove(playerId);
	}
}

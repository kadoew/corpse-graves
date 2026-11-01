package com.kador.corpsegraves.network;

import com.kador.corpsegraves.CorpseGraves;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GraveProtocol {
	private static final Set<UUID> MODDED = ConcurrentHashMap.newKeySet();

	private GraveProtocol() {
	}

	public static final class HelloC2S implements CustomPacketPayload {
		public static final Type<HelloC2S> TYPE = new Type<>(CorpseGraves.id("hello"));
		public static final HelloC2S INSTANCE = new HelloC2S();
		public static final StreamCodec<ByteBuf, HelloC2S> CODEC = StreamCodec.unit(INSTANCE);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void registerCommon() {
		PayloadTypeRegistry.playC2S().register(HelloC2S.TYPE, HelloC2S.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(HelloC2S.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			MODDED.add(player.getUUID());
		});
	}

	public static boolean isModded(ServerPlayer player) {
		return MODDED.contains(player.getUUID());
	}

	public static void remove(ServerPlayer player) {
		if (player != null) {
			MODDED.remove(player.getUUID());
		}
	}
}

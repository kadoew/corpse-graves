package com.kador.corpsegraves.network;

import com.kador.corpsegraves.CorpseGraves;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GraveProtocol {
	private static final Set<UUID> MODDED = ConcurrentHashMap.newKeySet();

	private GraveProtocol() {
	}

	public record HelloC2S() implements CustomPacketPayload {
		public static final HelloC2S INSTANCE = new HelloC2S();
		public static final Type<HelloC2S> TYPE = new Type<>(CorpseGraves.id("hello"));
		public static final StreamCodec<ByteBuf, HelloC2S> CODEC = StreamCodec.unit(INSTANCE);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void register(RegisterPayloadHandlersEvent event) {
		event.registrar("1").playToServer(HelloC2S.TYPE, HelloC2S.CODEC, (payload, context) -> {
			if (context.player() instanceof ServerPlayer player) {
				MODDED.add(player.getUUID());
				CorpseGraves.LOGGER.debug("Client handshake from {}", player.getGameProfile().getName());
			}
		});
	}

	public static boolean isModded(ServerPlayer player) {
		return MODDED.contains(player.getUUID());
	}
}

package com.kador.corpsegraves.network;

import com.kador.corpsegraves.CorpseGraves;
import com.kador.corpsegraves.grave.GraveMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GraveProtocol {
	private static final Set<UUID> MODDED_CLIENTS = ConcurrentHashMap.newKeySet();

	private GraveProtocol() {
	}

	public static final class HelloC2S implements CustomPacketPayload {
		public static final Type<HelloC2S> TYPE = new Type<>(CorpseGraves.id("hello"));
		public static final StreamCodec<ByteBuf, HelloC2S> CODEC =
			ByteBufCodecs.STRING_UTF8.map(HelloC2S::new, HelloC2S::version);
		private final String version;

		public HelloC2S() {
			this(CorpseGraves.VERSION);
		}

		public HelloC2S(String version) {
			this.version = version;
		}

		public String version() {
			return version;
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static final class HelloS2C implements CustomPacketPayload {
		public static final Type<HelloS2C> TYPE = new Type<>(CorpseGraves.id("hello_reply"));
		public static final StreamCodec<ByteBuf, HelloS2C> CODEC =
			ByteBufCodecs.STRING_UTF8.map(HelloS2C::new, HelloS2C::version);
		private final String version;

		public HelloS2C() {
			this(CorpseGraves.VERSION);
		}

		public HelloS2C(String version) {
			this.version = version;
		}

		public String version() {
			return version;
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void register(RegisterPayloadHandlersEvent event) {
		event.registrar("1")
			.playToServer(HelloC2S.TYPE, HelloC2S.CODEC, (payload, context) -> {
				if (context.player() instanceof ServerPlayer player) {
					MODDED_CLIENTS.add(player.getUUID());
					CorpseGraves.LOGGER.debug("Client {} announced Corpse Graves {}", player.getGameProfile().name(), payload.version());
				}
			})
			.playToServer(GraveActionC2S.TYPE, GraveActionC2S.CODEC, (payload, context) -> {
				if (context.player() instanceof ServerPlayer player) {
					context.enqueueWork(() -> {
						if (player.containerMenu instanceof GraveMenu menu && menu.containerId == payload.containerId()) {
							menu.handleClientAction(player, payload.action());
						}
					});
				}
			})
			.playToClient(HelloS2C.TYPE, HelloS2C.CODEC, (payload, context) -> {
			});
	}

	public static boolean isModded(ServerPlayer player) {
		return MODDED_CLIENTS.contains(player.getUUID());
	}

	public static void remove(ServerPlayer player) {
		if (player != null) {
			MODDED_CLIENTS.remove(player.getUUID());
		}
	}
}

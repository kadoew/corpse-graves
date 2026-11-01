package com.kador.corpsegraves.network;

import com.kador.corpsegraves.CorpseGraves;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent by the grave screen's buttons. Using our own payload instead of the
 * vanilla container-button packet keeps the buttons working across GUI
 * pipeline changes.
 */
public record GraveActionC2S(int containerId, int action) implements CustomPacketPayload {
	public static final Type<GraveActionC2S> TYPE = new Type<>(CorpseGraves.id("grave_action"));
	public static final StreamCodec<ByteBuf, GraveActionC2S> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, GraveActionC2S::containerId,
		ByteBufCodecs.VAR_INT, GraveActionC2S::action,
		GraveActionC2S::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

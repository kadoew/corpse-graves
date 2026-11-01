package com.kador.corpsegraves.network;

import com.kador.corpsegraves.CorpseGraves;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class GraveProtocol {
	public static final ResourceLocation CHANNEL = new ResourceLocation(CorpseGraves.MOD_ID, "hello");

	private GraveProtocol() {
	}

	public static boolean isModded(ServerPlayer player) {
		return ServerPlayNetworking.canSend(player, CHANNEL);
	}
}

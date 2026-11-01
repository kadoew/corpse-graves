package com.kador.corpsegraves.mixin.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Vanilla resolves a mannequin's skin only from the profile stored in its
 * data. Offline-mode servers (and launchers that inject skins locally) send
 * no texture property, so the corpse would show the default skin. Reuse the
 * live player's skin when that player is known to this client, and cache it
 * so the correct skin survives the player logging out.
 */
@Mixin(ClientMannequin.class)
public abstract class ClientMannequinSkinMixin {
	private static final Map<UUID, PlayerSkin> CORPSE_GRAVES$SKINS = new HashMap<>();

	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void corpsegraves$useLiveSkin(CallbackInfoReturnable<PlayerSkin> cir) {
		Mannequin mannequin = (Mannequin) (Object) this;
		GameProfile partial = mannequin.getProfile().partialProfile();
		if (partial == null) {
			return;
		}
		String name = partial.name();
		UUID id = partial.id();
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != null) {
			for (AbstractClientPlayer player : minecraft.level.players()) {
				UUID playerId = player.getUUID();
				String playerName = player.getGameProfile().name();
				if ((id != null && id.equals(playerId)) || (name != null && !name.isEmpty() && name.equals(playerName))) {
					PlayerSkin skin = player.getSkin();
					CORPSE_GRAVES$SKINS.put(playerId, skin);
					cir.setReturnValue(skin);
					return;
				}
			}
		}
		if (id != null) {
			PlayerSkin cached = CORPSE_GRAVES$SKINS.get(id);
			if (cached != null) {
				cir.setReturnValue(cached);
			}
		}
	}
}

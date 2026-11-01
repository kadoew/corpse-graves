package com.kador.corpsegraves.mixin;

import com.kador.corpsegraves.CorpseGraves;
import com.kador.corpsegraves.grave.GraveManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDeathMixin {
	@Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
	private void corpsegraves$store(DamageSource source, CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer player) {
			try {
				GraveManager.handleDeath(player, (ServerLevel) player.level());
			} catch (Throwable t) {
				CorpseGraves.LOGGER.error("Grave death handling failed for {}", player.getName().getString(), t);
			}
		}
	}
}

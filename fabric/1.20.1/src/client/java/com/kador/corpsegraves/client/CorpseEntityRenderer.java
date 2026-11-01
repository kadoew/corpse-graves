package com.kador.corpsegraves.client;

import com.kador.corpsegraves.grave.CorpseEntity;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import com.kador.corpsegraves.config.GraveConfig;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import com.mojang.blaze3d.vertex.PoseStack;

public class CorpseEntityRenderer extends LivingEntityRenderer<CorpseEntity, PlayerModel<CorpseEntity>> {
	public CorpseEntityRenderer(EntityRendererProvider.Context context) {
		super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
	}

	@Override
	public ResourceLocation getTextureLocation(CorpseEntity entity) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != null) {
			Player player = minecraft.level.getPlayerByUUID(entity.ownerId());
			if (player instanceof AbstractClientPlayer clientPlayer) {
				return clientPlayer.getSkinTextureLocation();
			}
		}
		return DefaultPlayerSkin.getDefaultSkin(entity.ownerId());
	}

	@Override
	protected void setupRotations(CorpseEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - rotationYaw));
		poseStack.translate(0.0D, GraveConfig.get().corpse.yOffset, 0.0D);
		poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
	}

	@Override
	protected boolean shouldShowName(CorpseEntity entity) {
		return false;
	}
}

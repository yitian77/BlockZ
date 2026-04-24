package com.yitianys.BlockZ.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yitianys.BlockZ.client.model.DayZZombieModel;
import com.yitianys.BlockZ.entity.DayZZombieEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DayZZombieRenderer extends GeoEntityRenderer<DayZZombieEntity> {
    public DayZZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new DayZZombieModel());
        this.shadowRadius = 0.45F;
    }

    @Override
    protected float getDeathMaxRotation(DayZZombieEntity animatable) {
        return 0.0F;
    }

    @Override
    public void render(DayZZombieEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float originalShadowRadius = this.shadowRadius;
        if (entity.isDeadOrDying()) {
            this.shadowRadius = 0.0F;
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        this.shadowRadius = originalShadowRadius;
    }

    @Override
    public int getPackedOverlay(DayZZombieEntity animatable, float u, float partialTick) {
        if (animatable.isDeadOrDying()) {
            return OverlayTexture.NO_OVERLAY;
        }

        return super.getPackedOverlay(animatable, u, partialTick);
    }
}

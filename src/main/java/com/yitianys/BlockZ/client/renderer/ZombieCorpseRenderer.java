package com.yitianys.BlockZ.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yitianys.BlockZ.client.model.ZombieCorpseModel;
import com.yitianys.BlockZ.entity.ZombieCorpseEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ZombieCorpseRenderer extends GeoEntityRenderer<ZombieCorpseEntity> {
    public ZombieCorpseRenderer(EntityRendererProvider.Context context) {
        super(context, new ZombieCorpseModel());
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ZombieCorpseEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public int getPackedOverlay(ZombieCorpseEntity animatable, float u, float partialTick) {
        return OverlayTexture.NO_OVERLAY;
    }
}

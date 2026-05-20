package com.yitianys.BlockZ.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yitianys.BlockZ.client.renderer.FirstPersonBodyRenderState;
import com.yitianys.BlockZ.client.renderer.layer.ClothingLayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class MixinPlayerRenderer extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    @Unique
    private ClothingLayer.OuterLayerState blockz$outerLayerState;

    protected MixinPlayerRenderer(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void blockz$beforeRender(AbstractClientPlayer player, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        PlayerModel<AbstractClientPlayer> model = this.getModel();
        this.blockz$outerLayerState = ClothingLayer.captureOuterLayerState(model);
    }

    @Inject(method = "setModelProperties", at = @At("RETURN"))
    private void blockz$afterSetModelProperties(AbstractClientPlayer player, CallbackInfo ci) {
        ClothingLayer.applyOuterLayerVisibility(player, this.getModel());
        if (!FirstPersonBodyRenderState.isRendering()) {
            return;
        }

        PlayerModel<AbstractClientPlayer> model = this.getModel();
        model.head.visible = false;
        model.hat.visible = false;
        if (FirstPersonBodyRenderState.shouldHideArms()) {
            model.leftArm.visible = false;
            model.rightArm.visible = false;
            model.leftSleeve.visible = false;
            model.rightSleeve.visible = false;
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("RETURN"))
    private void blockz$afterRender(AbstractClientPlayer player, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (this.blockz$outerLayerState != null) {
            ClothingLayer.restoreOuterLayerState(this.getModel(), this.blockz$outerLayerState);
            this.blockz$outerLayerState = null;
        }
    }
}

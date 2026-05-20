package com.yitianys.BlockZ.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yitianys.BlockZ.client.renderer.FirstPersonBodyRenderHelper;
import com.yitianys.BlockZ.client.renderer.FirstPersonBodyRenderState;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRendererFirstPersonBody {
    @Shadow
    private RenderBuffers renderBuffers;

    @Shadow
    private void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 0))
    private void blockz$renderFirstPersonBody(PoseStack poseStack, float partialTick, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        if (!FirstPersonBodyRenderHelper.shouldRenderPlayerBody(camera)) {
            return;
        }
        if (!(camera.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        Vec3 cameraPosition = camera.getPosition();
        boolean hideArms = FirstPersonBodyRenderHelper.shouldHideArms(player);
        boolean hideHeldItems = FirstPersonBodyRenderHelper.shouldHideHeldItems(player);
        FirstPersonBodyRenderState.begin(hideArms, hideHeldItems);
        try {
            this.renderEntity(player, cameraPosition.x(), cameraPosition.y(), cameraPosition.z(), partialTick, poseStack, this.renderBuffers.bufferSource());
        } finally {
            FirstPersonBodyRenderState.end();
        }
    }
}

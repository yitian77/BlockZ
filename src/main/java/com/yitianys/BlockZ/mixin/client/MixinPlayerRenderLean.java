package com.yitianys.BlockZ.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.util.LeanManager;
import com.yitianys.BlockZ.util.ProneManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinPlayerRenderLean {

    @Inject(method = "setupRotations", at = @At("HEAD"), cancellable = true)
    private void blockz$applyProneRotations(LivingEntity entity, PoseStack poseStack,
                                            float ageInTicks, float rotationYaw, float partialTicks, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player) || !ProneManager.isProne(player)) {
            return;
        }

        double horizontalSpeed = player.getDeltaMovement().horizontalDistance();
        float crawlIntensity = Mth.clamp((float) (horizontalSpeed * 8.0D), 0.0F, 1.0F);
        float crawlRoll = Mth.sin((player.tickCount + partialTicks) * 0.45F) * crawlIntensity * 4.0F;
        float crawlYaw = Mth.cos((player.tickCount + partialTicks) * 0.30F) * crawlIntensity * 2.5F;

        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - rotationYaw));
        poseStack.translate(0.0D, -0.2D, -0.05D);
        poseStack.mulPose(Axis.YP.rotationDegrees(crawlYaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(crawlRoll));
        ci.cancel();
    }

    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void blockz$applyLeanRotation(LivingEntity entity, PoseStack poseStack,
                                          float ageInTicks, float rotationYaw, float partialTicks, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player)) return;
        if (ProneManager.isProne(player)) return;
        if (!BlockZConfigs.isLeanEnabled()) return;

        LeanManager.tickClientLeanProgress(player);
        float progress = LeanManager.getSmoothLeanProgress(player.getUUID());
        if (Math.abs(progress) < 0.001F) return;

        float leanAngle = LeanManager.getLeanRollDegrees(progress);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(leanAngle));
    }
}

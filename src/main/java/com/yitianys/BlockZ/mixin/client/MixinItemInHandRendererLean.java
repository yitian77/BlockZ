package com.yitianys.BlockZ.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yitianys.BlockZ.client.camera.CameraWalkSwayManager;
import com.yitianys.BlockZ.client.renderer.FirstPersonBodyRenderHelper;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.util.LeanManager;
import com.yitianys.BlockZ.util.ProneManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRendererLean {

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void blockz$suppressVanillaArm(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
                                          float swingProgress, ItemStack itemStack, float equipProgress, PoseStack poseStack,
                                          MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        if (FirstPersonBodyRenderHelper.shouldSuppressVanillaHands(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void blockz$applyLeanRollToHand(float pPartialTicks, PoseStack pPoseStack,
                                            MultiBufferSource.BufferSource pBufferSource,
                                            LocalPlayer pPlayer, int pPackedLight, CallbackInfo ci) {
        if (BlockZConfigs.shouldReplaceVanillaWalkBobbing() && !FirstPersonBodyRenderHelper.shouldSuppressVanillaHands(pPlayer) && !pPlayer.isScoping()) {
            Vec3 handOffset = CameraWalkSwayManager.getHandLocalOffset(pPlayer, pPartialTicks);
            if (handOffset.lengthSqr() > 1.0E-6D) {
                pPoseStack.translate(handOffset.x, handOffset.y, handOffset.z);
            }
            float handPitch = CameraWalkSwayManager.getHandPitchOffset(pPlayer, pPartialTicks);
            float handYaw = CameraWalkSwayManager.getHandYawOffset(pPlayer, pPartialTicks);
            float handRoll = CameraWalkSwayManager.getHandRollOffset(pPlayer, pPartialTicks);
            if (Math.abs(handPitch) > 0.001F) {
                pPoseStack.mulPose(Axis.XP.rotationDegrees(handPitch));
            }
            if (Math.abs(handYaw) > 0.001F) {
                pPoseStack.mulPose(Axis.YP.rotationDegrees(handYaw));
            }
            if (Math.abs(handRoll) > 0.001F) {
                pPoseStack.mulPose(Axis.ZP.rotationDegrees(handRoll));
            }
        }
        if (!BlockZConfigs.isLeanEnabled()) return;
        if (ProneManager.isProne(pPlayer)) return;

        float progress = LeanManager.getSmoothLeanProgress(pPlayer.getUUID());
        if (Math.abs(progress) < 0.003F) return;

        pPoseStack.mulPose(Axis.ZP.rotationDegrees(LeanManager.getFirstPersonLeanRollDegrees(progress)));
    }
}

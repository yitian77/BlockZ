package com.yitianys.BlockZ.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.util.LeanManager;
import com.yitianys.BlockZ.util.ProneManager;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcherLean {

    @Inject(method = "renderHitbox", at = @At("HEAD"))
    private static void blockz$onRenderHitboxHead(PoseStack poseStack, VertexConsumer buffer, Entity entity, float partialTicks, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        if (!BlockZConfigs.isLeanEnabled()) return;
        if (ProneManager.isProne(player)) return;

        float progress = LeanManager.getSmoothLeanProgress(player.getUUID());
        if (Math.abs(progress) < 0.001F) return;

        float leanAngle = LeanManager.getLeanRollDegrees(progress);
        float yaw = LeanManager.getLeanYaw(player, partialTicks);

        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(leanAngle));
        poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(180.0F - yaw));
    }

    @Inject(method = "renderHitbox", at = @At("TAIL"))
    private static void blockz$onRenderHitboxTail(PoseStack poseStack, VertexConsumer buffer, Entity entity, float partialTicks, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        if (!BlockZConfigs.isLeanEnabled()) return;
        if (ProneManager.isProne(player)) return;

        float progress = LeanManager.getSmoothLeanProgress(player.getUUID());
        if (Math.abs(progress) < 0.001F) return;

        poseStack.popPose();
    }
}

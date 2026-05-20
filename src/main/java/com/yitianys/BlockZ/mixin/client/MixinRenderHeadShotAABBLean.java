package com.yitianys.BlockZ.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.util.LeanManager;
import com.yitianys.BlockZ.util.ProneManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.RenderLivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = com.tacz.guns.client.event.RenderHeadShotAABB.class)
public class MixinRenderHeadShotAABBLean {

    @Redirect(method = "onRenderEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLineBox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/phys/AABB;FFFF)V"))
    private static void blockz$redirectRenderLineBox(PoseStack poseStack, VertexConsumer buffer, AABB aabb, float r, float g, float b, float a, RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player && BlockZConfigs.isLeanEnabled()) {
            if (ProneManager.isProne(player)) {
                LevelRenderer.renderLineBox(poseStack, buffer, aabb, r, g, b, a);
                return;
            }
            float progress = LeanManager.getSmoothLeanProgress(player.getUUID());
            if (Math.abs(progress) >= 0.001F) {
                float leanAngle = LeanManager.getLeanRollDegrees(progress);
                float yaw = LeanManager.getLeanYaw(player, event.getPartialTick());

                poseStack.pushPose();
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - yaw));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(leanAngle));
                poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(180.0F - yaw));
                
                LevelRenderer.renderLineBox(poseStack, buffer, aabb, r, g, b, a);
                poseStack.popPose();
                return;
            }
        }
        LevelRenderer.renderLineBox(poseStack, buffer, aabb, r, g, b, a);
    }
}

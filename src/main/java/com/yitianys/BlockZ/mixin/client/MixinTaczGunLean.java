package com.yitianys.BlockZ.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.client.model.BedrockGunModel;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.util.LeanManager;
import com.yitianys.BlockZ.util.ProneManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.tacz.guns.client.event.FirstPersonRenderGunEvent", remap = false)
public abstract class MixinTaczGunLean {

    @Inject(method = "applyFirstPersonGunTransform", at = @At("HEAD"), remap = false)
    private static void blockz$applyLeanRoll(LocalPlayer player, ItemStack gunItemStack,
                                             PoseStack poseStack,
                                             BedrockGunModel model,
                                             float partialTicks, CallbackInfo ci) {
        if (!BlockZConfigs.isLeanEnabled()) return;
        if (ProneManager.isProne(player)) return;

        float progress = LeanManager.getSmoothLeanProgress(player.getUUID());
        if (Math.abs(progress) < 0.003F) return;

        poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
        poseStack.translate(0, -1.5, 0);

        poseStack.mulPose(Axis.ZP.rotationDegrees(LeanManager.getFirstPersonLeanRollDegrees(progress)));

        poseStack.translate(0, 1.5, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
    }
}

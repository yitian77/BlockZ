package com.yitianys.BlockZ.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yitianys.BlockZ.config.BlockZConfigs;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRendererWalkSway {
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void blockz$replaceVanillaWalkBobbing(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (BlockZConfigs.shouldReplaceVanillaWalkBobbing()) {
            ci.cancel();
        }
    }
}

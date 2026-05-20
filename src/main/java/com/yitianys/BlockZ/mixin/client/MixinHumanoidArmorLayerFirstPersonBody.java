package com.yitianys.BlockZ.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yitianys.BlockZ.client.renderer.FirstPersonBodyRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class MixinHumanoidArmorLayerFirstPersonBody {
    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void blockz$skipHeadArmorDuringFirstPersonBody(PoseStack poseStack, MultiBufferSource buffer, LivingEntity entity, EquipmentSlot slot, int packedLight, HumanoidModel<?> model, CallbackInfo ci) {
        if (slot == EquipmentSlot.HEAD && FirstPersonBodyRenderState.shouldHideHead()) {
            ci.cancel();
        }
    }
}

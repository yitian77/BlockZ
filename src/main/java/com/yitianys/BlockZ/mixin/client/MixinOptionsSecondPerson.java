package com.yitianys.BlockZ.mixin.client;

import com.yitianys.BlockZ.config.BlockZConfigs;
import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(net.minecraft.client.Options.class)
public abstract class MixinOptionsSecondPerson {
    @ModifyVariable(method = "setCameraType", at = @At("HEAD"), argsOnly = true)
    private CameraType blockz$blockSecondPerson(CameraType cameraType) {
        if (!BlockZConfigs.isThirdPersonFrontViewDisabled()) {
            return cameraType;
        }
        if (cameraType == CameraType.THIRD_PERSON_FRONT) {
            return CameraType.THIRD_PERSON_BACK;
        }
        return cameraType;
    }
}

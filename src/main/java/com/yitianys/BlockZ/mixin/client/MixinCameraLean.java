package com.yitianys.BlockZ.mixin.client;

import com.yitianys.BlockZ.client.camera.CameraWalkSwayManager;
import com.yitianys.BlockZ.client.camera.ThirdPersonShoulderCameraManager;
import com.yitianys.BlockZ.compat.TaczClientCompat;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.util.LeanManager;
import com.yitianys.BlockZ.util.ProneManager;
import net.minecraft.client.Camera;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCameraLean {
    @Shadow
    private Vec3 position;

    @Shadow
    public abstract Entity getEntity();

    @Inject(method = "setup", at = @At("RETURN"))
    private void blockz$applyLeanCameraOffset(BlockGetter level, Entity focusedEntity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        Entity entity = getEntity();
        if (!(entity instanceof Player player)) return;

        Vec3 basePosition = this.position;
        Vec3 totalOffset = Vec3.ZERO;
        if (detached) {
            totalOffset = totalOffset.add(ThirdPersonShoulderCameraManager.getCameraOffset(player, partialTick, thirdPersonReverse, this.position));
        }
        boolean skipDetachedLeanCameraOffset = detached && TaczClientCompat.isClientAiming(player);
        if (!skipDetachedLeanCameraOffset && BlockZConfigs.isLeanEnabled() && !ProneManager.isProne(player)) {
            LeanManager.tickClientLeanProgress(player);
            totalOffset = totalOffset.add(LeanManager.getCameraLeanOffset(player, partialTick));
        }
        if (!detached && BlockZConfigs.shouldReplaceVanillaWalkBobbing()) {
            totalOffset = totalOffset.add(CameraWalkSwayManager.getCameraOffset(player, partialTick));
        }
        if (totalOffset.lengthSqr() < 0.0001D) return;

        if (detached) {
            totalOffset = ThirdPersonShoulderCameraManager.clipDetachedCameraOffset(player, basePosition, totalOffset);
            if (totalOffset.lengthSqr() < 0.0001D) return;
        }

        this.position = this.position.add(totalOffset);
    }
}

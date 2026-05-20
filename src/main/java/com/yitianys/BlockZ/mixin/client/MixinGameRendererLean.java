package com.yitianys.BlockZ.mixin.client;

import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.util.LeanManager;
import com.yitianys.BlockZ.util.ProneManager;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRendererLean {
    @Shadow
    @Final
    Minecraft minecraft;

    @Inject(method = "pick(F)V", at = @At("HEAD"), cancellable = true)
    private void blockz$adjustPickForLean(float partialTick, CallbackInfo ci) {
        Entity cameraEntity = this.minecraft.getCameraEntity();
        if (!(cameraEntity instanceof Player player)) return;
        if (blockz$pickFromThirdPersonCenter(player)) {
            ci.cancel();
            return;
        }
        if (!BlockZConfigs.isLeanEnabled()) return;
        if (ProneManager.isProne(player)) return;

        float progress = LeanManager.getSmoothLeanProgress(player.getUUID());
        if (Math.abs(progress) < 0.001F) return;

        Vec3 leanOffset = LeanManager.getCameraLeanOffset(player, partialTick);
        if (leanOffset.lengthSqr() < 0.0001) return;

        double reach = player.isCreative() ? 5.0D : 4.5D;
        Vec3 eyePos = player.getEyePosition(partialTick).add(leanOffset);
        Vec3 viewVec = player.getViewVector(partialTick);
        Vec3 endPos = eyePos.add(viewVec.x * reach, viewVec.y * reach, viewVec.z * reach);

        ClipContext ctx = new ClipContext(eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        this.minecraft.hitResult = player.level().clip(ctx);

        ci.cancel();
    }

    private boolean blockz$pickFromThirdPersonCenter(Player player) {
        if (this.minecraft.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            return false;
        }
        Camera camera = this.minecraft.gameRenderer.getMainCamera();
        if (camera == null) {
            return false;
        }

        double reach = player.isCreative() ? 5.0D : 4.5D;
        Vec3 start = camera.getPosition();
        Vec3 viewVec = blockz$toVec3(camera.getLookVector()).normalize();
        Vec3 end = start.add(viewVec.scale(reach));

        ClipContext context = new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        BlockHitResult blockHit = player.level().clip(context);
        double entityReachSqr = reach * reach;
        if (blockHit.getType() != HitResult.Type.MISS) {
            entityReachSqr = start.distanceToSqr(blockHit.getLocation());
        }

        AABB searchBox = new AABB(start, end).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                searchBox,
                entity -> entity != player && !entity.isSpectator() && entity.isPickable(),
                entityReachSqr
        );

        this.minecraft.hitResult = entityHit != null ? entityHit : blockHit;
        return true;
    }

    private static Vec3 blockz$toVec3(Vector3f vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }
}

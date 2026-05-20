package com.yitianys.BlockZ.mixin;

import com.tacz.guns.entity.EntityKineticBullet;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.util.LeanManager;
import com.yitianys.BlockZ.util.ProneManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = com.tacz.guns.util.EntityUtil.class)
public class MixinEntityUtilLean {

    @Redirect(method = "getHitResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;"))
    private static java.util.Optional<Vec3> blockz$redirectClip(net.minecraft.world.phys.AABB instance, Vec3 startVec, Vec3 endVec, Projectile bulletEntity, Entity entity, Vec3 originalStart, Vec3 originalEnd) {
        Vec3 newStart = blockz$applyInverseLean(startVec, entity);
        Vec3 newEnd = blockz$applyInverseLean(endVec, entity);
        
        java.util.Optional<Vec3> result = instance.clip(newStart, newEnd);
        
        // If it hit, we need to transform the hit position BACK to the leaned world space
        if (result.isPresent()) {
            return java.util.Optional.of(blockz$applyLean(result.get(), entity));
        }
        return result;
    }

    private static Vec3 blockz$applyInverseLean(Vec3 vec, Entity entity) {
        return blockz$transformVec(vec, entity, true);
    }

    private static Vec3 blockz$applyLean(Vec3 vec, Entity entity) {
        return blockz$transformVec(vec, entity, false);
    }

    private static Vec3 blockz$transformVec(Vec3 vec, Entity entity, boolean inverse) {
        if (!(entity instanceof Player player)) return vec;
        if (!BlockZConfigs.isLeanEnabled()) return vec;
        if (ProneManager.isProne(player)) return vec;

        float progress = LeanManager.getAppliedLeanProgress(player);
        if (Math.abs(progress) < 0.001F) return vec;

        float leanAngle = LeanManager.getLeanRollDegrees(progress);
        float angleToApply = inverse ? -leanAngle : leanAngle;

        float yaw = LeanManager.getLeanYaw(player);

        Vector3f v = new Vector3f((float)(vec.x - player.getX()), (float)(vec.y - player.getY()), (float)(vec.z - player.getZ()));

        float yawRad = (float) Math.toRadians(180.0F - yaw);
        
        v.rotateY(-yawRad);
        v.rotateZ((float) Math.toRadians(angleToApply));
        v.rotateY(yawRad);

        return new Vec3(player.getX() + v.x(), player.getY() + v.y(), player.getZ() + v.z());
    }
}

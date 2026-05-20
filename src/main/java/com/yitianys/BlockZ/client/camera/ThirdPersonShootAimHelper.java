package com.yitianys.BlockZ.client.camera;

import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class ThirdPersonShootAimHelper {
    private static final double DEFAULT_AIM_REACH = 256.0D;

    private ThirdPersonShootAimHelper() {
    }

    public static boolean shouldUseThirdPersonCameraAim(Minecraft minecraft, Player player) {
        if (minecraft == null || player == null) {
            return false;
        }
        if (minecraft.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            return false;
        }
        return minecraft.getCameraEntity() == player;
    }

    public static Vec3 resolveThirdPersonAimTarget(Minecraft minecraft, Player player, float partialTick, double reach) {
        if (!shouldUseThirdPersonCameraAim(minecraft, player)) {
            return null;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (camera == null) {
            return null;
        }
        Vec3 start = camera.getPosition();
        Vec3 look = toVec3(camera.getLookVector());
        if (look.lengthSqr() < 1.0E-6D) {
            return null;
        }
        double actualReach = reach > 1.0E-6D ? reach : DEFAULT_AIM_REACH;
        Vec3 normalizedLook = look.normalize();
        Vec3 end = start.add(normalizedLook.scale(actualReach));

        ClipContext context = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult blockHit = player.level().clip(context);
        double entityReachSqr = actualReach * actualReach;
        if (blockHit.getType() != HitResult.Type.MISS) {
            entityReachSqr = start.distanceToSqr(blockHit.getLocation());
            end = blockHit.getLocation();
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
        if (entityHit != null) {
            return entityHit.getLocation();
        }
        if (blockHit.getType() != HitResult.Type.MISS) {
            return blockHit.getLocation();
        }
        return start.add(normalizedLook.scale(actualReach));
    }

    public static AimAngles resolveAimAnglesFromPlayerToTarget(Player player, Vec3 target) {
        if (player == null || target == null) {
            return null;
        }
        Vec3 eyePos = player.getEyePosition();
        Vec3 delta = target.subtract(eyePos);
        if (delta.lengthSqr() < 1.0E-6D) {
            return null;
        }
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) (-(Mth.atan2(delta.y, horizontal) * Mth.RAD_TO_DEG));
        return new AimAngles(pitch, yaw);
    }

    private static Vec3 toVec3(Vector3f vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }

    public record AimAngles(float pitch, float yaw) {
    }
}

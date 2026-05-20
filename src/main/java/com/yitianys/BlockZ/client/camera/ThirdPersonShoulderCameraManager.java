package com.yitianys.BlockZ.client.camera;

import com.yitianys.BlockZ.compat.TaczClientCompat;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.util.LeanManager;
import com.yitianys.BlockZ.util.ProneManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ThirdPersonShoulderCameraManager {
    private static float previousShoulderBlend = 1.0F;
    private static float shoulderBlend = 1.0F;
    private static float targetShoulderBlend = 1.0F;
    private static final double CAMERA_CLIP_MARGIN = 0.05D;
    private static final double CAMERA_SAMPLE_RADIUS_HORIZONTAL = 0.19D;
    private static final double CAMERA_SAMPLE_RADIUS_VERTICAL = 0.17D;

    private ThirdPersonShoulderCameraManager() {
    }

    public static void tick(Minecraft minecraft) {
        previousShoulderBlend = shoulderBlend;
        float defaultBlend = resolveDefaultShoulderBlend();
        if (!BlockZConfigs.isThirdPersonShoulderCameraEnabled()) {
            targetShoulderBlend = defaultBlend;
            shoulderBlend = defaultBlend;
            previousShoulderBlend = shoulderBlend;
            return;
        }
        LocalPlayer player = minecraft.player;
        if (player == null) {
            targetShoulderBlend = defaultBlend;
            shoulderBlend = defaultBlend;
            previousShoulderBlend = shoulderBlend;
            return;
        }

        if (TaczClientCompat.isClientAiming(player)) {
            float smoothing = Mth.clamp((float) BlockZConfigs.getThirdPersonShoulderSwitchSmoothing(), 0.01F, 1.0F);
            shoulderBlend = Mth.lerp(smoothing, shoulderBlend, targetShoulderBlend);
            if (Math.abs(shoulderBlend - targetShoulderBlend) < 0.001F) {
                shoulderBlend = targetShoulderBlend;
            }
            return;
        }

        targetShoulderBlend = resolveTargetShoulderBlend(player, targetShoulderBlend);
        float smoothing = Mth.clamp((float) BlockZConfigs.getThirdPersonShoulderSwitchSmoothing(), 0.01F, 1.0F);
        shoulderBlend = Mth.lerp(smoothing, shoulderBlend, targetShoulderBlend);
        if (Math.abs(shoulderBlend - targetShoulderBlend) < 0.001F) {
            shoulderBlend = targetShoulderBlend;
        }
    }

    public static Vec3 getCameraOffset(Player player, float partialTick, boolean thirdPersonReverse, Vec3 currentCameraPos) {
        if (!BlockZConfigs.isThirdPersonShoulderCameraEnabled()) {
            return Vec3.ZERO;
        }
        if (thirdPersonReverse) {
            return Vec3.ZERO;
        }
        if (player.isSpectator() || player.isSleeping()) {
            return Vec3.ZERO;
        }

        float shoulder = Mth.lerp(partialTick, previousShoulderBlend, shoulderBlend);
        if (Math.abs(shoulder) < 0.001F) {
            return Vec3.ZERO;
        }

        float yawRadians = player.getViewYRot(partialTick) * Mth.DEG_TO_RAD;
        double cosYaw = Mth.cos(yawRadians);
        double sinYaw = Mth.sin(yawRadians);

        double horizontalOffset = BlockZConfigs.getThirdPersonShoulderHorizontalOffset() * shoulder;
        double verticalOffset = BlockZConfigs.getThirdPersonShoulderVerticalOffset();
        double forwardOffset = BlockZConfigs.getThirdPersonShoulderForwardOffset();
        double distanceOffset = BlockZConfigs.getThirdPersonShoulderDistanceOffset();
        Vec3 pushInOffset = Vec3.ZERO;

        if (distanceOffset > 1.0E-6D) {
            Vec3 anchor = player.getEyePosition(partialTick);
            Vec3 towardAnchor = anchor.subtract(currentCameraPos);
            double towardLength = towardAnchor.length();
            if (towardLength > 1.0E-6D) {
                double appliedDistance = Math.min(distanceOffset, Math.max(0.0D, towardLength - 0.15D));
                if (appliedDistance > 1.0E-6D) {
                    pushInOffset = towardAnchor.scale(appliedDistance / towardLength);
                }
            }
        }

        double worldX = -horizontalOffset * cosYaw - forwardOffset * sinYaw;
        double worldZ = -horizontalOffset * sinYaw + forwardOffset * cosYaw;
        Vec3 desiredOffset = pushInOffset.add(worldX, verticalOffset, worldZ);
        return clipCameraOffset(player, currentCameraPos, desiredOffset);
    }

    public static Vec3 clipDetachedCameraOffset(Player player, Vec3 currentCameraPos, Vec3 desiredOffset) {
        return clipCameraOffset(player, currentCameraPos, desiredOffset);
    }

    public static void resetRuntimeState() {
        targetShoulderBlend = resolveDefaultShoulderBlend();
        shoulderBlend = targetShoulderBlend;
        previousShoulderBlend = shoulderBlend;
    }

    private static float resolveTargetShoulderBlend(LocalPlayer player, float currentTargetBlend) {
        if (!BlockZConfigs.isLeanEnabled() || ProneManager.isProne(player)) {
            return currentTargetBlend;
        }

        LeanManager.LeanState leanState = LeanManager.getClientLeanState(player.getUUID());
        if (leanState == LeanManager.LeanState.LEFT) {
            return -1.0F;
        }
        if (leanState == LeanManager.LeanState.RIGHT) {
            return 1.0F;
        }

        float leanProgress = LeanManager.getSmoothLeanProgress(player.getUUID());
        if (leanProgress < -0.001F) {
            return -1.0F;
        }
        if (leanProgress > 0.001F) {
            return 1.0F;
        }
        return currentTargetBlend;
    }

    private static float resolveDefaultShoulderBlend() {
        return BlockZConfigs.isThirdPersonShoulderDefaultRight() ? 1.0F : -1.0F;
    }

    private static Vec3 clipCameraOffset(Player player, Vec3 start, Vec3 desiredOffset) {
        double totalDistance = desiredOffset.length();
        if (totalDistance < 1.0E-6D) {
            return Vec3.ZERO;
        }

        Vec3 motionDir = desiredOffset.normalize();
        Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 sideDir = motionDir.cross(worldUp);
        if (sideDir.lengthSqr() < 1.0E-6D) {
            sideDir = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            sideDir = sideDir.normalize();
        }
        Vec3 verticalDir = sideDir.cross(motionDir);
        if (verticalDir.lengthSqr() < 1.0E-6D) {
            verticalDir = worldUp;
        } else {
            verticalDir = verticalDir.normalize();
        }

        Vec3 sideOffset = sideDir.scale(CAMERA_SAMPLE_RADIUS_HORIZONTAL);
        Vec3 verticalOffset = verticalDir.scale(CAMERA_SAMPLE_RADIUS_VERTICAL);

        Vec3[] samples = new Vec3[] {
            Vec3.ZERO,
            sideOffset,
            sideOffset.scale(-1.0D),
            verticalOffset,
            verticalOffset.scale(-1.0D),
            sideOffset.add(verticalOffset),
            sideOffset.subtract(verticalOffset),
            sideOffset.scale(-1.0D).add(verticalOffset),
            sideOffset.scale(-1.0D).subtract(verticalOffset)
        };

        double allowedFraction = 1.0D;
        for (Vec3 sample : samples) {
            allowedFraction = Math.min(allowedFraction, clipCameraSample(player, start.add(sample), desiredOffset, totalDistance));
            if (allowedFraction <= 0.0D) {
                return Vec3.ZERO;
            }
        }
        return desiredOffset.scale(Mth.clamp(allowedFraction, 0.0D, 1.0D));
    }

    private static double clipCameraSample(Player player, Vec3 sampleStart, Vec3 desiredOffset, double totalDistance) {
        Vec3 sampleEnd = sampleStart.add(desiredOffset);
        ClipContext context = new ClipContext(sampleStart, sampleEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult hit = player.level().clip(context);
        if (hit.getType() == HitResult.Type.MISS) {
            return 1.0D;
        }

        double allowedDistance = Math.max(0.0D, sampleStart.distanceTo(hit.getLocation()) - CAMERA_CLIP_MARGIN);
        return Mth.clamp(allowedDistance / totalDistance, 0.0D, 1.0D);
    }
}

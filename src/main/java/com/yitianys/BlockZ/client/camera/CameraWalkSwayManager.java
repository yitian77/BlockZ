package com.yitianys.BlockZ.client.camera;

import com.yitianys.BlockZ.config.BlockZConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class CameraWalkSwayManager {
    private static final float WALK_PHASE_SCALE = (float) Math.PI;
    private static float time;
    private static float previousTime;
    private static float previousWalkStrength;
    private static float walkStrength;
    private static float previousImpactStrength;
    private static float impactStrength;
    private static float prevAnimationRollDelta;
    private static float animationRollDelta;
    private static float prevAnimationYawDelta;
    private static float animationYawDelta;
    private static float prevAnimationPitchDelta;
    private static float animationPitchDelta;
    private static float prevIdleYaw;
    private static float idleYaw;
    private static float prevIdlePitch;
    private static float idlePitch;
    private static boolean wasOnGround;
    private static double lastVerticalSpeed;

    private CameraWalkSwayManager() {
    }

    public static void tick(Minecraft minecraft) {
        if (!BlockZConfigs.shouldReplaceVanillaWalkBobbing()) {
            resetRuntimeState();
            return;
        }
        LocalPlayer player = minecraft.player;
        if (player == null) {
            resetRuntimeState();
            return;
        }

        previousTime = time;
        previousWalkStrength = walkStrength;
        previousImpactStrength = impactStrength;
        prevIdleYaw = idleYaw;
        prevIdlePitch = idlePitch;
        prevAnimationRollDelta = animationRollDelta;
        prevAnimationYawDelta = animationYawDelta;
        prevAnimationPitchDelta = animationPitchDelta;

        time += (float) BlockZConfigs.getWalkSwaySpeed();

        if (impactStrength > 0.0F) {
            impactStrength = Math.max(0.0F, impactStrength - 0.12F);
        }

        float targetWalkStrength = resolveTargetWalkStrength(player);
        if (walkStrength < targetWalkStrength) {
            walkStrength = Math.min(targetWalkStrength, walkStrength + 0.12F);
        } else if (walkStrength > targetWalkStrength) {
            walkStrength = Math.max(targetWalkStrength, walkStrength - 0.12F);
        }

        if (!wasOnGround && player.onGround() && lastVerticalSpeed < -0.18D) {
            addImpact((float) Mth.clamp(-lastVerticalSpeed * 3.25D, 0.35D, 2.5D));
        }
        wasOnGround = player.onGround();
        lastVerticalSpeed = player.getDeltaMovement().y;

        float idleStrength = (float) BlockZConfigs.getIdleSwayStrength();
        if (!player.getAbilities().flying && !player.isSpectator()) {
            float divisor = resolveIdleDivisor(player);
            idleYaw = (float) (Math.sin(time / 20.0F) / divisor) * idleStrength;
            idlePitch = (float) (Math.sin(time / 30.0F) / divisor) * idleStrength;
        } else {
            idleYaw = 0.0F;
            idlePitch = 0.0F;
        }

        animationRollDelta *= 0.75F;
        prevAnimationRollDelta *= 0.75F;
        animationYawDelta *= 0.5F;
        prevAnimationYawDelta *= 0.5F;
        animationPitchDelta *= 0.5F;
        prevAnimationPitchDelta *= 0.5F;
    }

    public static float getYawOffset(Player player, float partialTick) {
        return Mth.lerp(partialTick, prevIdleYaw, idleYaw)
                + Mth.lerp(partialTick, prevAnimationYawDelta, animationYawDelta) / 6.0F;
    }

    public static float getPitchOffset(Player player, float partialTick) {
        float strengthScale = (float) BlockZConfigs.getWalkSwayStrength();
        float impactPitch = (float) (Math.sin(getInterpolatedTime(partialTick) / 5.0F) * getInterpolatedImpactStrength(partialTick) / 20.0F)
                * strengthScale
                * (player.isSprinting() ? 4.0F : 5.5F);
        return Mth.lerp(partialTick, prevIdlePitch, idlePitch)
                + Mth.lerp(partialTick, prevAnimationPitchDelta, animationPitchDelta) / 6.0F
                + impactPitch;
    }

    public static float getRollOffset(Player player, float partialTick) {
        float strengthScale = (float) BlockZConfigs.getWalkSwayStrength();
        float walkPhase = getWalkPhase(player, partialTick);
        float sprintRollScale = player.isSprinting() ? 0.72F : 1.0F;
        float walkRoll = (float) Math.sin(walkPhase)
                * getInterpolatedWalkStrength(partialTick)
                * strengthScale
                * 1.85F
                * sprintRollScale;
        float impactRoll = (float) Math.sin(getInterpolatedTime(partialTick) / 4.0F)
                * getInterpolatedImpactStrength(partialTick)
                * strengthScale
                * (player.isSprinting() ? 1.75F : 2.1F);
        return walkRoll + impactRoll + Mth.lerp(partialTick, prevAnimationRollDelta, animationRollDelta);
    }

    public static Vec3 getCameraOffset(Player player, float partialTick) {
        float interpolatedWalkStrength = getInterpolatedWalkStrength(partialTick);
        float interpolatedImpactStrength = getInterpolatedImpactStrength(partialTick);
        if (interpolatedWalkStrength < 0.0001F && interpolatedImpactStrength < 0.0001F) {
            return Vec3.ZERO;
        }

        float strengthScale = (float) BlockZConfigs.getWalkSwayStrength();
        float walkPhase = getWalkPhase(player, partialTick);
        float idleTime = getInterpolatedTime(partialTick);
        double lateralOffset = Math.sin(walkPhase) * interpolatedWalkStrength * strengthScale * 0.032D;
        double verticalOffset = Math.cos(walkPhase * 2.0F - 0.35F) * interpolatedWalkStrength * strengthScale * 0.016D;
        double forwardOffset = Math.cos(walkPhase) * interpolatedWalkStrength * strengthScale * 0.010D;
        verticalOffset += Math.sin(idleTime / 30.0F) * BlockZConfigs.getIdleSwayStrength() * 0.0035D;
        verticalOffset -= Math.sin(idleTime / 5.0F) * interpolatedImpactStrength * 0.005D;

        float yawRadians = player.getViewYRot(partialTick) * Mth.DEG_TO_RAD;
        double cosYaw = Mth.cos(yawRadians);
        double sinYaw = Mth.sin(yawRadians);
        double worldX = lateralOffset * cosYaw - forwardOffset * sinYaw;
        double worldZ = lateralOffset * sinYaw + forwardOffset * cosYaw;
        return new Vec3(worldX, verticalOffset, worldZ);
    }

    public static Vec3 getHandLocalOffset(Player player, float partialTick) {
        float interpolatedWalkStrength = getInterpolatedWalkStrength(partialTick);
        float interpolatedImpactStrength = getInterpolatedImpactStrength(partialTick);
        float strengthScale = (float) BlockZConfigs.getWalkSwayStrength();
        float walkPhase = getWalkPhase(player, partialTick);
        float idleTime = getInterpolatedTime(partialTick);
        double lateralOffset = Math.sin(walkPhase) * interpolatedWalkStrength * strengthScale * 0.042D;
        double verticalOffset = Math.cos(walkPhase * 2.0F - 0.35F) * interpolatedWalkStrength * strengthScale * 0.020D;
        double forwardOffset = Math.cos(walkPhase) * interpolatedWalkStrength * strengthScale * 0.016D;
        verticalOffset += Math.sin(idleTime / 30.0F) * BlockZConfigs.getIdleSwayStrength() * 0.004D;
        verticalOffset -= Math.sin(idleTime / 5.0F) * interpolatedImpactStrength * 0.007D;
        forwardOffset -= interpolatedImpactStrength * 0.003D;
        return new Vec3(lateralOffset, verticalOffset, forwardOffset);
    }

    public static float getHandYawOffset(Player player, float partialTick) {
        return getYawOffset(player, partialTick) * 1.35F;
    }

    public static float getHandPitchOffset(Player player, float partialTick) {
        return getPitchOffset(player, partialTick) * 1.1F;
    }

    public static float getHandRollOffset(Player player, float partialTick) {
        return getRollOffset(player, partialTick) * 0.9F;
    }

    public static void addImpact(float strength) {
        impactStrength = Math.min(10.0F, impactStrength + Math.max(0.0F, strength));
    }

    public static void addAnimationDelta(float rollDelta, float yawDelta, float pitchDelta) {
        animationRollDelta += rollDelta;
        animationYawDelta += yawDelta;
        animationPitchDelta += pitchDelta;
    }

    public static void resetRuntimeState() {
        time = 0.0F;
        previousTime = 0.0F;
        previousWalkStrength = 0.0F;
        walkStrength = 0.0F;
        previousImpactStrength = 0.0F;
        impactStrength = 0.0F;
        prevAnimationRollDelta = 0.0F;
        animationRollDelta = 0.0F;
        prevAnimationYawDelta = 0.0F;
        animationYawDelta = 0.0F;
        prevAnimationPitchDelta = 0.0F;
        animationPitchDelta = 0.0F;
        prevIdleYaw = 0.0F;
        idleYaw = 0.0F;
        prevIdlePitch = 0.0F;
        idlePitch = 0.0F;
        wasOnGround = false;
        lastVerticalSpeed = 0.0D;
    }

    private static float getInterpolatedWalkStrength(float partialTick) {
        return Mth.lerp(partialTick, previousWalkStrength, walkStrength);
    }

    private static float getInterpolatedImpactStrength(float partialTick) {
        return Mth.lerp(partialTick, previousImpactStrength, impactStrength);
    }

    private static float getInterpolatedTime(float partialTick) {
        return Mth.lerp(partialTick, previousTime, time);
    }

    private static float resolveTargetWalkStrength(Player player) {
        boolean moving = player.onGround()
                && !player.isPassenger()
                && !player.isSwimming()
                && player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D;
        if (!moving) {
            return 0.0F;
        }
        if (BlockZConfigs.isWalkSwayRequiresSprint() && !player.isSprinting()) {
            return 0.0F;
        }
        float baseStrength = player.isSprinting() ? 0.58F : 0.34F;
        double horizontalSpeed = player.getDeltaMovement().horizontalDistance();
        double expectedSpeed = player.isSprinting() ? 0.13D : 0.08D;
        float motionFactor = Mth.clamp((float) (horizontalSpeed / expectedSpeed), 0.45F, 1.0F);
        if (player.isCrouching()) {
            motionFactor *= 0.65F;
        }
        return baseStrength * motionFactor;
    }

    private static float resolveIdleDivisor(Player player) {
        float divisor = 15.0F;
        if (player.isCrouching()) {
            divisor += 5.0F;
        }
        return divisor;
    }

    private static float getWalkPhase(Player player, float partialTick) {
        float walkDistance = Mth.lerp(partialTick, player.walkDistO, player.walkDist);
        return walkDistance * WALK_PHASE_SCALE * (float) BlockZConfigs.getWalkSwaySpeed();
    }
}

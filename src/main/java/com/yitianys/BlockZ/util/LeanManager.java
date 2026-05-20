package com.yitianys.BlockZ.util;

import com.yitianys.BlockZ.config.BlockZConfigs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LeanManager {
    public enum LeanState { NONE, LEFT, RIGHT }

    private static final String LEAN_TAG = "blockz_lean_state";
    private static final float DEFAULT_LEAN_ANGLE_DEGREES = 28.0F;
    private static final float DEFAULT_LEAN_ANIMATION_DURATION = 0.20F;
    private static final float FIRST_PERSON_LEAN_RATIO = 12.0F / DEFAULT_LEAN_ANGLE_DEGREES;
    private static final long MAX_CLIENT_LEAN_STEP_NANOS = 100_000_000L;
    private static final double LEAN_COLLISION_MARGIN = 0.05D;

    private static final Map<UUID, LeanState> CLIENT_LEAN_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> CLIENT_LEAN_PROGRESS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> CLIENT_LEAN_LAST_UPDATE_NANOS = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> SERVER_LEAN_PROGRESS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SERVER_LEAN_LAST_UPDATE_NANOS = new ConcurrentHashMap<>();

    public static LeanState getLeanState(Player player) {
        CompoundTag data = player.getPersistentData();
        String stateName = data.getString(LEAN_TAG);
        try {
            return LeanState.valueOf(stateName);
        } catch (IllegalArgumentException e) {
            return LeanState.NONE;
        }
    }

    public static void setLeanState(Player player, LeanState state) {
        CompoundTag data = player.getPersistentData();
        if (state == LeanState.NONE) {
            data.remove(LEAN_TAG);
        } else {
            data.putString(LEAN_TAG, state.name());
        }
    }

    public static boolean isLeaning(Player player) {
        return getLeanState(player) != LeanState.NONE;
    }

    public static void setClientLeanState(UUID uuid, LeanState state) {
        CLIENT_LEAN_STATES.put(uuid, state);
    }

    public static LeanState getClientLeanState(UUID uuid) {
        return CLIENT_LEAN_STATES.getOrDefault(uuid, LeanState.NONE);
    }

    public static float getLeanProgressRaw(Player player) {
        LeanState state = getLeanState(player);
        if (state == LeanState.NONE) return 0.0F;
        float targetVal = state == LeanState.RIGHT ? 1.0F : -1.0F;
        return getMaxLeanProgress(player, targetVal);
    }

    public static float getAppliedLeanProgress(Player player) {
        if (player.level().isClientSide()) {
            return getSmoothLeanProgress(player.getUUID());
        }
        return SERVER_LEAN_PROGRESS.getOrDefault(player.getUUID(), 0.0F);
    }

    public static float getSmoothLeanProgress(UUID uuid) {
        Float raw = CLIENT_LEAN_PROGRESS.get(uuid);
        return raw != null ? raw : 0.0F;
    }

    public static float getLeanAngleDegrees() {
        return BlockZConfigs.isLeanEnabled() ? (float) BlockZConfigs.getLeanAngleDegrees() : DEFAULT_LEAN_ANGLE_DEGREES;
    }

    public static float getLeanAnimationDurationSeconds() {
        return BlockZConfigs.isLeanEnabled() ? (float) BlockZConfigs.getLeanAnimationDuration() : DEFAULT_LEAN_ANIMATION_DURATION;
    }

    public static float getLeanRollDegrees(float progress) {
        return -progress * getLeanAngleDegrees();
    }

    public static float getFirstPersonLeanRollDegrees(float progress) {
        return getLeanRollDegrees(progress) * FIRST_PERSON_LEAN_RATIO;
    }

    public static float getLeanYaw(Player player) {
        return player.getYRot();
    }

    public static float getLeanYaw(Player player, float partialTicks) {
        return Mth.lerp(partialTicks, player.yRotO, player.getYRot());
    }

    public static boolean shouldAlignBodyToHead(Player player) {
        if (!BlockZConfigs.isLeanEnabled()) return false;
        if (player.level().isClientSide()) {
            return Math.abs(getSmoothLeanProgress(player.getUUID())) > 0.001F || getClientLeanState(player.getUUID()) != LeanState.NONE;
        }
        return Math.abs(getAppliedLeanProgress(player)) > 0.001F || getLeanState(player) != LeanState.NONE;
    }

    public static void alignBodyToHead(Player player) {
        if (!shouldAlignBodyToHead(player)) return;

        float targetYaw = player.getYHeadRot();
        float bodyYaw = Mth.rotLerp(0.35F, player.yBodyRot, targetYaw);
        player.setYBodyRot(bodyYaw);
    }

    public static void tickClientLeanProgress(Player player) {
        UUID uuid = player.getUUID();
        LeanState target = CLIENT_LEAN_STATES.getOrDefault(uuid, LeanState.NONE);
        updateLeanProgress(player, uuid, target, CLIENT_LEAN_PROGRESS, CLIENT_LEAN_LAST_UPDATE_NANOS);
    }

    public static void tickServerLeanProgress(Player player) {
        UUID uuid = player.getUUID();
        LeanState target = getLeanState(player);
        updateLeanProgress(player, uuid, target, SERVER_LEAN_PROGRESS, SERVER_LEAN_LAST_UPDATE_NANOS);
    }

    private static void updateLeanProgress(Player player, UUID uuid, LeanState target, Map<UUID, Float> progressMap, Map<UUID, Long> lastUpdateMap) {
        float targetVal = 0.0F;
        if (target == LeanState.LEFT) targetVal = -1.0F;
        else if (target == LeanState.RIGHT) targetVal = 1.0F;

        long now = System.nanoTime();
        Long lastUpdate = lastUpdateMap.put(uuid, now);
        float deltaSeconds = lastUpdate == null
                ? 1.0F / 20.0F
                : (float) Math.min((now - lastUpdate) / 1_000_000_000.0D, MAX_CLIENT_LEAN_STEP_NANOS / 1_000_000_000.0D);

        if (targetVal != 0.0F) {
            float maxProgress = getMaxLeanProgress(player, targetVal);
            if (Math.abs(targetVal) > Math.abs(maxProgress)) {
                targetVal = maxProgress;
            }
        }

        float current = progressMap.getOrDefault(uuid, 0.0F);
        
        if (Math.abs(current) > Math.abs(targetVal) && Math.signum(current) == Math.signum(targetVal) && targetVal != 0) {
            current = targetVal; // Instant push back to prevent clipping
        } else {
            float diff = targetVal - current;
            if (Math.abs(diff) < 0.001F) {
                current = targetVal;
            } else {
                float animationDuration = Math.max(0.01F, getLeanAnimationDurationSeconds());
                current = Mth.approach(current, targetVal, deltaSeconds / animationDuration);
            }
        }

        if (target == LeanState.NONE && Math.abs(current) < 0.001F) {
            current = 0.0F;
            lastUpdateMap.remove(uuid);
        }
        progressMap.put(uuid, current);
    }

    private static float getMaxLeanProgress(Player player, float targetVal) {
        double eyeH = player.getEyeHeight();
        float yaw = getLeanYaw(player) * Mth.DEG_TO_RAD;
        Vec3 leanOffset = calculateLeanOffset(eyeH, yaw, targetVal);
        if (leanOffset.lengthSqr() < 1.0E-6D) {
            return 0.0F;
        }

        float allowedFraction = getAllowedLeanFraction(player, player.position(), yaw, targetVal);
        return targetVal * allowedFraction;
    }

    public static float getLeanOffsetAmount() {
        return BlockZConfigs.isLeanEnabled() ? (float) BlockZConfigs.getLeanOffset() : 0.0F;
    }

    public static Vec3 getRawLeanCameraOffset(Player player) {
        if (!BlockZConfigs.isLeanEnabled()) return Vec3.ZERO;
        float progress = getAppliedLeanProgress(player);
        if (Math.abs(progress) < 0.001F) return Vec3.ZERO;

        float yaw = getLeanYaw(player) * Mth.DEG_TO_RAD;
        double eyeH = player.getEyeHeight();
        return calculateLeanOffset(eyeH, yaw, progress);
    }

    public static Vec3 getCameraLeanOffset(Player player, float partialTicks) {
        if (!BlockZConfigs.isLeanEnabled()) return Vec3.ZERO;
        float progress = getSmoothLeanProgress(player.getUUID());
        if (Math.abs(progress) < 0.001F) return Vec3.ZERO;

        float yaw = getLeanYaw(player, partialTicks) * Mth.DEG_TO_RAD;
        double eyeH = player.getEyeHeight();
        Vec3 leanOffset = calculateLeanOffset(eyeH, yaw, progress);
        Vec3 basePosition = player.getEyePosition(partialTicks).subtract(0.0D, eyeH, 0.0D);
        float allowedFraction = getAllowedLeanFraction(player, basePosition, yaw, progress);
        if (allowedFraction <= 0.0F) {
            return Vec3.ZERO;
        }
        return leanOffset.scale(allowedFraction);
    }

    public static boolean shouldHideOffhandWhenLeaning(Player player) {
        return BlockZConfigs.isLeanEnabled() && Math.abs(getSmoothLeanProgress(player.getUUID())) > 0.4F;
    }

    private static Vec3 calculateLeanOffset(double eyeH, float yaw, float progress) {
        float leanRad = progress * getLeanAngleDegrees() * Mth.DEG_TO_RAD;
        double sinA = Math.sin(leanRad);
        double cosA = Math.cos(leanRad);
        double dx = -eyeH * sinA * Math.cos(yaw);
        double dy = eyeH * (cosA - 1.0);
        double dz = -eyeH * sinA * Math.sin(yaw);
        return new Vec3(dx, dy, dz);
    }

    private static float getAllowedLeanFraction(Player player, Vec3 basePosition, float yaw, float progress) {
        double eyeHeight = player.getEyeHeight();
        Vec3 eyeLeanOffset = calculateLeanOffset(eyeHeight, yaw, progress);
        if (eyeLeanOffset.lengthSqr() < 1.0E-6D) {
            return 0.0F;
        }

        double bodyRadius = Math.max(0.24D, player.getBbWidth() * 0.48D);
        double headRadius = Math.max(0.2D, player.getBbWidth() * 0.38D);
        double headVerticalRadius = Math.max(0.16D, player.getBbWidth() * 0.30D);
        double torsoVerticalRadius = Math.max(0.18D, bodyRadius * 0.65D);
        Vec3 horizontalOffset = new Vec3(eyeLeanOffset.x, 0.0D, eyeLeanOffset.z);
        Vec3 motionDir = horizontalOffset.lengthSqr() > 1.0E-6D ? horizontalOffset.normalize() : Vec3.ZERO;
        Vec3 forwardDir = motionDir.lengthSqr() > 1.0E-6D ? new Vec3(-motionDir.z, 0.0D, motionDir.x) : new Vec3(1.0D, 0.0D, 0.0D);
        double[] sampleHeights = new double[] {
                eyeHeight * 0.35D,
                eyeHeight * 0.58D,
                eyeHeight * 0.80D,
                eyeHeight
        };

        Vec3 leadingEdge = motionDir.scale(headRadius);
        Vec3 forwardEdge = forwardDir.scale(headRadius);
        Vec3 verticalEdge = new Vec3(0.0D, headVerticalRadius, 0.0D);

        float allowedFraction = 1.0F;
        for (double sampleHeight : sampleHeights) {
            Vec3 sampleLeanOffset = calculateLeanOffset(sampleHeight, yaw, progress);
            double totalDist = sampleLeanOffset.length();
            if (totalDist < 1.0E-6D) {
                continue;
            }

            boolean isHeadLayer = sampleHeight >= eyeHeight * 0.9D;
            double horizontalRadius = isHeadLayer ? headRadius : bodyRadius;
            double verticalRadius = isHeadLayer ? headVerticalRadius : torsoVerticalRadius;
            Vec3 sampleStart = basePosition.add(0.0D, sampleHeight, 0.0D);
            allowedFraction = Math.min(allowedFraction, clipLeanCrossSection(player, sampleStart, sampleLeanOffset, totalDist, motionDir, forwardDir, horizontalRadius, verticalRadius));
            if (allowedFraction <= 0.0F) {
                return 0.0F;
            }
        }
        return allowedFraction;
    }

    private static float clipLeanCrossSection(Player player, Vec3 start, Vec3 leanOffset, double totalDist, Vec3 motionDir, Vec3 forwardDir, double horizontalRadius, double verticalRadius) {
        Vec3 leadingEdge = motionDir.scale(horizontalRadius);
        Vec3 forwardEdge = forwardDir.scale(horizontalRadius);
        Vec3 verticalEdge = new Vec3(0.0D, verticalRadius, 0.0D);
        Vec3[] samples = new Vec3[] {
                Vec3.ZERO,
                leadingEdge,
                forwardEdge,
                forwardEdge.scale(-1.0D),
                leadingEdge.add(forwardEdge),
                leadingEdge.subtract(forwardEdge),
                verticalEdge,
                verticalEdge.scale(-1.0D),
                leadingEdge.add(verticalEdge),
                leadingEdge.subtract(verticalEdge)
        };

        float allowedFraction = 1.0F;
        for (Vec3 sample : samples) {
            allowedFraction = Math.min(allowedFraction, clipLeanSample(player, start.add(sample), leanOffset, totalDist));
            if (allowedFraction <= 0.0F) {
                return 0.0F;
            }
        }
        return allowedFraction;
    }

    private static float clipLeanSample(Player player, Vec3 sampleStart, Vec3 leanOffset, double totalDist) {
        Vec3 sampleEnd = sampleStart.add(leanOffset);
        ClipContext ctx = new ClipContext(sampleStart, sampleEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult hit = player.level().clip(ctx);
        if (hit.getType() == HitResult.Type.MISS) {
            return 1.0F;
        }

        double dist = sampleStart.distanceTo(hit.getLocation());
        double allowedDist = Math.max(0.0D, dist - LEAN_COLLISION_MARGIN);
        return (float) Mth.clamp(allowedDist / totalDist, 0.0D, 1.0D);
    }
}

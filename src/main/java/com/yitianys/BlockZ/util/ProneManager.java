package com.yitianys.BlockZ.util;

import com.yitianys.BlockZ.compat.TaczProneCompat;
import com.yitianys.BlockZ.network.NetworkHandler;
import com.yitianys.BlockZ.network.ProneSyncS2C;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProneManager {
    private static final String PRONE_TAG = "blockz_prone";
    private static final String PRONE_POSE_APPLIED_TAG = "blockz_prone_pose_applied";
    private static final double PLAYER_STANDING_WIDTH = 0.6D;
    private static final double PLAYER_STANDING_HEIGHT = 1.8D;
    private static final Map<UUID, Boolean> CLIENT_PRONE_STATES = new ConcurrentHashMap<>();

    private ProneManager() {
    }

    public static boolean isProne(Player player) {
        if (player.level().isClientSide) {
            return CLIENT_PRONE_STATES.getOrDefault(player.getUUID(), getStoredProneState(player));
        }
        return getStoredProneState(player);
    }

    public static boolean isServerProne(Player player) {
        return getStoredProneState(player);
    }

    public static boolean isClientProne(UUID playerUuid) {
        return CLIENT_PRONE_STATES.getOrDefault(playerUuid, false);
    }

    public static void setClientProneState(UUID playerUuid, boolean prone) {
        if (prone) {
            CLIENT_PRONE_STATES.put(playerUuid, true);
        } else {
            CLIENT_PRONE_STATES.remove(playerUuid);
        }
    }

    public static void clearClientStates() {
        CLIENT_PRONE_STATES.clear();
    }

    public static void setServerProne(Player player, boolean prone) {
        CompoundTag data = player.getPersistentData();
        if (prone) {
            data.putBoolean(PRONE_TAG, true);
        } else {
            data.remove(PRONE_TAG);
        }
        TaczProneCompat.setServerCrawling(player, prone);
    }

    public static boolean applyRequestedServerState(ServerPlayer player, boolean desiredProne) {
        boolean resolvedState = desiredProne ? canEnterProne(player) : mustStayProne(player);
        boolean currentState = isServerProne(player);
        if (currentState != resolvedState) {
            setServerProne(player, resolvedState);
            return true;
        }
        TaczProneCompat.setServerCrawling(player, resolvedState);
        return false;
    }

    public static boolean validateServerState(ServerPlayer player) {
        if (!isServerProne(player)) {
            return false;
        }
        if (canRemainProne(player)) {
            TaczProneCompat.setServerCrawling(player, true);
            return false;
        }
        setServerProne(player, false);
        return true;
    }

    public static boolean validateClientState(Player player) {
        boolean currentState = isClientProne(player.getUUID());
        if (!currentState) {
            return false;
        }
        if (canRemainProne(player)) {
            return false;
        }
        setClientProneState(player.getUUID(), false);
        return true;
    }

    public static void clearAppliedPronePose(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(PRONE_POSE_APPLIED_TAG)) {
            return;
        }
        player.setForcedPose(null);
        if (player.getPose() == Pose.SWIMMING && !isInActualSwimEnvironment(player)) {
            player.setPose(Pose.STANDING);
        }
        player.refreshDimensions();
        data.remove(PRONE_POSE_APPLIED_TAG);
    }

    public static void tickPlayer(Player player) {
        boolean shouldForcePronePose = isProne(player) && canRemainProne(player);
        CompoundTag data = player.getPersistentData();
        boolean appliedByBlockZ = data.getBoolean(PRONE_POSE_APPLIED_TAG);

        if (shouldForcePronePose) {
            player.setForcedPose(Pose.SWIMMING);
            if (!appliedByBlockZ || player.getPose() != Pose.SWIMMING) {
                player.setPose(Pose.SWIMMING);
                player.refreshDimensions();
            }
            data.putBoolean(PRONE_POSE_APPLIED_TAG, true);
            return;
        }

        if (appliedByBlockZ && !shouldPreserveForcedPose(player)) {
            clearAppliedPronePose(player);
        }
    }

    public static boolean canEnterProne(Player player) {
        if (player.isSpectator() || player.isPassenger() || player.isSleeping()) {
            return false;
        }
        if (!player.onGround() || player.onClimbable() || isInActualSwimEnvironment(player)) {
            return false;
        }
        return player.getPose() != Pose.FALL_FLYING;
    }

    public static boolean canRemainProne(Player player) {
        if (player.isSpectator() || player.isPassenger() || player.isSleeping()) {
            return false;
        }
        if (player.onClimbable() || isInActualSwimEnvironment(player)) {
            return false;
        }
        return player.getPose() != Pose.FALL_FLYING;
    }

    public static boolean canStandUp(Player player) {
        if (player.isSpectator() || player.isPassenger() || player.isSleeping() || isInActualSwimEnvironment(player)) {
            return true;
        }
        double halfWidth = PLAYER_STANDING_WIDTH * 0.5D;
        AABB standingBox = new AABB(
                player.getX() - halfWidth,
                player.getY(),
                player.getZ() - halfWidth,
                player.getX() + halfWidth,
                player.getY() + PLAYER_STANDING_HEIGHT,
                player.getZ() + halfWidth
        );
        return player.level().noCollision(standingBox);
    }

    public static boolean shouldApplyMovementPenalty(Player player) {
        return isProne(player) && canRemainProne(player) && !isInActualSwimEnvironment(player) && !player.getAbilities().instabuild;
    }

    public static void copyPersistentState(Player source, Player target) {
        setServerProne(target, isServerProne(source));
    }

    public static void reset(Player player) {
        setServerProne(player, false);
        clearAppliedPronePose(player);
    }

    public static void syncStateTo(ServerPlayer recipient, Player target) {
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipient), new ProneSyncS2C(target.getUUID(), isServerProne(target)));
    }

    public static void broadcastState(ServerPlayer player) {
        ProneSyncS2C syncMessage = new ProneSyncS2C(player.getUUID(), isServerProne(player));
        for (ServerPlayer tracker : player.serverLevel().players()) {
            if (tracker == player || tracker.distanceToSqr(player) < 4096.0D) {
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> tracker), syncMessage);
            }
        }
    }

    private static boolean getStoredProneState(Player player) {
        return player.getPersistentData().getBoolean(PRONE_TAG);
    }

    private static boolean mustStayProne(Player player) {
        return isProne(player) && !canStandUp(player);
    }

    private static boolean shouldPreserveForcedPose(Player player) {
        return isInActualSwimEnvironment(player);
    }

    private static boolean isInActualSwimEnvironment(Player player) {
        return player.isInWaterOrBubble() || player.isUnderWater();
    }
}

package com.yitianys.BlockZ.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class PlayerMessageUtils {
    // 动作栏提示冷却：存到玩家 PersistentData，避免刷屏
    private static final String ACTIONBAR_COOLDOWNS_TAG = "blockz_actionbar_cooldowns";

    private PlayerMessageUtils() {
    }

    public static void sendActionbar(Player player, Component message) {
        if (player == null || message == null) {
            return;
        }
        player.displayClientMessage(message, true);
    }

    public static boolean sendActionbarWithCooldown(Player player, Component message, String cooldownKey, int cooldownTicks) {
        if (player == null || message == null) {
            return false;
        }
        if (cooldownKey == null || cooldownKey.isEmpty() || cooldownTicks <= 0) {
            sendActionbar(player, message);
            return true;
        }

        long now = player.level().getGameTime();
        CompoundTag root = player.getPersistentData();
        CompoundTag cooldowns = root.getCompound(ACTIONBAR_COOLDOWNS_TAG);
        long last = cooldowns.getLong(cooldownKey);
        if (now - last < cooldownTicks) {
            return false;
        }

        cooldowns.putLong(cooldownKey, now);
        root.put(ACTIONBAR_COOLDOWNS_TAG, cooldowns);
        sendActionbar(player, message);
        return true;
    }
}

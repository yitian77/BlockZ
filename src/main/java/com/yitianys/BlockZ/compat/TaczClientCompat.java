package com.yitianys.BlockZ.compat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

public final class TaczClientCompat {
    private static boolean clientOperatorLookupAttempted = false;
    private static Class<?> clientOperatorClass = null;
    private static Method isAimMethod = null;
    private static Method aimingProgressMethod = null;

    private TaczClientCompat() {
    }

    public static boolean isClientAiming(Player player) {
        if (!(player instanceof LocalPlayer localPlayer)) {
            return false;
        }
        if (!resolveClientOperator()) {
            return false;
        }
        if (!clientOperatorClass.isInstance(localPlayer)) {
            return false;
        }
        try {
            Object operator = clientOperatorClass.cast(localPlayer);
            Object isAimValue = isAimMethod.invoke(operator);
            Object progressValue = aimingProgressMethod.invoke(operator, 1.0F);
            boolean isAim = isAimValue instanceof Boolean value && value;
            float progress = progressValue instanceof Number value ? value.floatValue() : 0.0F;
            return isAim || progress > 0.05F;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean resolveClientOperator() {
        if (clientOperatorClass != null && isAimMethod != null && aimingProgressMethod != null) {
            return true;
        }
        if (clientOperatorLookupAttempted) {
            return false;
        }
        clientOperatorLookupAttempted = true;
        try {
            clientOperatorClass = Class.forName("com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator");
            isAimMethod = clientOperatorClass.getMethod("isAim");
            aimingProgressMethod = clientOperatorClass.getMethod("getClientAimingProgress", float.class);
            return true;
        } catch (Exception ignored) {
            clientOperatorClass = null;
            isAimMethod = null;
            aimingProgressMethod = null;
            return false;
        }
    }
}

package com.yitianys.BlockZ.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class TaczProneCompat {
    private static final String TACZ_MOD_ID = "tacz";
    private static final String LOCAL_PLAYER_CLASS = "net.minecraft.client.player.LocalPlayer";

    private TaczProneCompat() {
    }

    public static void setClientCrawling(Player player, boolean crawling) {
        if (player == null || !isLoaded()) {
            return;
        }
        if (crawling) {
            return;
        }
        try {
            Class<?> localPlayerClass = Class.forName(LOCAL_PLAYER_CLASS);
            if (!localPlayerClass.isInstance(player)) {
                return;
            }
            Class<?> operatorClass = Class.forName("com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator");
            Method fromLocalPlayer = operatorClass.getMethod("fromLocalPlayer", localPlayerClass);
            Object operator = fromLocalPlayer.invoke(null, player);
            Method crawl = operatorClass.getMethod("crawl", boolean.class);
            crawl.invoke(operator, crawling);
        } catch (Exception ignored) {
        }
    }

    public static void setServerCrawling(LivingEntity entity, boolean crawling) {
        if (entity == null || !isLoaded()) {
            return;
        }
        try {
            Class<?> operatorClass = Class.forName("com.tacz.guns.api.entity.IGunOperator");
            Method fromLivingEntity = operatorClass.getMethod("fromLivingEntity", LivingEntity.class);
            Object operator = fromLivingEntity.invoke(null, entity);
            Method crawl = operatorClass.getMethod("crawl", boolean.class);
            crawl.invoke(operator, crawling);
        } catch (Exception ignored) {
        }
    }

    public static boolean isEntityCrawling(Player player) {
        if (player == null || !isLoaded()) {
            return false;
        }
        if (player.level().isClientSide) {
            try {
                Class<?> localPlayerClass = Class.forName(LOCAL_PLAYER_CLASS);
                if (!localPlayerClass.isInstance(player)) {
                    return false;
                }
                Class<?> operatorClass = Class.forName("com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator");
                Method fromLocalPlayer = operatorClass.getMethod("fromLocalPlayer", localPlayerClass);
                Object operator = fromLocalPlayer.invoke(null, player);
                Method isCrawl = operatorClass.getMethod("isCrawl");
                Object result = isCrawl.invoke(operator);
                return result instanceof Boolean value && value;
            } catch (Exception ignored) {
            }
        }
        try {
            Class<?> operatorClass = Class.forName("com.tacz.guns.api.entity.IGunOperator");
            Method fromLivingEntity = operatorClass.getMethod("fromLivingEntity", LivingEntity.class);
            Object operator = fromLivingEntity.invoke(null, player);
            Method getDataHolder = operatorClass.getMethod("getDataHolder");
            Object dataHolder = getDataHolder.invoke(operator);
            Field isCrawling = dataHolder.getClass().getField("isCrawling");
            return isCrawling.getBoolean(dataHolder);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isLoaded() {
        return ModList.get().isLoaded(TACZ_MOD_ID);
    }
}

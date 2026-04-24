package com.yitianys.BlockZ.util;

import com.yitianys.BlockZ.BlockZ;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DayZStatsManager {
    private static final File STATS_FILE = new File(Minecraft.getInstance().gameDirectory, "config/blockz-stats.dat");
    private static final String CONTEXTS_KEY = "Contexts";
    
    public static long timeAlive = 0;
    public static int playersKilled = 0;
    public static int zombiesKilled = 0;
    public static double distanceTraveled = 0;

    private static double lastX, lastY, lastZ;
    private static boolean firstTick = true;
    private static String activeContextKey = "";
    private static boolean contextLoaded = false;

    public static synchronized void load() {
        String contextKey = resolveContextKey();
        loadForContext(contextKey);
    }

    public static synchronized void save() {
        String contextKey = activeContextKey.isEmpty() ? resolveContextKey() : activeContextKey;
        CompoundTag root = readRootTag();
        CompoundTag contexts = root.contains(CONTEXTS_KEY) ? root.getCompound(CONTEXTS_KEY) : new CompoundTag();
        contexts.put(contextKey, createDataTag());
        root.put(CONTEXTS_KEY, contexts);

        writeRootTag(root);
    }

    private static synchronized void loadForContext(String contextKey) {
        resetData();
        CompoundTag root = readRootTag();
        if (root.contains(CONTEXTS_KEY)) {
            CompoundTag contexts = root.getCompound(CONTEXTS_KEY);
            if (contexts.contains(contextKey)) {
                CompoundTag tag = contexts.getCompound(contextKey);
                readDataTag(tag);
            }
        }

        activeContextKey = contextKey;
        contextLoaded = true;
        resetSession();
    }

    private static synchronized void ensureContext(Player player) {
        String currentContext = resolveContextKey();
        if (!contextLoaded) {
            loadForContext(currentContext);
            return;
        }

        if (!currentContext.equals(activeContextKey)) {
            save();
            loadForContext(currentContext);
        }
    }

    private static CompoundTag createDataTag() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("TimeAlive", timeAlive);
        tag.putInt("PlayersKilled", playersKilled);
        tag.putInt("ZombiesKilled", zombiesKilled);
        tag.putDouble("DistanceTraveled", distanceTraveled);
        return tag;
    }

    private static void readDataTag(CompoundTag tag) {
        timeAlive = tag.getLong("TimeAlive");
        playersKilled = tag.getInt("PlayersKilled");
        zombiesKilled = tag.getInt("ZombiesKilled");
        distanceTraveled = tag.getDouble("DistanceTraveled");
    }

    private static void resetData() {
        timeAlive = 0;
        playersKilled = 0;
        zombiesKilled = 0;
        distanceTraveled = 0;
    }

    private static CompoundTag readRootTag() {
        if (!STATS_FILE.exists()) {
            return new CompoundTag();
        }

        try {
            CompoundTag tag = NbtIo.read(STATS_FILE);
            if (tag != null) {
                return tag;
            }
        } catch (IOException e) {
            BlockZ.LOGGER.error("Failed to read stats file: {}", STATS_FILE.getAbsolutePath(), e);
        }

        return new CompoundTag();
    }

    private static void writeRootTag(CompoundTag root) {
        try {
            if (!STATS_FILE.getParentFile().exists()) STATS_FILE.getParentFile().mkdirs();
            NbtIo.write(root, STATS_FILE);
        } catch (IOException e) {
            BlockZ.LOGGER.error("Failed to write stats file: {}", STATS_FILE.getAbsolutePath(), e);
        }
    }

    public static void update(Player player) {
        if (player.level().isClientSide) {
            ensureContext(player);

            // 1. 记录存活时间 (每 tick)
            timeAlive++;

            // 2. 记录旅行距离
            if (firstTick) {
                lastX = player.getX();
                lastY = player.getY();
                lastZ = player.getZ();
                firstTick = false;
            } else {
                double dx = player.getX() - lastX;
                double dy = player.getY() - lastY;
                double dz = player.getZ() - lastZ;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                
                // 忽略过大的瞬间位移（可能是传送）
                if (dist < 20.0) {
                    distanceTraveled += dist;
                }
                
                lastX = player.getX();
                lastY = player.getY();
                lastZ = player.getZ();
            }

            // 每隔 1000 tick 自动保存一次，防止崩溃丢失数据
            if (timeAlive % 1000 == 0) {
                save();
            }
        }
    }

    public static synchronized void addZombieKill() {
        zombiesKilled++;
    }

    public static synchronized void addPlayerKill() {
        playersKilled++;
    }

    public static synchronized void resetSession() {
        firstTick = true;
    }

    private static String resolveContextKey() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return "unknown";
        }

        UUID profileId = minecraft.getUser() != null ? minecraft.getUser().getProfileId() : null;
        String playerPart = profileId != null ? profileId.toString() : "offline";

        if (minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer() != null) {
            String levelName = minecraft.getSingleplayerServer().getWorldData().getLevelName();
            return "sp:" + playerPart + ":" + (levelName == null ? "world" : levelName);
        }

        if (minecraft.getCurrentServer() != null && minecraft.getCurrentServer().ip != null && !minecraft.getCurrentServer().ip.isBlank()) {
            return "mp:" + playerPart + ":" + minecraft.getCurrentServer().ip;
        }

        if (minecraft.getConnection() != null && minecraft.getConnection().getServerData() != null) {
            String ip = minecraft.getConnection().getServerData().ip;
            if (ip != null && !ip.isBlank()) {
                return "mp:" + playerPart + ":" + ip;
            }
        }

        return "menu:" + playerPart;
    }

    public static String getFormattedTime() {
        long totalMinutes = timeAlive / (20 * 60);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours + "h " + minutes + "m";
    }

    public static String getFormattedDistance() {
        return String.format("%.1fkm", distanceTraveled / 1000.0);
    }
}

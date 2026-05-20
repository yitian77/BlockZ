package com.yitianys.BlockZ.util;

import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.init.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class DayZPlayerStatusManager {
    private static final String ROOT_TAG = "blockz_dayz_status";
    private static final String HEALTH_TAG = "health";
    private static final String STAMINA_TAG = "stamina";
    private static final String INFECTION_TAG = "infection";
    private static final String STAMINA_RECOVERY_DELAY_TAG = "stamina_recovery_delay";
    private static final String STAMINA_SPRINT_LOCKED_TAG = "stamina_sprint_locked";

    public static final float MAX_HEALTH = 100.0F;
    public static final float MAX_INFECTION = 100.0F;

    public static float getMaxStamina() {
        return BlockZConfigs.isStaminaEnabled() ? (float) BlockZConfigs.getStaminaMaxCapacity() : 100.0F;
    }

    public static float getSprintStaminaCost() {
        return BlockZConfigs.isStaminaEnabled() ? (float) BlockZConfigs.getStaminaSprintCost() : 0.0F;
    }

    public static float getSwimStaminaCost() {
        // 游泳消耗基于疾跑消耗，加上水中额外惩罚
        return BlockZConfigs.isStaminaEnabled() ? (float) (BlockZConfigs.getStaminaSprintCost() + BlockZConfigs.getStaminaWaterPenalty()) : 0.0F;
    }

    public static float getJumpStaminaCost() {
        return BlockZConfigs.isStaminaEnabled() ? (float) BlockZConfigs.getStaminaJumpCost() : 0.0F;
    }

    public static float getBaseRecoveryRate() {
        return BlockZConfigs.isStaminaEnabled() ? (float) BlockZConfigs.getStaminaRecoveryRate() : getMaxStamina();
    }

    public static float getIdleRecoveryRate() {
        return getBaseRecoveryRate();
    }

    public static float getWalkRecoveryRate() {
        return getBaseRecoveryRate() * 0.28F;
    }

    public static float getCrouchBonusRecovery() {
        return getBaseRecoveryRate() * 0.55F;
    }

    public static float getLowStaminaBonusRecovery() {
        return getBaseRecoveryRate() * 0.35F;
    }

    private static final float BLEEDING_HEALTH_LOSS_PER_TICK = 0.025F;
    private static final float FRACTURE_HEALTH_LOSS_PER_TICK = 0.006F;
    private static final float STARVATION_HEALTH_LOSS_PER_TICK = 0.010F;
    private static final float PASSIVE_HEALTH_RECOVERY_PER_TICK = 0.010F;

    private static final float SPRINT_EXHAUSTED_STAMINA = 1.0F;
    private static final float SPRINT_RECOVERY_STAMINA = 8.0F;
    private static final int STAMINA_RECOVERY_DELAY_TICKS = 14;
    private static final double MOVEMENT_EPSILON_SQR = 1.0E-4D;

    private DayZPlayerStatusManager() {
    }

    public static void ensureInitialized(Player player) {
        CompoundTag statusTag = getOrCreateStatusTag(player);
        boolean changed = false;
        if (!statusTag.contains(HEALTH_TAG, Tag.TAG_FLOAT)) {
            statusTag.putFloat(HEALTH_TAG, MAX_HEALTH);
            changed = true;
        }
        if (!statusTag.contains(STAMINA_TAG, Tag.TAG_FLOAT)) {
            statusTag.putFloat(STAMINA_TAG, getMaxStamina());
            changed = true;
        }
        if (!statusTag.contains(INFECTION_TAG, Tag.TAG_FLOAT)) {
            statusTag.putFloat(INFECTION_TAG, 0.0F);
            changed = true;
        }
        if (!statusTag.contains(STAMINA_RECOVERY_DELAY_TAG, Tag.TAG_INT)) {
            statusTag.putInt(STAMINA_RECOVERY_DELAY_TAG, 0);
            changed = true;
        }
        if (!statusTag.contains(STAMINA_SPRINT_LOCKED_TAG, Tag.TAG_BYTE)) {
            statusTag.putBoolean(STAMINA_SPRINT_LOCKED_TAG, false);
            changed = true;
        }
        if (changed) {
            saveStatusTag(player, statusTag);
        }
    }

    public static void copyPersistentStatus(Player source, Player target) {
        CompoundTag sourceRoot = source.getPersistentData();
        if (!sourceRoot.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            reset(target);
            return;
        }
        target.getPersistentData().put(ROOT_TAG, sourceRoot.getCompound(ROOT_TAG).copy());
        ensureInitialized(target);
    }

    public static void reset(Player player) {
        CompoundTag statusTag = new CompoundTag();
        statusTag.putFloat(HEALTH_TAG, MAX_HEALTH);
        statusTag.putFloat(STAMINA_TAG, getMaxStamina());
        statusTag.putFloat(INFECTION_TAG, 0.0F);
        statusTag.putInt(STAMINA_RECOVERY_DELAY_TAG, 0);
        statusTag.putBoolean(STAMINA_SPRINT_LOCKED_TAG, false);
        saveStatusTag(player, statusTag);
    }

    public static void tick(Player player) {
        if (!BlockZConfigs.isHealthSystemEnabled()) {
            return;
        }
        ensureInitialized(player);

        float health = getHealthValue(player);
        float stamina = getStaminaValue(player);
        int staminaRecoveryDelay = getStaminaRecoveryDelay(player);
        boolean sprintRecoveryLocked = isSprintRecoveryLocked(player);

        if (player.isCreative() || player.isSpectator()) {
            setStaminaValue(player, getMaxStamina());
            setStaminaRecoveryDelay(player, 0);
            setSprintRecoveryLocked(player, false);
            return;
        }

        sprintRecoveryLocked = updateSprintRecoveryLock(stamina, sprintRecoveryLocked);
        if (sprintRecoveryLocked && player.isSprinting()) {
            player.setSprinting(false);
        }

        boolean bleeding = player.hasEffect(ModEffects.BLEEDING.get());
        boolean fractured = player.hasEffect(ModEffects.FRACTURE.get());

        float healthDelta = 0.0F;
        if (bleeding) {
            healthDelta -= BLEEDING_HEALTH_LOSS_PER_TICK;
        }
        if (fractured) {
            healthDelta -= FRACTURE_HEALTH_LOSS_PER_TICK;
        }
        if (player.getFoodData().getFoodLevel() <= 4) {
            healthDelta -= STARVATION_HEALTH_LOSS_PER_TICK;
        }
        if (healthDelta == 0.0F
                && player.getHealth() >= player.getMaxHealth() * 0.85F
                && player.getFoodData().getFoodLevel() >= 10) {
            healthDelta += PASSIVE_HEALTH_RECOVERY_PER_TICK;
        }

        boolean consumingStamina = false;
        float staminaDelta = 0.0F;

        if (player.isSwimming()) {
            staminaDelta -= getSwimStaminaCost();
            consumingStamina = true;
        } else if (player.isSprinting()) {
            staminaDelta -= getSprintStaminaCost();
            consumingStamina = true;
        }

        if (consumingStamina) {
            staminaRecoveryDelay = STAMINA_RECOVERY_DELAY_TICKS;
        } else if (staminaRecoveryDelay > 0) {
            staminaRecoveryDelay--;
        } else {
            boolean moving = player.getDeltaMovement().horizontalDistanceSqr() > MOVEMENT_EPSILON_SQR;
            if (moving) {
                staminaDelta += getWalkRecoveryRate();
            } else {
                staminaDelta += getIdleRecoveryRate();
            }
            if (player.isCrouching()) {
                staminaDelta += getCrouchBonusRecovery();
            }
            if (stamina < 35.0F) {
                staminaDelta += getLowStaminaBonusRecovery();
            }
        }

        float nextHealth = clamp(health + healthDelta, 0.0F, MAX_HEALTH);
        float nextStamina = clamp(stamina + staminaDelta, 0.0F, getMaxStamina());
        sprintRecoveryLocked = updateSprintRecoveryLock(nextStamina, sprintRecoveryLocked);

        setHealthValue(player, nextHealth);
        setStaminaValue(player, nextStamina);
        setStaminaRecoveryDelay(player, staminaRecoveryDelay);
        setSprintRecoveryLocked(player, sprintRecoveryLocked);

        if (sprintRecoveryLocked && player.isSprinting()) {
            player.setSprinting(false);
        }
    }

    public static void applyDamage(Player player, float damageAmount) {
        if (damageAmount <= 0.0F) {
            return;
        }
        if (!BlockZConfigs.isHealthSystemEnabled()) {
            return;
        }
        ensureInitialized(player);
        float healthLoss = Math.max(0.75F, damageAmount * 1.60F);
        setHealthValue(player, getHealthValue(player) - healthLoss);
    }

    public static void consumeStamina(Player player, float amount) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        ensureInitialized(player);
        float current = getStaminaValue(player);
        float nextStamina = clamp(current - amount, 0.0F, getMaxStamina());
        setStaminaValue(player, nextStamina);
        setStaminaRecoveryDelay(player, STAMINA_RECOVERY_DELAY_TICKS);
        setSprintRecoveryLocked(player, updateSprintRecoveryLock(nextStamina, isSprintRecoveryLocked(player)));
    }

    public static boolean canSprint(Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return true;
        }
        ensureInitialized(player);
        return !isSprintRecoveryLocked(player) && getStaminaValue(player) > getSprintExhaustedThreshold();
    }

    public static boolean canJump(Player player, float jumpCost) {
        if (player.isCreative() || player.isSpectator()) {
            return true;
        }
        ensureInitialized(player);
        if (isSprintRecoveryLocked(player)) {
            return false;
        }
        return getStaminaValue(player) >= Math.max(getJumpStaminaCost(), jumpCost);
    }

    public static float getHealthPointsRatio(Player player) {
        return clamp(player.getHealth() / Math.max(1.0F, player.getMaxHealth()), 0.0F, 1.0F);
    }

    public static float getHealthRatio(Player player) {
        if (!BlockZConfigs.isHealthSystemEnabled()) {
            return getHealthPointsRatio(player);
        }
        ensureInitialized(player);
        return clamp(getHealthValue(player) / MAX_HEALTH, 0.0F, 1.0F);
    }

    public static float getStaminaRatio(Player player) {
        ensureInitialized(player);
        return clamp(getStaminaValue(player) / getMaxStamina(), 0.0F, 1.0F);
    }

    public static float getInfectionRatio(Player player) {
        ensureInitialized(player);
        return clamp(getInfectionValue(player) / MAX_INFECTION, 0.0F, 1.0F);
    }

    public static float getHealthValue(Player player) {
        if (!BlockZConfigs.isHealthSystemEnabled()) {
            return getHealthPointsRatio(player) * MAX_HEALTH;
        }
        return getValue(player, HEALTH_TAG, MAX_HEALTH);
    }

    public static float getStaminaValue(Player player) {
        return getValue(player, STAMINA_TAG, getMaxStamina());
    }

    public static float getInfectionValue(Player player) {
        return getValue(player, INFECTION_TAG, 0.0F);
    }

    public static int getStaminaRecoveryDelay(Player player) {
        CompoundTag statusTag = getOrCreateStatusTag(player);
        if (!statusTag.contains(STAMINA_RECOVERY_DELAY_TAG, Tag.TAG_INT)) {
            statusTag.putInt(STAMINA_RECOVERY_DELAY_TAG, 0);
            saveStatusTag(player, statusTag);
            return 0;
        }
        return statusTag.getInt(STAMINA_RECOVERY_DELAY_TAG);
    }

    public static boolean isSprintRecoveryLocked(Player player) {
        CompoundTag statusTag = getOrCreateStatusTag(player);
        if (!statusTag.contains(STAMINA_SPRINT_LOCKED_TAG, Tag.TAG_BYTE)) {
            statusTag.putBoolean(STAMINA_SPRINT_LOCKED_TAG, false);
            saveStatusTag(player, statusTag);
            return false;
        }
        return statusTag.getBoolean(STAMINA_SPRINT_LOCKED_TAG);
    }

    public static void setInfectionValue(Player player, float infection) {
        setValue(player, INFECTION_TAG, infection, 0.0F, MAX_INFECTION);
    }

    public static double getMovementPenaltyMultiplier(Player player) {
        if (!BlockZConfigs.isHealthSystemEnabled()) {
            return 0.0D;
        }
        float health = getHealthValue(player);
        if (health <= 15.0F) {
            return -0.35D;
        }
        if (health <= 35.0F) {
            return -0.22D;
        }
        if (health <= 55.0F) {
            return -0.12D;
        }
        return 0.0D;
    }

    private static void setHealthValue(Player player, float health) {
        setValue(player, HEALTH_TAG, health, 0.0F, MAX_HEALTH);
    }

    private static void setStaminaValue(Player player, float stamina) {
        setValue(player, STAMINA_TAG, stamina, 0.0F, getMaxStamina());
    }

    private static void setStaminaRecoveryDelay(Player player, int ticks) {
        CompoundTag statusTag = getOrCreateStatusTag(player);
        statusTag.putInt(STAMINA_RECOVERY_DELAY_TAG, Math.max(0, ticks));
        saveStatusTag(player, statusTag);
    }

    private static void setSprintRecoveryLocked(Player player, boolean locked) {
        CompoundTag statusTag = getOrCreateStatusTag(player);
        statusTag.putBoolean(STAMINA_SPRINT_LOCKED_TAG, locked);
        saveStatusTag(player, statusTag);
    }

    private static float getValue(Player player, String key, float fallback) {
        CompoundTag statusTag = getOrCreateStatusTag(player);
        if (!statusTag.contains(key, Tag.TAG_FLOAT)) {
            statusTag.putFloat(key, fallback);
            saveStatusTag(player, statusTag);
            return fallback;
        }
        return statusTag.getFloat(key);
    }

    private static void setValue(Player player, String key, float value, float min, float max) {
        CompoundTag statusTag = getOrCreateStatusTag(player);
        statusTag.putFloat(key, clamp(value, min, max));
        saveStatusTag(player, statusTag);
    }

    private static CompoundTag getOrCreateStatusTag(Player player) {
        CompoundTag rootTag = player.getPersistentData();
        if (!rootTag.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            rootTag.put(ROOT_TAG, new CompoundTag());
        }
        return rootTag.getCompound(ROOT_TAG);
    }

    private static void saveStatusTag(Player player, CompoundTag statusTag) {
        player.getPersistentData().put(ROOT_TAG, statusTag);
    }

    private static boolean updateSprintRecoveryLock(float stamina, boolean locked) {
        if (stamina <= getSprintExhaustedThreshold()) {
            return true;
        }
        if (locked && stamina >= getSprintRecoveryThreshold()) {
            return false;
        }
        return locked;
    }

    private static float getSprintExhaustedThreshold() {
        return Math.min(SPRINT_EXHAUSTED_STAMINA, getMaxStamina());
    }

    private static float getSprintRecoveryThreshold() {
        return Math.min(SPRINT_RECOVERY_STAMINA, getMaxStamina());
    }

    private static float clamp(float value, float min, float max) {
        return Mth.clamp(value, min, max);
    }
}

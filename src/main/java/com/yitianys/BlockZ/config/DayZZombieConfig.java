package com.yitianys.BlockZ.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class DayZZombieConfig {
    public static ForgeConfigSpec COMMON_SPEC;

    public static final boolean DEFAULT_ENABLE_CUSTOM_SENSE = true;
    public static final boolean DEFAULT_ENABLE_NATURAL_SPAWN = true;
    public static final double DEFAULT_CROUCH_DETECTION_RANGE = 6.0D;
    public static final double DEFAULT_WALK_DETECTION_RANGE = 12.0D;
    public static final double DEFAULT_MOVING_DETECTION_RANGE = 16.0D;
    public static final double DEFAULT_SPRINT_DETECTION_RANGE = 20.0D;
    public static final double DEFAULT_CLOSE_DETECTION_RANGE = 4.0D;
    public static final double DEFAULT_TARGET_SEARCH_VERTICAL_RANGE = 8.0D;
    public static final int DEFAULT_TARGET_SCAN_INTERVAL = 12;
    public static final int DEFAULT_TARGET_SCAN_RANDOM_DELAY = 8;
    public static final int DEFAULT_TARGET_MEMORY_TICKS = 60;
    public static final int DEFAULT_ALERT_INTERVAL = 30;
    public static final double DEFAULT_ALERT_RANGE_HORIZONTAL = 14.0D;
    public static final double DEFAULT_ALERT_RANGE_VERTICAL = 6.0D;
    public static final double DEFAULT_TRACKING_EXTRA_RANGE = 4.0D;
    public static final double DEFAULT_TRACKING_MEMORY_EXTRA_RANGE = 6.0D;
    public static final double DEFAULT_MAX_HEALTH = 26.0D;
    public static final double DEFAULT_MOVEMENT_SPEED = 0.23D;
    public static final double DEFAULT_ATTACK_DAMAGE = 5.0D;
    public static final double DEFAULT_FOLLOW_RANGE = 28.0D;
    public static final double DEFAULT_ARMOR = 1.0D;
    public static final double DEFAULT_KNOCKBACK_RESISTANCE = 0.1D;
    public static final double DEFAULT_ATTACK_BLEEDING_CHANCE = 0.18D;
    public static final int DEFAULT_CORPSE_STAY_DURATION = 200;
    public static final boolean DEFAULT_ENABLE_NIGHT_BOOST = true;
    public static final double DEFAULT_NIGHT_MOVEMENT_SPEED_MULTIPLIER = 1.18D;
    public static final double DEFAULT_NIGHT_ATTACK_DAMAGE_MULTIPLIER = 1.15D;
    public static final double DEFAULT_NIGHT_FOLLOW_RANGE_MULTIPLIER = 1.2D;

    public static ForgeConfigSpec.BooleanValue enableCustomSense;
    public static ForgeConfigSpec.BooleanValue enableNaturalSpawn;
    public static ForgeConfigSpec.DoubleValue crouchDetectionRange;
    public static ForgeConfigSpec.DoubleValue walkDetectionRange;
    public static ForgeConfigSpec.DoubleValue movingDetectionRange;
    public static ForgeConfigSpec.DoubleValue sprintDetectionRange;
    public static ForgeConfigSpec.DoubleValue closeDetectionRange;
    public static ForgeConfigSpec.DoubleValue targetSearchVerticalRange;
    public static ForgeConfigSpec.IntValue targetScanInterval;
    public static ForgeConfigSpec.IntValue targetScanRandomDelay;
    public static ForgeConfigSpec.IntValue targetMemoryTicks;
    public static ForgeConfigSpec.IntValue alertInterval;
    public static ForgeConfigSpec.DoubleValue alertRangeHorizontal;
    public static ForgeConfigSpec.DoubleValue alertRangeVertical;
    public static ForgeConfigSpec.DoubleValue trackingExtraRange;
    public static ForgeConfigSpec.DoubleValue trackingMemoryExtraRange;

    public static ForgeConfigSpec.DoubleValue maxHealth;
    public static ForgeConfigSpec.DoubleValue movementSpeed;
    public static ForgeConfigSpec.DoubleValue attackDamage;
    public static ForgeConfigSpec.DoubleValue followRange;
    public static ForgeConfigSpec.DoubleValue armor;
    public static ForgeConfigSpec.DoubleValue knockbackResistance;
    public static ForgeConfigSpec.DoubleValue attackBleedingChance;

    public static ForgeConfigSpec.IntValue corpseStayDuration;
    public static ForgeConfigSpec.BooleanValue enableNightBoost;
    public static ForgeConfigSpec.DoubleValue nightMovementSpeedMultiplier;
    public static ForgeConfigSpec.DoubleValue nightAttackDamageMultiplier;
    public static ForgeConfigSpec.DoubleValue nightFollowRangeMultiplier;

    private DayZZombieConfig() {
    }

    public static void register() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("sense");
        enableCustomSense = builder.comment("是否启用自定义感知逻辑").define("enable_custom_sense", DEFAULT_ENABLE_CUSTOM_SENSE);
        enableNaturalSpawn = builder.comment("是否允许 DayZ 丧尸自然生成").define("enable_natural_spawn", DEFAULT_ENABLE_NATURAL_SPAWN);
        crouchDetectionRange = builder.comment("玩家潜行时的发现距离").defineInRange("crouch_detection_range", DEFAULT_CROUCH_DETECTION_RANGE, 0.0D, 128.0D);
        walkDetectionRange = builder.comment("玩家正常步行时的发现距离").defineInRange("walk_detection_range", DEFAULT_WALK_DETECTION_RANGE, 0.0D, 128.0D);
        movingDetectionRange = builder.comment("玩家处于普通移动状态时的发现距离").defineInRange("moving_detection_range", DEFAULT_MOVING_DETECTION_RANGE, 0.0D, 128.0D);
        sprintDetectionRange = builder.comment("玩家冲刺时的发现距离").defineInRange("sprint_detection_range", DEFAULT_SPRINT_DETECTION_RANGE, 0.0D, 128.0D);
        closeDetectionRange = builder.comment("贴身强制锁定距离").defineInRange("close_detection_range", DEFAULT_CLOSE_DETECTION_RANGE, 0.0D, 32.0D);
        targetSearchVerticalRange = builder.comment("索敌纵向范围").defineInRange("target_search_vertical_range", DEFAULT_TARGET_SEARCH_VERTICAL_RANGE, 0.0D, 32.0D);
        targetScanInterval = builder.comment("索敌基础间隔 tick").defineInRange("target_scan_interval", DEFAULT_TARGET_SCAN_INTERVAL, 1, 200);
        targetScanRandomDelay = builder.comment("索敌随机附加间隔 tick").defineInRange("target_scan_random_delay", DEFAULT_TARGET_SCAN_RANDOM_DELAY, 0, 200);
        targetMemoryTicks = builder.comment("丢失目标后的记忆时长 tick").defineInRange("target_memory_ticks", DEFAULT_TARGET_MEMORY_TICKS, 0, 1200);
        alertInterval = builder.comment("呼叫周围丧尸的冷却 tick").defineInRange("alert_interval", DEFAULT_ALERT_INTERVAL, 1, 1200);
        alertRangeHorizontal = builder.comment("呼叫同伴水平范围").defineInRange("alert_range_horizontal", DEFAULT_ALERT_RANGE_HORIZONTAL, 0.0D, 128.0D);
        alertRangeVertical = builder.comment("呼叫同伴垂直范围").defineInRange("alert_range_vertical", DEFAULT_ALERT_RANGE_VERTICAL, 0.0D, 32.0D);
        trackingExtraRange = builder.comment("持续追踪时额外追加的距离").defineInRange("tracking_extra_range", DEFAULT_TRACKING_EXTRA_RANGE, 0.0D, 64.0D);
        trackingMemoryExtraRange = builder.comment("记忆追踪时额外追加的距离").defineInRange("tracking_memory_extra_range", DEFAULT_TRACKING_MEMORY_EXTRA_RANGE, 0.0D, 64.0D);
        builder.pop();

        builder.push("combat");
        maxHealth = builder.comment("丧尸最大生命值").defineInRange("max_health", DEFAULT_MAX_HEALTH, 1.0D, 1024.0D);
        movementSpeed = builder.comment("丧尸基础移动速度").defineInRange("movement_speed", DEFAULT_MOVEMENT_SPEED, 0.01D, 2.0D);
        attackDamage = builder.comment("丧尸基础攻击伤害").defineInRange("attack_damage", DEFAULT_ATTACK_DAMAGE, 0.0D, 100.0D);
        followRange = builder.comment("丧尸基础跟随距离").defineInRange("follow_range", DEFAULT_FOLLOW_RANGE, 1.0D, 128.0D);
        armor = builder.comment("丧尸基础护甲").defineInRange("armor", DEFAULT_ARMOR, 0.0D, 100.0D);
        knockbackResistance = builder.comment("丧尸基础击退抗性").defineInRange("knockback_resistance", DEFAULT_KNOCKBACK_RESISTANCE, 0.0D, 1.0D);
        attackBleedingChance = builder.comment("丧尸攻击附加流血概率").defineInRange("attack_bleeding_chance", DEFAULT_ATTACK_BLEEDING_CHANCE, 0.0D, 1.0D);
        builder.pop();

        builder.push("state");
        corpseStayDuration = builder.comment("丧尸死亡后尸体保留时长 tick").defineInRange("corpse_stay_duration", DEFAULT_CORPSE_STAY_DURATION, 0, 24000);
        enableNightBoost = builder.comment("夜晚是否强化丧尸").define("enable_night_boost", DEFAULT_ENABLE_NIGHT_BOOST);
        nightMovementSpeedMultiplier = builder.comment("夜晚移速倍率").defineInRange("night_movement_speed_multiplier", DEFAULT_NIGHT_MOVEMENT_SPEED_MULTIPLIER, 0.1D, 5.0D);
        nightAttackDamageMultiplier = builder.comment("夜晚攻击伤害倍率").defineInRange("night_attack_damage_multiplier", DEFAULT_NIGHT_ATTACK_DAMAGE_MULTIPLIER, 0.1D, 5.0D);
        nightFollowRangeMultiplier = builder.comment("夜晚跟随距离倍率").defineInRange("night_follow_range_multiplier", DEFAULT_NIGHT_FOLLOW_RANGE_MULTIPLIER, 0.1D, 5.0D);
        builder.pop();

        COMMON_SPEC = builder.build();
    }
}

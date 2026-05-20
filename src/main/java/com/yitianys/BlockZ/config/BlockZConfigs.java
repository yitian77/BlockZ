package com.yitianys.BlockZ.config;

import com.yitianys.BlockZ.util.ItemSizeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

public class BlockZConfigs {
    public static ForgeConfigSpec COMMON_SPEC;
    public static ForgeConfigSpec.IntValue gridCols;
    public static ForgeConfigSpec.IntValue gridRows;
    public static ForgeConfigSpec.BooleanValue enableGridSystem;
    public static ForgeConfigSpec.DoubleValue uiScale;
    public static ForgeConfigSpec.BooleanValue enableDayzInventory;
    public static ForgeConfigSpec.BooleanValue allowPlayerToggleDayz;
    public static ForgeConfigSpec.BooleanValue showDayzHud;
    public static ForgeConfigSpec.BooleanValue enableHealthSystem;
    public static ForgeConfigSpec.BooleanValue showDayzToggleChatHint;
    public static ForgeConfigSpec.BooleanValue enableNursingSystem;
    public static ForgeConfigSpec.BooleanValue enableBleeding;
    public static ForgeConfigSpec.BooleanValue enableBrokenLegs;
    public static ForgeConfigSpec.DoubleValue baseBleedingChance;
    public static ForgeConfigSpec.DoubleValue brokenLegChanceMultiplier;
    public static ForgeConfigSpec.DoubleValue brokenLegMaxChance;
    public static ForgeConfigSpec.BooleanValue enableVanillaBackpackLock;
    public static ForgeConfigSpec.IntValue initialPocketSlots;
    public static ForgeConfigSpec.BooleanValue replaceVanillaWalkBobbing;
    public static ForgeConfigSpec.DoubleValue walkSwayStrength;
    public static ForgeConfigSpec.DoubleValue walkSwaySpeed;
    public static ForgeConfigSpec.DoubleValue idleSwayStrength;
    public static ForgeConfigSpec.BooleanValue walkSwayRequiresSprint;
    public static ForgeConfigSpec.BooleanValue enableFocusZoom;
    public static ForgeConfigSpec.DoubleValue focusFovMultiplierWalk;
    public static ForgeConfigSpec.DoubleValue focusFovMultiplierSprint;
    public static ForgeConfigSpec.DoubleValue focusFovSmoothing;
    public static ForgeConfigSpec.BooleanValue enableRealFirstPerson;
    public static ForgeConfigSpec.BooleanValue enableThirdPersonShoulderCamera;
    public static ForgeConfigSpec.BooleanValue thirdPersonShoulderDefaultRight;
    public static ForgeConfigSpec.DoubleValue thirdPersonShoulderHorizontalOffset;
    public static ForgeConfigSpec.DoubleValue thirdPersonShoulderVerticalOffset;
    public static ForgeConfigSpec.DoubleValue thirdPersonShoulderForwardOffset;
    public static ForgeConfigSpec.DoubleValue thirdPersonShoulderDistanceOffset;
    public static ForgeConfigSpec.DoubleValue thirdPersonShoulderSwitchSmoothing;
    public static ForgeConfigSpec.BooleanValue disableThirdPersonFrontView;

    // 体力系统配置
    public static ForgeConfigSpec.BooleanValue enableStaminaSystem;
    public static ForgeConfigSpec.DoubleValue staminaSprintCost;
    public static ForgeConfigSpec.DoubleValue staminaJumpCost;
    public static ForgeConfigSpec.DoubleValue staminaRecoveryRate;
    public static ForgeConfigSpec.DoubleValue staminaWaterPenalty;
    public static ForgeConfigSpec.DoubleValue staminaMaxCapacity;
    
    // 背包格子数配置
    public static ForgeConfigSpec.IntValue backpackCoyoteSlots;
    public static ForgeConfigSpec.IntValue backpackAliceSlots;
    public static ForgeConfigSpec.IntValue backpackCzechSlots;
    public static ForgeConfigSpec.IntValue backpackCzechPouchSlots;
    public static ForgeConfigSpec.IntValue backpackPatrolPackSlots;
    public static ForgeConfigSpec.IntValue vest0Slots;
    public static ForgeConfigSpec.IntValue shirtSlots;
    public static ForgeConfigSpec.IntValue pantsSlots;
    
    // 尸体配置
    public static ForgeConfigSpec.IntValue corpseDespawnTime;
    public static ForgeConfigSpec.BooleanValue enableCorpse;

    // 氛围雾气配置 (DayZ 冷蓝雾气效果)
    public static ForgeConfigSpec.BooleanValue enableBlueFog;
    public static ForgeConfigSpec.DoubleValue blueFogTintStrength;
    public static ForgeConfigSpec.DoubleValue blueFogDensity;
    public static ForgeConfigSpec.DoubleValue worldDesaturation;
    public static ForgeConfigSpec.BooleanValue enableVignette;
    public static ForgeConfigSpec.DoubleValue vignetteStrength;

    // 左右探头配置
    public static ForgeConfigSpec.BooleanValue enableLeanSystem;
    public static ForgeConfigSpec.DoubleValue leanOffset;
    public static ForgeConfigSpec.DoubleValue leanAngleDegrees;
    public static ForgeConfigSpec.DoubleValue leanAnimationDuration;

    // 服务器配置
    public static ForgeConfigSpec.ConfigValue<String> serverAddress;

    // 主菜单配置
    public static ForgeConfigSpec.BooleanValue enableCustomMainMenu;
    public static ForgeConfigSpec.IntValue mainMenuBackgroundRotationSpeed;
    public static ForgeConfigSpec.DoubleValue mainMenuBackgroundTransitionStep;
    public static ForgeConfigSpec.DoubleValue mainMenuCameraSwayStrength;

    // 宣传图配置
    public static ForgeConfigSpec.ConfigValue<String> posterTitle0;
    public static ForgeConfigSpec.ConfigValue<String> posterUrl0;
    public static ForgeConfigSpec.ConfigValue<String> posterMsg0;
    public static ForgeConfigSpec.ConfigValue<String> posterButton0;
    public static ForgeConfigSpec.ConfigValue<String> posterTitle1;
    public static ForgeConfigSpec.ConfigValue<String> posterUrl1;
    public static ForgeConfigSpec.ConfigValue<String> posterMsg1;
    public static ForgeConfigSpec.ConfigValue<String> posterButton1;
    public static ForgeConfigSpec.ConfigValue<String> posterTitle2;
    public static ForgeConfigSpec.ConfigValue<String> posterUrl2;
    public static ForgeConfigSpec.ConfigValue<String> posterMsg2;
    public static ForgeConfigSpec.ConfigValue<String> posterButton2;
    public static ForgeConfigSpec.IntValue posterRotationSpeed;
    
    // 同步缓存 (仅客户端使用)
    private static boolean isSynced = false;
    private static int s_gridCols;
    private static int s_gridRows;
    private static boolean s_enableGridSystem;
    private static double s_uiScale;
    private static boolean s_enableDayzInventory;
    private static boolean s_allowPlayerToggleDayz;
    private static boolean s_showDayzHud;
    private static boolean s_enableHealthSystem;
    private static boolean s_showDayzToggleChatHint;
    private static boolean s_enableNursingSystem;
    private static boolean s_enableBleeding;
    private static boolean s_enableBrokenLegs;
    private static double s_baseBleedingChance;
    private static double s_brokenLegChanceMultiplier;
    private static double s_brokenLegMaxChance;
    private static boolean s_enableVanillaBackpackLock;
    private static int s_initialPocketSlots;
    private static boolean s_replaceVanillaWalkBobbing;
    private static double s_walkSwayStrength;
    private static double s_walkSwaySpeed;
    private static double s_idleSwayStrength;
    private static boolean s_walkSwayRequiresSprint;
    private static boolean s_enableFocusZoom;
    private static double s_focusFovMultiplierWalk;
    private static double s_focusFovMultiplierSprint;
    private static double s_focusFovSmoothing;
    private static boolean s_enableRealFirstPerson;
    private static boolean s_enableThirdPersonShoulderCamera;
    private static boolean s_thirdPersonShoulderDefaultRight;
    private static double s_thirdPersonShoulderHorizontalOffset;
    private static double s_thirdPersonShoulderVerticalOffset;
    private static double s_thirdPersonShoulderForwardOffset;
    private static double s_thirdPersonShoulderDistanceOffset;
    private static double s_thirdPersonShoulderSwitchSmoothing;
    private static boolean s_disableThirdPersonFrontView;
    private static boolean s_enableStaminaSystem;
    private static double s_staminaMaxCapacity;
    private static double s_staminaSprintCost;
    private static double s_staminaJumpCost;
    private static double s_staminaRecoveryRate;
    private static double s_staminaWaterPenalty;
    private static int s_backpackCoyoteSlots;
    private static int s_backpackAliceSlots;
    private static int s_backpackCzechSlots;
    private static int s_backpackCzechPouchSlots;
    private static int s_backpackPatrolPackSlots;
    private static int s_vest0Slots;
    private static int s_shirtSlots;
    private static int s_pantsSlots;
    private static int s_corpseDespawnTime;
    private static boolean s_enableCorpse;
    private static boolean s_enableBlueFog;
    private static double s_blueFogTintStrength;
    private static double s_blueFogDensity;
    private static double s_worldDesaturation;
    private static boolean s_enableVignette;
    private static double s_vignetteStrength;
    private static boolean s_enableLeanSystem;
    private static double s_leanOffset;
    private static double s_leanAngleDegrees;
    private static double s_leanAnimationDuration;

    public static void setSyncedValues(
        int gridCols, int gridRows, boolean enableGridSystem, double uiScale, boolean enableDayzInventory,
        boolean allowPlayerToggleDayz, boolean showDayzHud, boolean enableHealthSystem, boolean showDayzToggleChatHint, boolean enableNursingSystem, boolean enableBleeding,
        boolean enableBrokenLegs, double baseBleedingChance, double brokenLegChanceMultiplier, double brokenLegMaxChance,
        boolean enableVanillaBackpackLock, int initialPocketSlots, boolean replaceVanillaWalkBobbing,
        double walkSwayStrength, double walkSwaySpeed, double idleSwayStrength,
        boolean walkSwayRequiresSprint, boolean enableFocusZoom, double focusFovMultiplierWalk, double focusFovMultiplierSprint, double focusFovSmoothing,
        boolean enableRealFirstPerson,
        boolean enableThirdPersonShoulderCamera, boolean thirdPersonShoulderDefaultRight,
        double thirdPersonShoulderHorizontalOffset, double thirdPersonShoulderVerticalOffset,
        double thirdPersonShoulderForwardOffset, double thirdPersonShoulderDistanceOffset, double thirdPersonShoulderSwitchSmoothing,
        boolean disableThirdPersonFrontView,
        boolean enableStaminaSystem, double staminaMaxCapacity,
        double staminaSprintCost, double staminaJumpCost, double staminaRecoveryRate, double staminaWaterPenalty,
        int backpackCoyoteSlots, int backpackAliceSlots, int backpackCzechSlots, int backpackCzechPouchSlots,
        int backpackPatrolPackSlots, int vest0Slots, int shirtSlots, int pantsSlots, int corpseDespawnTime,
        boolean enableCorpse, boolean enableBlueFog, double blueFogTintStrength, double blueFogDensity,
        double worldDesaturation, boolean enableVignette, double vignetteStrength,
        boolean enableLeanSystem, double leanOffset, double leanAngleDegrees, double leanAnimationDuration
    ) {
        s_gridCols = gridCols;
        s_gridRows = gridRows;
        s_enableGridSystem = enableGridSystem;
        s_uiScale = uiScale;
        s_enableDayzInventory = enableDayzInventory;
        s_allowPlayerToggleDayz = allowPlayerToggleDayz;
        s_showDayzHud = showDayzHud;
        s_enableHealthSystem = enableHealthSystem;
        s_showDayzToggleChatHint = showDayzToggleChatHint;
        s_enableNursingSystem = enableNursingSystem;
        s_enableBleeding = enableBleeding;
        s_enableBrokenLegs = enableBrokenLegs;
        s_baseBleedingChance = baseBleedingChance;
        s_brokenLegChanceMultiplier = brokenLegChanceMultiplier;
        s_brokenLegMaxChance = brokenLegMaxChance;
        s_enableVanillaBackpackLock = enableVanillaBackpackLock;
        s_initialPocketSlots = initialPocketSlots;
        s_replaceVanillaWalkBobbing = replaceVanillaWalkBobbing;
        s_walkSwayStrength = walkSwayStrength;
        s_walkSwaySpeed = walkSwaySpeed;
        s_idleSwayStrength = idleSwayStrength;
        s_walkSwayRequiresSprint = walkSwayRequiresSprint;
        s_enableFocusZoom = enableFocusZoom;
        s_focusFovMultiplierWalk = focusFovMultiplierWalk;
        s_focusFovMultiplierSprint = focusFovMultiplierSprint;
        s_focusFovSmoothing = focusFovSmoothing;
        s_enableRealFirstPerson = enableRealFirstPerson;
        s_enableThirdPersonShoulderCamera = enableThirdPersonShoulderCamera;
        s_thirdPersonShoulderDefaultRight = thirdPersonShoulderDefaultRight;
        s_thirdPersonShoulderHorizontalOffset = thirdPersonShoulderHorizontalOffset;
        s_thirdPersonShoulderVerticalOffset = thirdPersonShoulderVerticalOffset;
        s_thirdPersonShoulderForwardOffset = thirdPersonShoulderForwardOffset;
        s_thirdPersonShoulderDistanceOffset = thirdPersonShoulderDistanceOffset;
        s_thirdPersonShoulderSwitchSmoothing = thirdPersonShoulderSwitchSmoothing;
        s_disableThirdPersonFrontView = disableThirdPersonFrontView;
        s_enableStaminaSystem = enableStaminaSystem;
        s_staminaMaxCapacity = staminaMaxCapacity;
        s_staminaSprintCost = staminaSprintCost;
        s_staminaJumpCost = staminaJumpCost;
        s_staminaRecoveryRate = staminaRecoveryRate;
        s_staminaWaterPenalty = staminaWaterPenalty;
        s_backpackCoyoteSlots = backpackCoyoteSlots;
        s_backpackAliceSlots = backpackAliceSlots;
        s_backpackCzechSlots = backpackCzechSlots;
        s_backpackCzechPouchSlots = backpackCzechPouchSlots;
        s_backpackPatrolPackSlots = backpackPatrolPackSlots;
        s_vest0Slots = vest0Slots;
        s_shirtSlots = shirtSlots;
        s_pantsSlots = pantsSlots;
        s_corpseDespawnTime = corpseDespawnTime;
        s_enableCorpse = enableCorpse;
        s_enableBlueFog = enableBlueFog;
        s_blueFogTintStrength = blueFogTintStrength;
        s_blueFogDensity = blueFogDensity;
        s_worldDesaturation = worldDesaturation;
        s_enableVignette = enableVignette;
        s_vignetteStrength = vignetteStrength;
        s_enableLeanSystem = enableLeanSystem;
        s_leanOffset = leanOffset;
        s_leanAngleDegrees = leanAngleDegrees;
        s_leanAnimationDuration = leanAnimationDuration;
        isSynced = true;
    }

    public static void clearSyncedValues() {
        isSynced = false;
    }

    public static double getUiScale() { return isSynced ? s_uiScale : uiScale.get(); }
    public static boolean isDayzInventoryEnabled() { return isSynced ? s_enableDayzInventory : enableDayzInventory.get(); }
    public static boolean getAllowPlayerToggleDayz() { return isSynced ? s_allowPlayerToggleDayz : allowPlayerToggleDayz.get(); }
    public static boolean getShowDayzHud() { return isSynced ? s_showDayzHud : showDayzHud.get(); }
    public static boolean isHealthSystemEnabled() { return isSynced ? s_enableHealthSystem : enableHealthSystem.get(); }
    public static boolean getShowDayzToggleChatHint() { return isSynced ? s_showDayzToggleChatHint : showDayzToggleChatHint.get(); }
    public static boolean getEnableVanillaBackpackLock() { return isSynced ? s_enableVanillaBackpackLock : enableVanillaBackpackLock.get(); }
    
    public static boolean shouldReplaceVanillaWalkBobbing() { return isSynced ? s_replaceVanillaWalkBobbing : replaceVanillaWalkBobbing.get(); }
    public static double getWalkSwayStrength() { return isSynced ? s_walkSwayStrength : walkSwayStrength.get(); }
    public static double getWalkSwaySpeed() { return isSynced ? s_walkSwaySpeed : walkSwaySpeed.get(); }
    public static double getIdleSwayStrength() { return isSynced ? s_idleSwayStrength : idleSwayStrength.get(); }
    public static boolean isWalkSwayRequiresSprint() { return isSynced ? s_walkSwayRequiresSprint : walkSwayRequiresSprint.get(); }
    public static boolean isFocusZoomEnabled() { return isSynced ? s_enableFocusZoom : enableFocusZoom.get(); }
    public static double getFocusFovMultiplierWalk() { return isSynced ? s_focusFovMultiplierWalk : focusFovMultiplierWalk.get(); }
    public static double getFocusFovMultiplierSprint() { return isSynced ? s_focusFovMultiplierSprint : focusFovMultiplierSprint.get(); }
    public static double getFocusFovSmoothing() { return isSynced ? s_focusFovSmoothing : focusFovSmoothing.get(); }
    public static boolean isRealFirstPersonEnabled() { return isSynced ? s_enableRealFirstPerson : enableRealFirstPerson.get(); }
    public static boolean isThirdPersonShoulderCameraEnabled() { return isSynced ? s_enableThirdPersonShoulderCamera : enableThirdPersonShoulderCamera.get(); }
    public static boolean isThirdPersonShoulderDefaultRight() { return isSynced ? s_thirdPersonShoulderDefaultRight : thirdPersonShoulderDefaultRight.get(); }
    public static double getThirdPersonShoulderHorizontalOffset() { return isSynced ? s_thirdPersonShoulderHorizontalOffset : thirdPersonShoulderHorizontalOffset.get(); }
    public static double getThirdPersonShoulderVerticalOffset() { return isSynced ? s_thirdPersonShoulderVerticalOffset : thirdPersonShoulderVerticalOffset.get(); }
    public static double getThirdPersonShoulderForwardOffset() { return isSynced ? s_thirdPersonShoulderForwardOffset : thirdPersonShoulderForwardOffset.get(); }
    public static double getThirdPersonShoulderDistanceOffset() { return isSynced ? s_thirdPersonShoulderDistanceOffset : thirdPersonShoulderDistanceOffset.get(); }
    public static double getThirdPersonShoulderSwitchSmoothing() { return isSynced ? s_thirdPersonShoulderSwitchSmoothing : thirdPersonShoulderSwitchSmoothing.get(); }
    public static boolean isThirdPersonFrontViewDisabled() { return isSynced ? s_disableThirdPersonFrontView : disableThirdPersonFrontView.get(); }
    
    public static int getCorpseDespawnTime() { return isSynced ? s_corpseDespawnTime : corpseDespawnTime.get(); }
    public static boolean getEnableCorpse() { return isSynced ? s_enableCorpse : enableCorpse.get(); }
    
    public static boolean getEnableBlueFog() { return isSynced ? s_enableBlueFog : enableBlueFog.get(); }
    public static double getBlueFogTintStrength() { return isSynced ? s_blueFogTintStrength : blueFogTintStrength.get(); }
    public static double getBlueFogDensity() { return isSynced ? s_blueFogDensity : blueFogDensity.get(); }
    public static double getWorldDesaturation() { return isSynced ? s_worldDesaturation : worldDesaturation.get(); }
    public static boolean getEnableVignette() { return isSynced ? s_enableVignette : enableVignette.get(); }
    public static double getVignetteStrength() { return isSynced ? s_vignetteStrength : vignetteStrength.get(); }
    public static boolean isLeanEnabled() { return isSynced ? s_enableLeanSystem : enableLeanSystem.get(); }
    public static double getLeanOffset() { return isSynced ? s_leanOffset : leanOffset.get(); }
    public static double getLeanAngleDegrees() { return isSynced ? s_leanAngleDegrees : leanAngleDegrees.get(); }
    public static double getLeanAnimationDuration() { return isSynced ? s_leanAnimationDuration : leanAnimationDuration.get(); }

    private static <T> T getConfigValue(ForgeConfigSpec.ConfigValue<T> value, T fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return value.get();
        } catch (IllegalStateException ignored) {
            return fallback;
        }
    }

    public static boolean isCustomMainMenuEnabled() { return getConfigValue(enableCustomMainMenu, true); }
    public static int getMainMenuBackgroundRotationSpeed() { return getConfigValue(mainMenuBackgroundRotationSpeed, 20); }
    public static double getMainMenuBackgroundTransitionStep() { return getConfigValue(mainMenuBackgroundTransitionStep, 0.015D); }
    public static double getMainMenuCameraSwayStrength() { return getConfigValue(mainMenuCameraSwayStrength, 12.0D); }
    public static int getPosterRotationSpeed() { return getConfigValue(posterRotationSpeed, 15); }
    public static String getServerAddress() { return getConfigValue(serverAddress, "localhost:25565"); }
    public static String getPosterTitle(int index) {
        return switch (index) {
            case 1 -> getConfigValue(posterTitle1, "BlockZ News");
            case 2 -> getConfigValue(posterTitle2, "Support Us");
            default -> getConfigValue(posterTitle0, "BlockZ Discord");
        };
    }

    public static String getPosterUrl(int index) {
        return switch (index) {
            case 1 -> getConfigValue(posterUrl1, "");
            case 2 -> getConfigValue(posterUrl2, "https://ifdian.net/a/yitianys");
            default -> getConfigValue(posterUrl0, "https://discord.gg/3GktAbyfYt");
        };
    }

    public static String getPosterMessage(int index) {
        return switch (index) {
            case 1 -> getConfigValue(posterMsg1, "尽请期待......");
            case 2 -> getConfigValue(posterMsg2, "感谢支持！正在前往爱发电...");
            default -> getConfigValue(posterMsg0, "正在前往 Discord...");
        };
    }

    public static String getPosterButtonText(int index) {
        return switch (index) {
            case 1 -> getConfigValue(posterButton1, "了解更多");
            case 2 -> getConfigValue(posterButton2, "赞助作者 ↗");
            default -> getConfigValue(posterButton0, "加入 Discord ↗");
        };
    }

    public static int getBackpackCoyoteSlots() { return isSynced ? s_backpackCoyoteSlots : backpackCoyoteSlots.get(); }
    public static int getBackpackAliceSlots() { return isSynced ? s_backpackAliceSlots : backpackAliceSlots.get(); }
    public static int getBackpackCzechSlots() { return isSynced ? s_backpackCzechSlots : backpackCzechSlots.get(); }
    public static int getBackpackCzechPouchSlots() { return isSynced ? s_backpackCzechPouchSlots : backpackCzechPouchSlots.get(); }
    public static int getBackpackPatrolPackSlots() { return isSynced ? s_backpackPatrolPackSlots : backpackPatrolPackSlots.get(); }
    public static int getVest0Slots() { return isSynced ? s_vest0Slots : vest0Slots.get(); }
    public static int getShirtSlots() { return isSynced ? s_shirtSlots : shirtSlots.get(); }
    public static int getPantsSlots() { return isSynced ? s_pantsSlots : pantsSlots.get(); }
    public static int getInitialPocketSlots() { return isSynced ? s_initialPocketSlots : initialPocketSlots.get(); }

    public static boolean isStaminaEnabled() { return isSynced ? s_enableStaminaSystem : enableStaminaSystem.get(); }
    public static double getStaminaMaxCapacity() { return isSynced ? s_staminaMaxCapacity : staminaMaxCapacity.get(); }
    public static double getStaminaSprintCost() { return isSynced ? s_staminaSprintCost : staminaSprintCost.get(); }
    public static double getStaminaJumpCost() { return isSynced ? s_staminaJumpCost : staminaJumpCost.get(); }
    public static double getStaminaRecoveryRate() { return isSynced ? s_staminaRecoveryRate : staminaRecoveryRate.get(); }
    public static double getStaminaWaterPenalty() { return isSynced ? s_staminaWaterPenalty : staminaWaterPenalty.get(); }

    public static boolean isGridEnabled() { return isSynced ? s_enableGridSystem : enableGridSystem.get(); }
    public static int getGridCols() { return isSynced ? s_gridCols : gridCols.get(); }
    public static int getGridRows() { return isSynced ? s_gridRows : gridRows.get(); }

    public static boolean isNursingEnabled() { return isSynced ? s_enableNursingSystem : enableNursingSystem.get(); }
    public static boolean isBleedingEnabled() { return isSynced ? s_enableBleeding : enableBleeding.get(); }
    public static boolean isBrokenLegsEnabled() { return isSynced ? s_enableBrokenLegs : enableBrokenLegs.get(); }
    public static double getBaseBleedingChance() { return isSynced ? s_baseBleedingChance : baseBleedingChance.get(); }
    public static double getBrokenLegChanceMultiplier() { return isSynced ? s_brokenLegChanceMultiplier : brokenLegChanceMultiplier.get(); }
    public static double getBrokenLegMaxChance() { return isSynced ? s_brokenLegMaxChance : brokenLegMaxChance.get(); }

    public static void register() {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        
        b.push("gui");
        enableGridSystem = b.comment("是否启用占格系统 / Enable grid items").define("grid.enable", true);
        gridCols = b.comment("网格列数 / Grid columns").defineInRange("grid.cols", 9, 1, 20);
        gridRows = b.comment("网格行数 / Grid rows").defineInRange("grid.rows", 4, 1, 20);
        uiScale = b.comment("UI 缩放 / UI scale").defineInRange("ui.scale", 1.0, 0.5, 2.0);
        enableDayzInventory = b.comment("是否启用 DayZ 背包界面 / Enable DayZ inventory UI").define("ui.enable_dayz_inventory", true);
        allowPlayerToggleDayz = b.comment("允许玩家切换 DayZ 界面 / Allow player toggle").define("ui.allow_player_toggle", true);
        showDayzHud = b.comment("显示 DayZ HUD 覆盖层 / Show DayZ HUD overlay").define("ui.show_dayz_hud", true);
        enableHealthSystem = b.comment("是否启用健康值系统 / Enable custom health system").define("ui.enable_health_system", true);
        showDayzToggleChatHint = b.comment("显示 DayZ 切换提示 / Show DayZ toggle chat hint").define("ui.show_dayz_toggle_hint", true);
        enableNursingSystem = b.comment("是否启用护理系统（伤口/骨折/绷带等） / Enable nursing system").define("ui.enable_nursing_system", true);
        enableBleeding = b.comment("是否启用流血效果 / Enable bleeding").define("ui.enable_bleeding", true);
        enableBrokenLegs = b.comment("是否启用骨折效果 / Enable broken legs").define("ui.enable_broken_legs", true);
        baseBleedingChance = b.comment("基础流血概率（与伤害值相乘） / Base bleeding chance").defineInRange("ui.base_bleeding_chance", 0.05D, 0.0D, 1.0D);
        brokenLegChanceMultiplier = b.comment("骨折概率倍率（fallDistance/maxFallDistance * multiplier） / Broken leg chance multiplier").defineInRange("ui.broken_leg_chance_multiplier", 0.35D, 0.0D, 1.0D);
        brokenLegMaxChance = b.comment("骨折概率上限 / Broken leg max chance").defineInRange("ui.broken_leg_max_chance", 0.85D, 0.0D, 1.0D);
        enableVanillaBackpackLock = b.comment("是否启用原版背包锁定机制 / Enable vanilla backpack locking").define("ui.enable_vanilla_lock", true);
        initialPocketSlots = b.comment("初始口袋格子数 (无背包时) / Initial pocket slots (without backpack)").defineInRange("ui.initial_pocket_slots", 5, 0, 27);
        b.pop();

        b.push("camera");
        replaceVanillaWalkBobbing = b.comment("是否用自定义镜头摇晃完全替换原版走路摇晃 / Replace vanilla walk bobbing with custom camera sway")
                .define("replace_vanilla_walk_bobbing", true);
        walkSwayStrength = b.comment("走路摇晃强度倍率 / Walk sway strength multiplier")
                .defineInRange("walk_sway_strength", 1.0D, 0.0D, 5.0D);
        walkSwaySpeed = b.comment("走路摇晃速度倍率 / Walk sway speed multiplier")
                .defineInRange("walk_sway_speed", 1.0D, 0.1D, 5.0D);
        idleSwayStrength = b.comment("细微镜头漂移强度倍率 / Idle camera drift strength multiplier")
                .defineInRange("idle_sway_strength", 1.0D, 0.0D, 5.0D);
        walkSwayRequiresSprint = b.comment("是否仅在疾跑时启用主步态摇晃 / Require sprinting for primary walk sway")
                .define("walk_sway_requires_sprint", false);
        enableFocusZoom = b.comment("是否启用注视缩放 / Enable focus zoom")
                .define("enable_focus_zoom", true);
        focusFovMultiplierWalk = b.comment("普通移动时的注视视野倍率，越小看得越远 / Focus FOV multiplier while walking")
                .defineInRange("focus_fov_multiplier_walk", 0.55D, 0.1D, 1.0D);
        focusFovMultiplierSprint = b.comment("奔跑时的注视视野倍率，越小看得越远 / Focus FOV multiplier while sprinting")
                .defineInRange("focus_fov_multiplier_sprint", 0.78D, 0.1D, 1.0D);
        focusFovSmoothing = b.comment("注视缩放平滑速度 / Focus zoom smoothing speed")
                .defineInRange("focus_fov_smoothing", 0.28D, 0.01D, 1.0D);
        enableRealFirstPerson = b.comment("是否启用真实第一人称身体渲染 / Enable true first-person body rendering")
                .define("enable_real_first_person", true);
        enableThirdPersonShoulderCamera = b.comment("是否启用 DayZ 风格第三人称越肩视角 / Enable DayZ-style third-person shoulder camera")
                .define("enable_third_person_shoulder_camera", true);
        thirdPersonShoulderDefaultRight = b.comment("第三人称默认使用右肩视角，关闭则默认左肩 / Use right shoulder as default third-person shoulder")
                .define("third_person_shoulder_default_right", true);
        thirdPersonShoulderHorizontalOffset = b.comment("第三人称越肩横向偏移量 / Third-person shoulder horizontal offset")
                .defineInRange("third_person_shoulder_horizontal_offset", 0.85D, -2.0D, 2.0D);
        thirdPersonShoulderVerticalOffset = b.comment("第三人称越肩垂直偏移量 / Third-person shoulder vertical offset")
                .defineInRange("third_person_shoulder_vertical_offset", 0.15D, -1.0D, 1.5D);
        thirdPersonShoulderForwardOffset = b.comment("第三人称越肩前后偏移量，负值更靠后 / Third-person shoulder forward offset")
                .defineInRange("third_person_shoulder_forward_offset", 0.0D, -1.5D, 1.5D);
        thirdPersonShoulderDistanceOffset = b.comment("第三人称镜头朝角色推进距离，值越大镜头越近 / Extra distance to move shoulder camera closer to the player")
                .defineInRange("third_person_shoulder_distance_offset", 0.95D, 0.0D, 3.0D);
        thirdPersonShoulderSwitchSmoothing = b.comment("左右切肩平滑速度 / Third-person shoulder switching smoothing")
                .defineInRange("third_person_shoulder_switch_smoothing", 0.25D, 0.01D, 1.0D);
        disableThirdPersonFrontView = b.comment("是否禁用前置第三人称（第二人称） / Disable front-facing third person view")
                .define("disable_third_person_front_view", false);
        b.pop();

        b.push("stamina");
        enableStaminaSystem = b.comment("是否启用体力系统 / Enable stamina system").define("stamina.enable", true);
        staminaMaxCapacity = b.comment("基础最大体力值 / Base max stamina").defineInRange("stamina.max_capacity", 100.0D, 1.0D, 1000.0D);
        staminaSprintCost = b.comment("疾跑体力消耗速度 (每tick) / Sprint stamina cost per tick").defineInRange("stamina.sprint_cost", 0.3D, 0.0D, 100.0D);
        staminaJumpCost = b.comment("跳跃体力消耗 / Jump stamina cost").defineInRange("stamina.jump_cost", 4.5D, 0.0D, 100.0D);
        staminaRecoveryRate = b.comment("体力恢复速度 (每tick) / Stamina recovery rate per tick").defineInRange("stamina.recovery_rate", 0.07D, 0.0D, 100.0D);
        staminaWaterPenalty = b.comment("在水中行走的体力消耗额外惩罚 (每tick) / Water movement penalty per tick").defineInRange("stamina.water_penalty", 0.3D, 0.0D, 100.0D);
        b.pop();

        b.push("backpacks");
        backpackCoyoteSlots = b.comment("土狼背包格子数 / Coyote backpack slots").defineInRange("backpack_coyote_slots", 24, 0, 30);
        backpackAliceSlots = b.comment("Alice 背包格子数 / Alice backpack slots").defineInRange("backpack_alice_slots", 20, 0, 30);
        backpackCzechSlots = b.comment("捷克背包格子数 / Czech backpack slots").defineInRange("backpack_czech_slots", 16, 0, 30);
        backpackCzechPouchSlots = b.comment("捷克挂包格子数 / Czech pouch slots").defineInRange("backpack_czechpouch_slots", 6, 0, 30);
        backpackPatrolPackSlots = b.comment("巡逻包格子数 / Patrol pack slots").defineInRange("backpack_patrolpack_slots", 8, 0, 30);
        vest0Slots = b.comment("背心格子数 / Vest slots").defineInRange("vest_0_slots", 12, 0, 30);
        shirtSlots = b.comment("衣服口袋格子数 / Shirt pocket slots").defineInRange("shirt_slots", 6, 0, 30);
        pantsSlots = b.comment("裤子口袋格子数 / Pants pocket slots").defineInRange("pants_slots", 4, 0, 30);
        b.pop();

        b.push("corpse");
        enableCorpse = b.comment("是否启用尸体功能 / Enable corpse system").define("enable_corpse", true);
        corpseDespawnTime = b.comment("尸体消失时间 (秒) / Corpse despawn time (seconds)").defineInRange("corpse_despawn_time", 3600, 1, 86400);
        b.pop();

        b.push("atmosphere");
        enableBlueFog = b.comment("是否启用 BlockZ 冷蓝雾气氛围 / Enable DayZ cold blue atmospheric fog").define("enable_blue_fog", true);
        blueFogTintStrength = b.comment("雾气冷色调强度 (0=无, 1=纯冷色) / Cold fog tint strength").defineInRange("blue_fog_tint_strength", 0.3D, 0.0D, 1.0D);
        blueFogDensity = b.comment("雾气浓度倍率 (1=原版, <1 更浓) / Fog density multiplier (1=vanilla, <1 means denser)").defineInRange("blue_fog_density", 0.65D, 0.2D, 1.0D);
        worldDesaturation = b.comment("世界雾气去饱和度 (0=彩色, 1=纯灰) / World fog desaturation (wasteland feel)").defineInRange("world_desaturation", 0.5D, 0.0D, 1.0D);
        enableVignette = b.comment("是否启用电影黑边暗角 / Enable cinematic vignette overlay").define("enable_vignette", false);
        vignetteStrength = b.comment("黑边暗角强度 (0=无, 1=最强) / Vignette darkness strength").defineInRange("vignette_strength", 0.45D, 0.0D, 1.0D);
        b.pop();

        b.push("lean");
        enableLeanSystem = b.comment("是否启用左右探头系统 (Q/E键) / Enable lean system (Q/E keys)").define("lean.enable", true);
        leanOffset = b.comment("探头身体偏移量 (格) / Lean body offset in blocks").defineInRange("lean.offset", 0.45D, 0.05D, 0.8D);
        leanAngleDegrees = b.comment("探头侧倾角度 (度) / Lean tilt angle in degrees").defineInRange("lean.angle_degrees", 28.0D, 5.0D, 60.0D);
        leanAnimationDuration = b.comment("探头动画总时长 (秒) / Total lean animation duration in seconds").defineInRange("lean.animation_duration", 0.20D, 0.01D, 2.0D);
        b.pop();

        b.push("server");
        serverAddress = b.comment("主菜单'开始游戏'按钮连接的服务器地址 / Server address for direct connect").define("address", "localhost:25565");
        b.pop();

        b.push("mainmenu");
        enableCustomMainMenu = b.comment("是否启用 BlockZ 自定义主菜单 / Enable BlockZ custom main menu").define("enable_custom_mainmenu", true);
        mainMenuBackgroundRotationSpeed = b.comment("主菜单背景轮换速度(秒)，0为不自动轮换 / Main menu background rotation speed in seconds, 0 to disable").defineInRange("background_rotation_speed", 20, 0, 3600);
        mainMenuBackgroundTransitionStep = b.comment("主菜单背景过渡速度(每tick alpha步进) / Main menu background transition alpha step per tick").defineInRange("background_transition_step", 0.015D, 0.001D, 0.2D);
        mainMenuCameraSwayStrength = b.comment("主菜单镜头鼠标晃动强度(像素) / Main menu camera sway strength in pixels").defineInRange("camera_sway_strength", 12.0D, 0.0D, 60.0D);
        b.pop();

        b.push("mainmenu_posters");
        posterTitle0 = b.comment("宣传图 0 标题 / Poster 0 Title").define("title0", "BlockZ Discord");
        posterUrl0 = b.comment("宣传图 0 跳转链接 / Poster 0 URL").define("url0", "https://discord.gg/3GktAbyfYt");
        posterMsg0 = b.comment("宣传图 0 点击提示 / Poster 0 Message").define("msg0", "正在前往 Discord...");
        posterButton0 = b.comment("宣传图 0 按钮文字 / Poster 0 Button Text").define("button0", "加入 Discord ↗");
        
        posterTitle1 = b.comment("宣传图 1 标题 / Poster 1 Title").define("title1", "BlockZ News");
        posterUrl1 = b.comment("宣传图 1 跳转链接 / Poster 1 URL").define("url1", "");
        posterMsg1 = b.comment("宣传图 1 点击提示 / Poster 1 Message").define("msg1", "尽请期待......");
        posterButton1 = b.comment("宣传图 1 按钮文字 / Poster 1 Button Text").define("button1", "了解更多");
        
        posterTitle2 = b.comment("宣传图 2 标题 / Poster 2 Title").define("title2", "Support Us");
        posterUrl2 = b.comment("宣传图 2 跳转链接 / Poster 2 URL").define("url2", "https://ifdian.net/a/yitianys");
        posterMsg2 = b.comment("宣传图 2 点击提示 / Poster 2 Message").define("msg2", "感谢支持！正在前往爱发电...");
        posterButton2 = b.comment("宣传图 2 按钮文字 / Poster 2 Button Text").define("button2", "赞助作者 ↗");
        
        posterRotationSpeed = b.comment("宣传图轮换速度 (秒), 0 为不自动轮换 / Poster rotation speed in seconds, 0 to disable").defineInRange("rotation_speed", 15, 0, 3600);
        b.pop();
        
        COMMON_SPEC = b.build();
    }

    

    /**
     * 获取指定物品提供的背包格子数
     */
    public static int getBackpackSlots(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Item item = stack.getItem();
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
        if (rl == null) return 0;

        int customSlots = ItemSizeManager.getCustomSlots(stack);
        if (customSlots >= 0) return customSlots;

        String name = rl.getPath();
        if (name.equals("backpack_coyote")) return getBackpackCoyoteSlots();
        if (name.equals("backpack_alice")) return getBackpackAliceSlots();
        if (name.equals("backpack_czech")) return getBackpackCzechSlots();
        if (name.equals("backpack_czechpouch")) return getBackpackCzechPouchSlots();
        if (name.equals("backpack_patrolpack")) return getBackpackPatrolPackSlots();
        if (name.equals("vest_0")) return getVest0Slots();
        
        // 衣服和裤子通用配置
        if (name.startsWith("shirt_") || name.equals("shirt")) {
             return getShirtSlots();
        }
        if (name.startsWith("pants_") || name.equals("pants")) return getPantsSlots();
        
        // 兼容旧的
        if (name.equals("small_backpack")) return 9;
        if (name.equals("medium_backpack")) return 15;
        if (name.equals("large_backpack")) return 22;
        if (name.equals("vest")) return 6;
        
        if (rl.getNamespace().equals("blockz") && (name.contains("shirt") || name.contains("pants"))) {
            com.yitianys.BlockZ.BlockZ.LOGGER.warn("BlockZConfigs: Item {} (path={}) has 0 slots! CustomSlots={}, ShirtSlots={}, PantsSlots={}", 
                rl, name, customSlots, shirtSlots.get(), pantsSlots.get());
        }

        return 0;
    }
}

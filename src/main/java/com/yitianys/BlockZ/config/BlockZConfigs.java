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
    public static ForgeConfigSpec.BooleanValue allowPlayerToggleDayz;
    public static ForgeConfigSpec.BooleanValue showDayzHud;
    public static ForgeConfigSpec.BooleanValue showDayzToggleChatHint;
    public static ForgeConfigSpec.BooleanValue enableNursingSystem;
    public static ForgeConfigSpec.BooleanValue enableBleeding;
    public static ForgeConfigSpec.BooleanValue enableBrokenLegs;
    public static ForgeConfigSpec.DoubleValue baseBleedingChance;
    public static ForgeConfigSpec.DoubleValue brokenLegChanceMultiplier;
    public static ForgeConfigSpec.DoubleValue brokenLegMaxChance;
    public static ForgeConfigSpec.BooleanValue enableVanillaBackpackLock;
    public static ForgeConfigSpec.IntValue initialPocketSlots;

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

    public static void register() {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        
        b.push("gui");
        enableGridSystem = b.comment("是否启用占格系统 / Enable grid items").define("grid.enable", true);
        gridCols = b.comment("网格列数 / Grid columns").defineInRange("grid.cols", 9, 1, 20);
        gridRows = b.comment("网格行数 / Grid rows").defineInRange("grid.rows", 4, 1, 20);
        uiScale = b.comment("UI 缩放 / UI scale").defineInRange("ui.scale", 1.0, 0.5, 2.0);
        allowPlayerToggleDayz = b.comment("允许玩家切换 DayZ 界面 / Allow player toggle").define("ui.allow_player_toggle", true);
        showDayzHud = b.comment("显示 DayZ HUD 覆盖层 / Show DayZ HUD overlay").define("ui.show_dayz_hud", true);
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
        enableVignette = b.comment("是否启用电影黑边暗角 / Enable cinematic vignette overlay").define("enable_vignette", true);
        vignetteStrength = b.comment("黑边暗角强度 (0=无, 1=最强) / Vignette darkness strength").defineInRange("vignette_strength", 0.45D, 0.0D, 1.0D);
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
        if (name.equals("backpack_coyote")) return backpackCoyoteSlots.get();
        if (name.equals("backpack_alice")) return backpackAliceSlots.get();
        if (name.equals("backpack_czech")) return backpackCzechSlots.get();
        if (name.equals("backpack_czechpouch")) return backpackCzechPouchSlots.get();
        if (name.equals("backpack_patrolpack")) return backpackPatrolPackSlots.get();
        if (name.equals("vest_0")) return vest0Slots.get();
        
        // 衣服和裤子通用配置
        if (name.startsWith("shirt_") || name.equals("shirt")) {
             return shirtSlots.get();
        }
        if (name.startsWith("pants_") || name.equals("pants")) return pantsSlots.get();
        
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

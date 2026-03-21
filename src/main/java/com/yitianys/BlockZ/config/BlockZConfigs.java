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
        baseBleedingChance = b.comment("基础流血概率（与伤害值相乘） / Base bleeding chance").defineInRange("ui.base_bleeding_chance", 0.1D, 0.0D, 1.0D);
        brokenLegChanceMultiplier = b.comment("骨折概率倍率（fallDistance/maxFallDistance * multiplier） / Broken leg chance multiplier").defineInRange("ui.broken_leg_chance_multiplier", 0.35D, 0.0D, 1.0D);
        brokenLegMaxChance = b.comment("骨折概率上限 / Broken leg max chance").defineInRange("ui.broken_leg_max_chance", 0.85D, 0.0D, 1.0D);
        enableVanillaBackpackLock = b.comment("是否启用原版背包锁定机制 / Enable vanilla backpack locking").define("ui.enable_vanilla_lock", true);
        initialPocketSlots = b.comment("初始口袋格子数 (无背包时) / Initial pocket slots (without backpack)").defineInRange("ui.initial_pocket_slots", 5, 0, 27);
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

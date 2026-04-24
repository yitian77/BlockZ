package com.yitianys.BlockZ.init;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.item.BandageItem;
import com.yitianys.BlockZ.item.BackpackItem;
import com.yitianys.BlockZ.item.ClothingItem;
import com.yitianys.BlockZ.item.CodeinePillsItem;
import com.yitianys.BlockZ.item.LockItem;
import com.yitianys.BlockZ.item.MorphineSyringeItem;
import com.yitianys.BlockZ.item.RagsItem;
import com.yitianys.BlockZ.item.SplintItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BlockZ.MODID);

    // --- 背包 ---
    public static final RegistryObject<Item> BACKPACK_COYOTE = ITEMS.register("backpack_coyote", 
        () -> new BackpackItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BACKPACK_ALICE = ITEMS.register("backpack_alice", 
        () -> new BackpackItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BACKPACK_CZECH = ITEMS.register("backpack_czech", 
        () -> new BackpackItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BACKPACK_CZECHPOUCH = ITEMS.register("backpack_czechpouch", 
        () -> new BackpackItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BACKPACK_PATROLPACK = ITEMS.register("backpack_patrolpack", 
        () -> new BackpackItem(new Item.Properties().stacksTo(1)));
        
    // --- 衣服装备 ---
    // 头部

    // 背心
    public static final RegistryObject<Item> VEST_0 = ITEMS.register("vest_0",
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.VEST));
        
    // 手套
    public static final RegistryObject<Item> GLOVES_0 = ITEMS.register("gloves_0", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.GLOVES));
    public static final RegistryObject<Item> GLOVES_1 = ITEMS.register("gloves_1", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.GLOVES));
    public static final RegistryObject<Item> GLOVES_2 = ITEMS.register("gloves_2", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.GLOVES));

    // 上衣
    public static final RegistryObject<Item> SHIRT_0 = ITEMS.register("shirt_0", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHIRT));
    public static final RegistryObject<Item> SHIRT_1 = ITEMS.register("shirt_1", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHIRT));
    public static final RegistryObject<Item> SHIRT_2 = ITEMS.register("shirt_2", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHIRT));
    public static final RegistryObject<Item> SHIRT_3 = ITEMS.register("shirt_3", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHIRT));
    public static final RegistryObject<Item> SHIRT_4 = ITEMS.register("shirt_4", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHIRT));
    public static final RegistryObject<Item> SHIRT_5 = ITEMS.register("shirt_5", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHIRT));
    public static final RegistryObject<Item> SHIRT_6 = ITEMS.register("shirt_6", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHIRT));
    public static final RegistryObject<Item> SHIRT_7 = ITEMS.register("shirt_7", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHIRT));
    public static final RegistryObject<Item> SHIRT_8 = ITEMS.register("shirt_8", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHIRT));
    public static final RegistryObject<Item> SHIRT_9 = ITEMS.register("shirt_9", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHIRT));
    public static final RegistryObject<Item> SHIRT_10 = ITEMS.register("shirt_10", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHIRT));
    public static final RegistryObject<Item> SHIRT_11 = ITEMS.register("shirt_11", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHIRT));
    public static final RegistryObject<Item> SHIRT_12 = ITEMS.register("shirt_12", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHIRT));
    public static final RegistryObject<Item> SHIRT_13 = ITEMS.register("shirt_13", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHIRT));
    public static final RegistryObject<Item> SHIRT_14 = ITEMS.register("shirt_14", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHIRT));

    // 裤子
    public static final RegistryObject<Item> PANTS_0 = ITEMS.register("pants_0", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.PANTS));
    public static final RegistryObject<Item> PANTS_1 = ITEMS.register("pants_1", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.PANTS));
    public static final RegistryObject<Item> PANTS_2 = ITEMS.register("pants_2", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.PANTS));
    public static final RegistryObject<Item> PANTS_3 = ITEMS.register("pants_3", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.PANTS));
    public static final RegistryObject<Item> PANTS_4 = ITEMS.register("pants_4", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.PANTS));
    public static final RegistryObject<Item> PANTS_5 = ITEMS.register("pants_5", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.PANTS));

    // 鞋子
    public static final RegistryObject<Item> SHOES_0 = ITEMS.register("shoes_0", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHOES));
    public static final RegistryObject<Item> SHOES_1 = ITEMS.register("shoes_1", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHOES));
    public static final RegistryObject<Item> SHOES_2 = ITEMS.register("shoes_2", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHOES));
    public static final RegistryObject<Item> SHOES_3 = ITEMS.register("shoes_3", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHOES));

    // 旧项保留 (可选)
    public static final RegistryObject<Item> GLOVES = ITEMS.register("gloves", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.GLOVES));
    public static final RegistryObject<Item> SHIRT = ITEMS.register("shirt", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHIRT));
    public static final RegistryObject<Item> PANTS = ITEMS.register("pants", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.PANTS));
    public static final RegistryObject<Item> SHOES = ITEMS.register("shoes", 
        () -> new ClothingItem(new Item.Properties().stacksTo(1), ClothingItem.ClothingType.SHOES));

    public static final RegistryObject<Item> SPLINT = ITEMS.register("splint",
        () -> new SplintItem(new Item.Properties().stacksTo(4)));

    public static final RegistryObject<Item> BANDAGE = ITEMS.register("bandage",
        () -> new BandageItem(new Item.Properties().stacksTo(8)));

    public static final RegistryObject<Item> RAGS = ITEMS.register("rags",
        () -> new RagsItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> MORPHINE_SYRINGE = ITEMS.register("morphine_syringe",
        () -> new MorphineSyringeItem(new Item.Properties().stacksTo(4)));

    public static final RegistryObject<Item> CODEINE_PILLS = ITEMS.register("codeine_pills",
        () -> new CodeinePillsItem(new Item.Properties().stacksTo(8)));

    public static final RegistryObject<Item> DAYZ_ZOMBIE_SPAWN_EGG = ITEMS.register("dayz_zombie_spawn_egg",
        () -> new ForgeSpawnEggItem(ModEntities.DAYZ_ZOMBIE, 0x5b6a59, 0xa58971, new Item.Properties()));

    // --- 系统物品 ---
    public static final RegistryObject<Item> LOCK_ITEM = ITEMS.register("lock_item",
        () -> new LockItem(new Item.Properties().stacksTo(1)));
}

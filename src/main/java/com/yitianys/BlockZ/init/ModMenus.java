package com.yitianys.BlockZ.init;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.common.extensions.IForgeMenuType;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, BlockZ.MODID);
    public static final RegistryObject<MenuType<DayZInventoryMenu>> DAYZ_INVENTORY = MENUS.register("dayz_inventory", () -> IForgeMenuType.create(DayZInventoryMenu::fromNetwork));
}

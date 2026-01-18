package com.yitianys.BlockZ.init;

import com.yitianys.BlockZ.BlockZ;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BlockZ.MODID);

    public static final RegistryObject<CreativeModeTab> BLOCKZ_TAB = CREATIVE_MODE_TABS.register("blockz_tab",
        () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.SMALL_BACKPACK.get()))
            .title(Component.translatable("creativetab.blockz_tab"))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.SMALL_BACKPACK.get());
                output.accept(ModItems.MEDIUM_BACKPACK.get());
                output.accept(ModItems.LARGE_BACKPACK.get());
            })
            .build());
}

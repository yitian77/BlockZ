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

    // 捷克挂包图标作为 Tab 图标
    public static final RegistryObject<CreativeModeTab> BLOCKZ_TAB = CREATIVE_MODE_TABS.register("blockz_tab",
        () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.BACKPACK_CZECHPOUCH.get()))
            .title(Component.translatable("creativetab.blockz_tab"))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.BACKPACK_COYOTE.get());
                output.accept(ModItems.BACKPACK_ALICE.get());
                output.accept(ModItems.BACKPACK_CZECH.get());
                output.accept(ModItems.BACKPACK_CZECHPOUCH.get());
                output.accept(ModItems.BACKPACK_PATROLPACK.get());
                output.accept(ModItems.VEST_0.get());
                
                output.accept(ModItems.SHIRT_0.get());
                output.accept(ModItems.SHIRT_1.get());
                output.accept(ModItems.SHIRT_2.get());
                output.accept(ModItems.SHIRT_3.get());
                output.accept(ModItems.SHIRT_4.get());
                output.accept(ModItems.SHIRT_5.get());
                output.accept(ModItems.SHIRT_6.get());
                output.accept(ModItems.SHIRT_7.get());
                output.accept(ModItems.SHIRT_8.get());
                output.accept(ModItems.SHIRT_9.get());
                output.accept(ModItems.SHIRT_10.get());
                output.accept(ModItems.SHIRT_11.get());
                output.accept(ModItems.SHIRT_12.get());
                output.accept(ModItems.SHIRT_13.get());
                output.accept(ModItems.SHIRT_14.get());
                
                output.accept(ModItems.PANTS_0.get());
                output.accept(ModItems.PANTS_1.get());
                output.accept(ModItems.PANTS_2.get());
                output.accept(ModItems.PANTS_3.get());
                output.accept(ModItems.PANTS_4.get());
                output.accept(ModItems.PANTS_5.get());
                
                output.accept(ModItems.SHOES_0.get());
                output.accept(ModItems.SHOES_1.get());
                output.accept(ModItems.SHOES_2.get());
                output.accept(ModItems.SHOES_3.get());

                output.accept(ModItems.GLOVES_0.get());
                output.accept(ModItems.GLOVES_1.get());
                output.accept(ModItems.GLOVES_2.get());
                output.accept(ModItems.DAYZ_ZOMBIE_SPAWN_EGG.get());
            })
            .build());

    public static final RegistryObject<CreativeModeTab> NURSING_TAB = CREATIVE_MODE_TABS.register("nursing_tab",
        () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.BANDAGE.get()))
            .title(Component.translatable("creativetab.blockz_nursing_tab"))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.BANDAGE.get());
                output.accept(ModItems.SPLINT.get());
                output.accept(ModItems.RAGS.get());
                output.accept(ModItems.MORPHINE_SYRINGE.get());
                output.accept(ModItems.CODEINE_PILLS.get());
            })
            .build());
}

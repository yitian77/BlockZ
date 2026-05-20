package com.yitianys.BlockZ.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class TaczRenderCompat {
    private static final ResourceLocation MODERN_KINETIC_GUN_ID = ResourceLocation.fromNamespaceAndPath("tacz", "modern_kinetic_gun");

    private TaczRenderCompat() {
    }

    public static boolean isTaczGun(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return MODERN_KINETIC_GUN_ID.equals(itemId);
    }
}

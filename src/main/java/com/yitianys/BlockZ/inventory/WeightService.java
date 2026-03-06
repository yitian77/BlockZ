package com.yitianys.BlockZ.inventory;

import net.minecraft.world.item.ItemStack;

public class WeightService {
    public static double weightOf(ItemStack stack) {
        return Math.max(0.1, stack.getMaxStackSize() > 1 ? 0.1 : 1.0) * stack.getCount();
    }
}

package com.yitianys.BlockZ.inventory;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.PotionItem;

public class CategoryService {
    public enum Category {WEAPON, FOOD, MEDICAL, OTHER}

    public static Category of(ItemStack stack) {
        if (stack.getItem() instanceof SwordItem) return Category.WEAPON;
        if (stack.getItem().isEdible()) return Category.FOOD;
        if (stack.getItem() instanceof PotionItem) return Category.MEDICAL;
        return Category.OTHER;
    }
}

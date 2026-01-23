package com.yitianys.BlockZ.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BackpackItem extends Item {
    public BackpackItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

        if (!stack.hasTag()) return;
        CompoundTag tag = stack.getTag();
        if (!tag.contains("Inventory")) return;
        
        CompoundTag invTag = tag.getCompound("Inventory");
        if (!invTag.contains("Items")) return;

        ListTag items = invTag.getList("Items", 10);
        if (items.isEmpty()) return;

        tooltipComponents.add(Component.literal("§7Contents:"));
        
        int count = 0;
        for (int i = 0; i < items.size(); i++) {
            if (count >= 5) { // Limit to 5 items to prevent huge tooltips
                tooltipComponents.add(Component.literal("§7... and " + (items.size() - count) + " more"));
                break;
            }
            
            CompoundTag itemTag = items.getCompound(i);
            ItemStack itemStack = ItemStack.of(itemTag);
            
            if (!itemStack.isEmpty()) {
                tooltipComponents.add(Component.literal("§8- " + itemStack.getHoverName().getString() + " x" + itemStack.getCount()));
                count++;
            }
        }
    }
}

package com.yitianys.BlockZ.menu;

import java.util.List;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class VicinityManager {
    private VicinityManager() {}

    public static int fillGroundItems(SimpleContainer vicinityInventory, Player player, List<ItemEntity> sink, int limit) {
        int slotIndex = 0;
        double range = 2.0D;
        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class,
            player.getBoundingBox().inflate(range),
            entity -> entity.distanceTo(player) <= range);
        sink.clear();
        for (ItemEntity entity : items) {
            if (slotIndex >= limit) break;
            sink.add(entity);
            vicinityInventory.setItem(slotIndex, entity.getItem());
            slotIndex++;
        }
        while (slotIndex < limit) {
            if (!vicinityInventory.getItem(slotIndex).isEmpty()) {
                vicinityInventory.setItem(slotIndex, ItemStack.EMPTY);
            }
            slotIndex++;
        }
        return Math.min(items.size(), limit);
    }
}

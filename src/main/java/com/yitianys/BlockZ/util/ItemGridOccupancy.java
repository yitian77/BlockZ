package com.yitianys.BlockZ.util;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public final class ItemGridOccupancy {
    private ItemGridOccupancy() {}

    public static boolean[] computeBlocked(IItemHandler handler, int cols, int sectionStart, int sectionSize) {
        return computeBlocked(handler, cols, sectionStart, sectionSize, ItemStack.EMPTY);
    }

    public static boolean[] computeBlocked(IItemHandler handler, int cols, int sectionStart, int sectionSize, ItemStack ignoreItem) {
        int end = Math.min(handler.getSlots(), sectionStart + sectionSize);
        boolean[] blocked = new boolean[sectionSize]; // Relative to sectionStart
        if (!ItemSizeManager.isGridEnabled()) return blocked;

        for (int i = sectionStart; i < end; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            // Ignore the specific item instance (reference equality is safer for "this specific item in slot")
            // But handler.getStackInSlot returns the stack. If ignoreItem is passed from slot.getItem(), it should be the same object.
            if (!ignoreItem.isEmpty() && stack == ignoreItem) continue;

            ItemSizeManager.ItemSize size = ItemSizeManager.getSize(stack);
            int w = Math.max(1, size.width());
            int h = Math.max(1, size.height());

            // Coordinates relative to section start
            int relIndex = i - sectionStart;
            int col = relIndex % cols;
            int row = relIndex / cols;

            // Check if item fits within section bounds
            if (col + w > cols) {
                // Item extends beyond column width (shouldn't happen if placed correctly, but marks as blocked)
                if (relIndex < blocked.length) blocked[relIndex] = true;
                continue;
            }

            for (int r = 0; r < h; r++) {
                for (int c = 0; c < w; c++) {
                    int targetRelIdx = (row + r) * cols + (col + c);
                    if (targetRelIdx >= 0 && targetRelIdx < blocked.length) {
                        blocked[targetRelIdx] = true;
                    }
                }
            }
        }

        return blocked;
    }

    public static boolean canPlaceAt(boolean[] blocked, int cols, int sectionSize, int relAnchorIndex, ItemSizeManager.ItemSize size) {
        if (sectionSize <= 0) return false;
        if (relAnchorIndex < 0 || relAnchorIndex >= sectionSize) return false;

        int w = Math.max(1, size.width());
        int h = Math.max(1, size.height());

        int col = relAnchorIndex % cols;
        int row = relAnchorIndex / cols;

        if (col + w > cols) return false;

        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                int targetRelIdx = (row + r) * cols + (col + c);
                // Must fit within section size
                if (targetRelIdx < 0 || targetRelIdx >= sectionSize) return false;
                // Must not be blocked
                if (targetRelIdx < blocked.length && blocked[targetRelIdx]) return false;
            }
        }

        return true;
    }
}

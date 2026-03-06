package com.yitianys.BlockZ.menu.slot;

import com.yitianys.BlockZ.util.ItemGridOccupancy;
import com.yitianys.BlockZ.util.ItemSizeManager;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

public class TetrisSlot extends SlotItemHandler {
    private int cols;
    private final IntSupplier effectiveSlotsSupplier;
    private final Predicate<ItemStack> disallowed;

    public TetrisSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition, int cols, IntSupplier effectiveSlotsSupplier, Predicate<ItemStack> disallowed) {
        super(itemHandler, index, xPosition, yPosition);
        this.cols = cols;
        this.effectiveSlotsSupplier = Objects.requireNonNull(effectiveSlotsSupplier);
        this.disallowed = Objects.requireNonNull(disallowed);
    }

    private int sectionStart = 0;
    private int sectionSize = 0;

    public void setSectionBounds(int start, int size) {
        this.sectionStart = start;
        this.sectionSize = size;
    }

    public void setSectionGridCols(int cols) {
        this.cols = cols;
    }

    @Override
    public boolean isActive() {
        if (sectionSize <= 0) return false;
        int index = this.getSlotIndex();
        return index >= sectionStart && index < sectionStart + sectionSize;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (!isActive()) return false;
        if (stack.isEmpty()) return false;
        if (disallowed.test(stack)) return false;

        int anchor = this.getSlotIndex();
        if (anchor < sectionStart || anchor >= sectionStart + sectionSize) return false;

        ItemStack existing = this.getItem();
        if (!existing.isEmpty()) {
            ItemSizeManager.ItemSize incomingSize = ItemSizeManager.getSize(stack);
            
            // Check if the new item can fit if we remove the existing one
            // We use computeBlocked with the existing item ignored
            boolean[] blocked = ItemGridOccupancy.computeBlocked(this.getItemHandler(), cols, sectionStart, sectionSize, existing);
            return ItemGridOccupancy.canPlaceAt(blocked, cols, sectionSize, anchor - sectionStart, incomingSize);
        }

        ItemSizeManager.ItemSize size = ItemSizeManager.getSize(stack);
        // Compute blocked slots ONLY for this section
        boolean[] blocked = ItemGridOccupancy.computeBlocked(this.getItemHandler(), cols, sectionStart, sectionSize);
        // Check placement relative to section start
        return ItemGridOccupancy.canPlaceAt(blocked, cols, sectionSize, anchor - sectionStart, size);
    }

    @Override
    public boolean mayPickup(net.minecraft.world.entity.player.Player playerIn) {
        return isActive() && super.mayPickup(playerIn);
    }
}

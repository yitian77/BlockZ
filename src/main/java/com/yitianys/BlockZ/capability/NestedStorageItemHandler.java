package com.yitianys.BlockZ.capability;

import com.yitianys.BlockZ.config.BlockZConfigs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import java.util.function.Supplier;

public class NestedStorageItemHandler implements IItemHandler {
    private final Supplier<ItemStack> stackSupplier;
    private ItemStack observedStack = ItemStack.EMPTY;
    private ItemStackHandler cachedHandler = new ItemStackHandler(0);
    private CompoundTag observedInventoryTag = new CompoundTag();
    private int observedSlotCount = 0;

    public NestedStorageItemHandler(Supplier<ItemStack> stackSupplier) {
        this.stackSupplier = stackSupplier;
    }

    public void syncToStack() {
        refreshState(true);
    }

    private void refreshCache() {
        refreshState(false);
    }

    private void refreshState(boolean persistCacheChanges) {
        ItemStack currentStack = stackSupplier.get();
        int currentSlots = Math.max(BlockZConfigs.getBackpackSlots(currentStack), 0);
        CompoundTag currentInventoryTag = readInventoryTag(currentStack);
        CompoundTag cachedInventoryTag = snapshotCachedInventory();
        boolean cacheChanged = !cachedInventoryTag.equals(observedInventoryTag);
        boolean stackChanged = currentStack != observedStack || currentSlots != observedSlotCount;
        boolean externalInventoryChanged = !currentInventoryTag.equals(observedInventoryTag);

        if (stackChanged) {
            if (cacheChanged) {
                saveCurrent();
            }
            loadFromStack(currentStack, currentSlots, currentInventoryTag);
            return;
        }

        if (externalInventoryChanged && !cacheChanged) {
            loadFromStack(currentStack, currentSlots, currentInventoryTag);
            return;
        }

        if (persistCacheChanges && cacheChanged) {
            saveCurrent();
        }
    }

    private void loadFromStack(ItemStack currentStack, int slots, CompoundTag inventoryTag) {
        observedStack = currentStack;
        observedSlotCount = slots;
        observedInventoryTag = inventoryTag.copy();
        cachedHandler = new ItemStackHandler(slots);
        if (currentStack.isEmpty() || inventoryTag.isEmpty()) {
            return;
        }
        cachedHandler.deserializeNBT(inventoryTag);
    }

    private void saveCurrent() {
        if (observedStack.isEmpty()) {
            return;
        }
        CompoundTag tag = observedStack.getTag();
        boolean hasItems = hasAnyItems();
        if (!hasItems) {
            if (tag != null) {
                tag.remove("Inventory");
                if (tag.isEmpty()) {
                    observedStack.setTag(null);
                }
            }
            observedInventoryTag = new CompoundTag();
            return;
        }
        CompoundTag saveTag = observedStack.getOrCreateTag();
        CompoundTag serialized = cachedHandler.serializeNBT();
        saveTag.put("Inventory", serialized);
        observedInventoryTag = serialized.copy();
    }

    private CompoundTag readInventoryTag(ItemStack stack) {
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("Inventory", Tag.TAG_COMPOUND)) {
            return new CompoundTag();
        }
        return tag.getCompound("Inventory").copy();
    }

    private CompoundTag snapshotCachedInventory() {
        if (!hasAnyItems()) {
            return new CompoundTag();
        }
        return cachedHandler.serializeNBT();
    }

    private boolean hasAnyItems() {
        for (int i = 0; i < cachedHandler.getSlots(); i++) {
            if (!cachedHandler.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidSlot(int slot) {
        refreshCache();
        return slot >= 0 && slot < cachedHandler.getSlots();
    }

    @Override
    public int getSlots() {
        refreshCache();
        return cachedHandler.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        return cachedHandler.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!isValidSlot(slot) || stack.isEmpty()) {
            return stack;
        }
        ItemStack remaining = cachedHandler.insertItem(slot, stack, simulate);
        if (!simulate) {
            saveCurrent();
        }
        return remaining;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!isValidSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack extracted = cachedHandler.extractItem(slot, amount, simulate);
        if (!simulate && !extracted.isEmpty()) {
            saveCurrent();
        }
        return extracted;
    }

    @Override
    public int getSlotLimit(int slot) {
        if (!isValidSlot(slot)) {
            return 0;
        }
        return cachedHandler.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isValidSlot(slot) && cachedHandler.isItemValid(slot, stack);
    }
}

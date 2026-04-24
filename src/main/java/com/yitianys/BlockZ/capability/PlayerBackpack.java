package com.yitianys.BlockZ.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.ItemStackHandler;

public class PlayerBackpack implements INBTSerializable<CompoundTag> {
    public static final int SLOT_BACKPACK = 0;
    public static final int SLOT_VEST = 1;
    public static final int SLOT_GLOVES = 2;
    public static final int SLOT_MASK = 3;
    public static final int SLOT_COUNT = 4;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT);
    private boolean dayzEnabled = true;

    public ItemStackHandler getInventory() {
        ensureInventorySize();
        return inventory;
    }

    public boolean isDayzEnabled() {
        return dayzEnabled;
    }

    public void setDayzEnabled(boolean dayzEnabled) {
        this.dayzEnabled = dayzEnabled;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        
        // 始终完整保存所有槽位，确保数据持久化安全
        // 即使 Curios 存在，我们也应保留自身 Capability 的数据作为备份或主存储
        nbt.put("Inventory", inventory.serializeNBT());
        
        nbt.putBoolean("DayzEnabled", dayzEnabled);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("Inventory")) {
            CompoundTag invNbt = nbt.getCompound("Inventory");
            int expectedSize = SLOT_COUNT;
            inventory.deserializeNBT(invNbt);
            
            if (inventory.getSlots() != expectedSize) {
                repairInventorySize(expectedSize);
            }
        }
        if (nbt.contains("DayzEnabled")) {
            dayzEnabled = nbt.getBoolean("DayzEnabled");
        } else {
            dayzEnabled = true;
        }
    }

    public void copyFrom(PlayerBackpack other) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            this.inventory.setStackInSlot(i, other.inventory.getStackInSlot(i).copy());
        }
        this.dayzEnabled = other.dayzEnabled;
    }

    private void ensureInventorySize() {
        int expectedSize = SLOT_COUNT;
        if (inventory.getSlots() != expectedSize) {
            repairInventorySize(expectedSize);
        }
    }

    private void repairInventorySize(int expectedSize) {
        ItemStackHandler newInv = new ItemStackHandler(expectedSize);
        for (int i = 0; i < Math.min(expectedSize, inventory.getSlots()); i++) {
            newInv.setStackInSlot(i, inventory.getStackInSlot(i));
        }
        CompoundTag fixedNbt = newInv.serializeNBT();
        inventory.deserializeNBT(fixedNbt);
    }
}

package com.yitianys.BlockZ.capability;

import com.yitianys.BlockZ.menu.StorageRefreshableMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

public class BlockZPlayerItemHandler implements IItemHandler {
    private final Player player;
    private final IItemHandler playerInventoryHandler;
    private final NestedStorageItemHandler vestHandler;
    private final NestedStorageItemHandler shirtHandler;
    private final NestedStorageItemHandler pantsHandler;

    public BlockZPlayerItemHandler(Player player) {
        this.player = player;
        this.playerInventoryHandler = new InvWrapper(player.getInventory());
        this.vestHandler = new NestedStorageItemHandler(() -> player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                .map(cap -> cap.getInventory().getStackInSlot(PlayerBackpack.SLOT_VEST))
                .orElse(ItemStack.EMPTY));
        this.shirtHandler = new NestedStorageItemHandler(() -> player.getInventory().getArmor(2));
        this.pantsHandler = new NestedStorageItemHandler(() -> player.getInventory().getArmor(1));
    }

    public void syncNestedStorages() {
        vestHandler.syncToStack();
        shirtHandler.syncToStack();
        pantsHandler.syncToStack();
    }

    @Override
    public int getSlots() {
        return playerInventoryHandler.getSlots()
                + vestHandler.getSlots()
                + shirtHandler.getSlots()
                + pantsHandler.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        SlotAccess access = resolve(slot);
        return access == null ? ItemStack.EMPTY : access.handler().getStackInSlot(access.slot());
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        SlotAccess access = resolve(slot);
        if (access == null) {
            return stack;
        }
        ItemStack remaining = access.handler().insertItem(access.slot(), stack, simulate);
        if (!simulate && access.handler() != playerInventoryHandler && remaining.getCount() != stack.getCount()) {
            refreshOpenStorageMenu();
        }
        return remaining;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        SlotAccess access = resolve(slot);
        if (access == null) {
            return ItemStack.EMPTY;
        }
        ItemStack extracted = access.handler().extractItem(access.slot(), amount, simulate);
        if (!simulate && access.handler() != playerInventoryHandler && !extracted.isEmpty()) {
            refreshOpenStorageMenu();
        }
        return extracted;
    }

    @Override
    public int getSlotLimit(int slot) {
        SlotAccess access = resolve(slot);
        return access == null ? 0 : access.handler().getSlotLimit(access.slot());
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        SlotAccess access = resolve(slot);
        return access != null && access.handler().isItemValid(access.slot(), stack);
    }

    private SlotAccess resolve(int slot) {
        if (slot < 0) {
            return null;
        }
        int remaining = slot;
        if (remaining < playerInventoryHandler.getSlots()) {
            return new SlotAccess(playerInventoryHandler, remaining);
        }
        remaining -= playerInventoryHandler.getSlots();
        if (remaining < vestHandler.getSlots()) {
            return new SlotAccess(vestHandler, remaining);
        }
        remaining -= vestHandler.getSlots();
        if (remaining < shirtHandler.getSlots()) {
            return new SlotAccess(shirtHandler, remaining);
        }
        remaining -= shirtHandler.getSlots();
        if (remaining < pantsHandler.getSlots()) {
            return new SlotAccess(pantsHandler, remaining);
        }
        return null;
    }

    private void refreshOpenStorageMenu() {
        if (player.containerMenu instanceof StorageRefreshableMenu menu) {
            menu.blockz$refreshStorageAfterExternalChange();
        }
    }

    private record SlotAccess(IItemHandler handler, int slot) {
    }
}

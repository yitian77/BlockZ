package com.yitianys.BlockZ.util;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * A wrapper that exposes an IItemHandler as a net.minecraft.world.Container.
 */
public class ItemHandlerContainer implements Container {
    private final IItemHandler itemHandler;
    private final Container container;

    public ItemHandlerContainer(IItemHandler itemHandler) {
        this.itemHandler = itemHandler;
        this.container = itemHandler instanceof net.minecraftforge.items.wrapper.InvWrapper wrapper ? wrapper.getInv() : null;
    }

    @Override
    public int getContainerSize() {
        return itemHandler.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return itemHandler.getStackInSlot(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return itemHandler.extractItem(index, count, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack stack = itemHandler.getStackInSlot(index);
        if (itemHandler instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(index, ItemStack.EMPTY);
            return stack;
        }
        return itemHandler.extractItem(index, stack.getCount(), false);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (itemHandler instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(index, stack);
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        if (container != null) {
            container.setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (container != null) {
            return container.stillValid(player);
        }
        return true;
    }

    @Override
    public void clearContent() {
        if (itemHandler instanceof IItemHandlerModifiable modifiable) {
            for (int i = 0; i < modifiable.getSlots(); i++) {
                modifiable.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void startOpen(Player player) {
        if (container != null) {
            container.startOpen(player);
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (container != null) {
            container.stopOpen(player);
        }
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }
}

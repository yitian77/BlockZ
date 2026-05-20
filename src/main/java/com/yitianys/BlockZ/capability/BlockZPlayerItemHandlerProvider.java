package com.yitianys.BlockZ.capability;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockZPlayerItemHandlerProvider implements ICapabilityProvider {
    private final BlockZPlayerItemHandler handler;
    private final LazyOptional<IItemHandler> optional;

    public BlockZPlayerItemHandlerProvider(Player player) {
        this.handler = new BlockZPlayerItemHandler(player);
        this.optional = LazyOptional.of(() -> handler);
    }

    public BlockZPlayerItemHandler getHandler() {
        return handler;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && side == null) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }
}

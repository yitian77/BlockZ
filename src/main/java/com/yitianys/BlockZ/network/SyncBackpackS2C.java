package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncBackpackS2C {
    private final ItemStack backpack;

    public SyncBackpackS2C(ItemStack backpack) {
        this.backpack = backpack;
    }

    public static void encode(SyncBackpackS2C msg, FriendlyByteBuf buf) {
        buf.writeItem(msg.backpack);
    }

    public static SyncBackpackS2C decode(FriendlyByteBuf buf) {
        return new SyncBackpackS2C(buf.readItem());
    }

    public static void handle(SyncBackpackS2C msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
                    cap.getInventory().setStackInSlot(0, msg.backpack);
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

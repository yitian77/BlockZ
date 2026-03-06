package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import com.yitianys.BlockZ.util.ItemSizeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RotateItemC2S {
    public static void encode(RotateItemC2S msg, FriendlyByteBuf buf) {}

    public static RotateItemC2S decode(FriendlyByteBuf buf) {
        return new RotateItemC2S();
    }

    public static void handle(RotateItemC2S msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (player.containerMenu instanceof DayZInventoryMenu menu) {
                ItemStack carried = menu.getCarried();
                if (carried.isEmpty()) return;
                if (ItemSizeManager.toggleRotation(carried)) {
                    menu.setCarried(carried);
                    menu.broadcastChanges();
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}

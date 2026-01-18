package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class OpenDayZMenuC2S {
    public static void encode(OpenDayZMenuC2S msg, FriendlyByteBuf buf) {}
    public static OpenDayZMenuC2S decode(FriendlyByteBuf buf) { return new OpenDayZMenuC2S(); }
    public static void handle(OpenDayZMenuC2S msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            MenuProvider provider = new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("screen.blockz.dayz");
                }
                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, Player p) {
                    return new DayZInventoryMenu(id, inv);
                }
            };
            NetworkHooks.openScreen(player, provider);
        });
        ctx.setPacketHandled(true);
    }
}

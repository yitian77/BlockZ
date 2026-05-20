package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.compat.CuriosIntegration;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
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
            if (!BlockZConfigs.isDayzInventoryEnabled()) return;

            NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inv, p) -> new DayZInventoryMenu(id, inv),
                Component.translatable("screen.blockz.dayz")
            ), buf -> {
                buf.writeInt(com.yitianys.BlockZ.config.BlockZConfigs.getInitialPocketSlots());
                buf.writeBoolean(false); // 不是通过容器打开的
                buf.writeByte(0);
                CuriosIntegration.writeAdditionalDayZSlotRefs(player, buf);
            });
        });
        ctx.setPacketHandled(true);
    }
}

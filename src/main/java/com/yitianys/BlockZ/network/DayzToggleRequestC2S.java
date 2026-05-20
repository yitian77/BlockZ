package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.config.BlockZConfigs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class DayzToggleRequestC2S {
    private final boolean enabled;

    public DayzToggleRequestC2S(boolean enabled) {
        this.enabled = enabled;
    }

    public static void encode(DayzToggleRequestC2S msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enabled);
    }

    public static DayzToggleRequestC2S decode(FriendlyByteBuf buf) {
        return new DayzToggleRequestC2S(buf.readBoolean());
    }

    public static void handle(DayzToggleRequestC2S msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            boolean allowed = BlockZConfigs.getAllowPlayerToggleDayz() || player.hasPermissions(2);
            if (!allowed) {
                player.sendSystemMessage(Component.translatable("msg.blockz.dayz_toggle_denied"));
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DayzToggleStateS2C(true));
                return;
            }
            
            player.getCapability(com.yitianys.BlockZ.capability.PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
                cap.setDayzEnabled(msg.enabled);
            });

            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DayzToggleStateS2C(msg.enabled));
        });
        ctx.setPacketHandled(true);
    }
}

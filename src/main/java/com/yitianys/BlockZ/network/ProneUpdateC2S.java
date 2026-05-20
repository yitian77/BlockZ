package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.util.ProneManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ProneUpdateC2S {
    private final boolean prone;

    public ProneUpdateC2S(boolean prone) {
        this.prone = prone;
    }

    public boolean isProne() {
        return prone;
    }

    public static void encode(ProneUpdateC2S msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.prone);
    }

    public static ProneUpdateC2S decode(FriendlyByteBuf buf) {
        return new ProneUpdateC2S(buf.readBoolean());
    }

    public static void handle(ProneUpdateC2S msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            boolean before = ProneManager.isServerProne(player);
            ProneManager.applyRequestedServerState(player, msg.isProne());
            ProneManager.tickPlayer(player);
            boolean after = ProneManager.isServerProne(player);
            if (before != after || after != msg.isProne()) {
                ProneManager.broadcastState(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}

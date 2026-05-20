package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.util.LeanManager;
import com.yitianys.BlockZ.util.LeanManager.LeanState;
import com.yitianys.BlockZ.util.ProneManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class LeanUpdateC2S {
    private final int leanOrdinal;

    public LeanUpdateC2S(LeanState state) {
        this.leanOrdinal = state.ordinal();
    }

    public LeanState getLeanState() {
        return LeanState.values()[leanOrdinal];
    }

    public static void encode(LeanUpdateC2S msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.leanOrdinal);
    }

    public static LeanUpdateC2S decode(FriendlyByteBuf buf) {
        return new LeanUpdateC2S(LeanState.values()[buf.readVarInt()]);
    }

    public static void handle(LeanUpdateC2S msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            LeanState resolvedState = ProneManager.isServerProne(player) ? LeanState.NONE : msg.getLeanState();
            LeanManager.setLeanState(player, resolvedState);
            LeanSyncS2C sync = new LeanSyncS2C(player.getUUID(), resolvedState);
            for (ServerPlayer tracker : player.serverLevel().players()) {
                if (tracker != player && tracker.distanceToSqr(player) < 4096.0) {
                    NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> tracker), sync);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}

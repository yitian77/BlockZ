package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.util.LeanManager;
import com.yitianys.BlockZ.util.LeanManager.LeanState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class LeanSyncS2C {
    private final UUID playerUuid;
    private final int leanOrdinal;

    public LeanSyncS2C(UUID playerUuid, LeanState state) {
        this.playerUuid = playerUuid;
        this.leanOrdinal = state.ordinal();
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public LeanState getLeanState() {
        return LeanState.values()[leanOrdinal];
    }

    public static void encode(LeanSyncS2C msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerUuid);
        buf.writeVarInt(msg.leanOrdinal);
    }

    public static LeanSyncS2C decode(FriendlyByteBuf buf) {
        return new LeanSyncS2C(buf.readUUID(), LeanState.values()[buf.readVarInt()]);
    }

    public static void handle(LeanSyncS2C msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> LeanManager.setClientLeanState(msg.playerUuid, msg.getLeanState()));
        });
        ctxSupplier.get().setPacketHandled(true);
    }
}

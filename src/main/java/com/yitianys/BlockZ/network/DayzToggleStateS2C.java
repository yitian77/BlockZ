package com.yitianys.BlockZ.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DayzToggleStateS2C {
    private final boolean enabled;

    public DayzToggleStateS2C(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static void encode(DayzToggleStateS2C msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enabled);
    }

    public static DayzToggleStateS2C decode(FriendlyByteBuf buf) {
        return new DayzToggleStateS2C(buf.readBoolean());
    }

    public static void handle(DayzToggleStateS2C msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> com.yitianys.BlockZ.client.network.ClientPacketHandler.handleDayzToggleState(msg, ctxSupplier));
        });
        ctxSupplier.get().setPacketHandled(true);
    }
}

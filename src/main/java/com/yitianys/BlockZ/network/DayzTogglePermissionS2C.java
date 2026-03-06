package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.client.ClientSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DayzTogglePermissionS2C {
    private final boolean allowed;

    public DayzTogglePermissionS2C(boolean allowed) {
        this.allowed = allowed;
    }

    public static void encode(DayzTogglePermissionS2C msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.allowed);
    }

    public static DayzTogglePermissionS2C decode(FriendlyByteBuf buf) {
        return new DayzTogglePermissionS2C(buf.readBoolean());
    }

    public static void handle(DayzTogglePermissionS2C msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().enqueueWork(() -> {
            ClientSettings.dayzToggleAllowed = msg.allowed;
            if (!msg.allowed) {
                ClientSettings.dayzEnabled = true;
            }
        });
        ctxSupplier.get().setPacketHandled(true);
    }
}

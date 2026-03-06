package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.util.ItemSizeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketReloadConfigS2C {
    public PacketReloadConfigS2C() {
    }

    public PacketReloadConfigS2C(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // 在客户端线程执行
            ItemSizeManager.loadCustomSizes();
        });
        return true;
    }
}

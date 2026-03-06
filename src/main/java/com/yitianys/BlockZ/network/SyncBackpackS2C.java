package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncBackpackS2C {
    private final int slotId;
    private final ItemStack stack;

    public SyncBackpackS2C(int slotId, ItemStack stack) {
        this.slotId = slotId;
        this.stack = stack;
    }
    
    public SyncBackpackS2C(ItemStack backpack) {
        this(0, backpack);
    }

    public int getSlotId() {
        return slotId;
    }

    public ItemStack getStack() {
        return stack;
    }

    public static void encode(SyncBackpackS2C msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.slotId);
        buf.writeItem(msg.stack);
    }

    public static SyncBackpackS2C decode(FriendlyByteBuf buf) {
        return new SyncBackpackS2C(buf.readInt(), buf.readItem());
    }

    public static void handle(SyncBackpackS2C msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> com.yitianys.BlockZ.client.network.ClientPacketHandler.handleSyncBackpack(msg, ctx));
        });
        ctx.get().setPacketHandled(true);
    }
}

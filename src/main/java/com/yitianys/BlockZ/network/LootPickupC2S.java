package com.yitianys.BlockZ.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class LootPickupC2S {
    public final int entityId;

    public LootPickupC2S(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(LootPickupC2S msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
    }

    public static LootPickupC2S decode(FriendlyByteBuf buf) {
        return new LootPickupC2S(buf.readVarInt());
    }

    public static void handle(LootPickupC2S msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            Player player = ctx.getSender();
            if (player == null) return;
            Entity e = player.level().getEntity(msg.entityId);
            if (e instanceof ItemEntity item) {
                item.playerTouch(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}

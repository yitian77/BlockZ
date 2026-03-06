package com.yitianys.BlockZ.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import com.yitianys.BlockZ.util.InventoryUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

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
            if (e instanceof ItemEntity item && item.isAlive()) {
                ItemStack stack = item.getItem();
                if (InventoryUtils.addItemToDayZInventory(player.getInventory(), stack)) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                            ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
                    if (stack.isEmpty()) {
                        item.discard();
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}

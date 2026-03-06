package com.yitianys.BlockZ.client.network;

import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import com.yitianys.BlockZ.client.ClientSettings;
import com.yitianys.BlockZ.network.DayzToggleStateS2C;
import com.yitianys.BlockZ.network.SyncBackpackS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

    public static void handleSyncBackpack(SyncBackpackS2C msg, Supplier<NetworkEvent.Context> ctx) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
                cap.getInventory().setStackInSlot(msg.getSlotId(), msg.getStack());
            });
        }
    }

    public static void handleDayzToggleState(DayzToggleStateS2C msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        ClientSettings.dayzEnabled = msg.isEnabled();
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(Component.translatable(
                    msg.isEnabled() ? "msg.blockz.dayz_enabled" : "msg.blockz.dayz_disabled"));
        }
    }
}

package com.yitianys.BlockZ.event;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.capability.PlayerBackpack;
import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.yitianys.BlockZ.network.NetworkHandler;
import com.yitianys.BlockZ.network.SyncBackpackS2C;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = BlockZ.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
                ItemStack stack = cap.getInventory().getStackInSlot(0);
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncBackpackS2C(stack));
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
                ItemStack stack = cap.getInventory().getStackInSlot(0);
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncBackpackS2C(stack));
            });
        }
    }

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).isPresent()) {
                event.addCapability(new ResourceLocation(BlockZ.MODID, "properties"), new PlayerBackpackProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(oldStore -> {
                event.getEntity().getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(newStore -> {
                    newStore.copyFrom(oldStore);
                });
            });
        }
    }

}

@Mod.EventBusSubscriber(modid = BlockZ.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
class ModBusEvents {
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerBackpack.class);
    }
}

package com.yitianys.BlockZ.event;

import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.entity.CorpseEntity;
import com.yitianys.BlockZ.network.DayzTogglePermissionS2C;
import com.yitianys.BlockZ.network.DayzToggleStateS2C;
import com.yitianys.BlockZ.network.NetworkHandler;
import com.yitianys.BlockZ.network.SyncBackpackS2C;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.PacketDistributor;

@EventBusSubscriber(
        modid = "blockz",
        bus = Bus.FORGE
)
public class ModEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
                for (int i = 0; i < 4; i++) {
                    ItemStack stack = cap.getInventory().getStackInSlot(i);
                    NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncBackpackS2C(i, stack));
                }
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DayzToggleStateS2C(cap.isDayzEnabled()));
            });
            boolean allowed = BlockZConfigs.allowPlayerToggleDayz.get() || player.hasPermissions(2);
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DayzTogglePermissionS2C(allowed));
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
                for (int i = 0; i < 4; i++) {
                    ItemStack stack = cap.getInventory().getStackInSlot(i);
                    NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncBackpackS2C(i, stack));
                }
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DayzToggleStateS2C(cap.isDayzEnabled()));
            });
            boolean allowed = BlockZConfigs.allowPlayerToggleDayz.get() || player.hasPermissions(2);
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DayzTogglePermissionS2C(allowed));
        }
    }

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player && !event.getObject().getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).isPresent()) {
            event.addCapability(new ResourceLocation("blockz", "properties"), new PlayerBackpackProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(Clone event) {
        event.getOriginal().getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(oldStore -> {
            event.getEntity().getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(newStore -> {
                newStore.setDayzEnabled(oldStore.isDayzEnabled());

                boolean keepInventory = event.getEntity().level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
                if (keepInventory || !event.isWasDeath()) {
                    for (int i = 0; i < 4; i++) {
                        newStore.getInventory().setStackInSlot(i, oldStore.getInventory().getStackInSlot(i).copy());
                    }
                }
            });
        });
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!event.getEntity().level().isClientSide) {
                if (!BlockZConfigs.enableCorpse.get() || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
                    return;
                }

                CorpseEntity corpse = new CorpseEntity(player.level(), player);
                player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
                    IItemHandler inv = cap.getInventory();
                    corpse.setItem(0, inv.getStackInSlot(0).copy());
                    corpse.setItem(1, inv.getStackInSlot(1).copy());
                    corpse.setItem(7, inv.getStackInSlot(3).copy());
                    corpse.setItem(8, inv.getStackInSlot(2).copy());

                    for (int i = 0; i < 4; i++) {
                        if (inv instanceof ItemStackHandler h) {
                            h.setStackInSlot(i, ItemStack.EMPTY);
                        }
                    }
                });

                corpse.setItem(2, player.getInventory().getArmor(2).copy());
                corpse.setItem(3, player.getInventory().getArmor(1).copy());
                corpse.setItem(4, player.getInventory().getArmor(3).copy());
                corpse.setItem(5, player.getInventory().getArmor(0).copy());
                corpse.setItem(6, player.getOffhandItem().copy());

                for (int i = 0; i < 9; i++) {
                    corpse.setItem(9 + i, player.getInventory().items.get(i).copy());
                }

                int pocketCount = BlockZConfigs.initialPocketSlots.get();
                int maxCorpsePockets = Math.max(0, corpse.getContainerSize() - 18);
                int transferCount = Math.min(pocketCount, maxCorpsePockets);
                for (int i = 0; i < transferCount; i++) {
                    corpse.setItem(18 + i, player.getInventory().items.get(9 + i).copy());
                }

                player.level().addFreshEntity(corpse);
                player.getInventory().clearContent();
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity living = event.getEntity();
        if (living instanceof Player player) {
            if (!event.getEntity().level().isClientSide && !player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
                event.getDrops().clear();
            }
        }
    }
}

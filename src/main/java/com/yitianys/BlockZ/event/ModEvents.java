package com.yitianys.BlockZ.event;

import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import com.yitianys.BlockZ.compat.CuriosIntegration;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.config.DayZZombieConfig;
import com.yitianys.BlockZ.entity.CorpseEntity;
import com.yitianys.BlockZ.entity.DayZZombieEntity;
import com.yitianys.BlockZ.network.DayzTogglePermissionS2C;
import com.yitianys.BlockZ.network.DayzToggleStateS2C;
import com.yitianys.BlockZ.network.NetworkHandler;
import com.yitianys.BlockZ.network.SyncBackpackS2C;
import com.yitianys.BlockZ.network.SyncGridRulesS2C;
import com.yitianys.BlockZ.network.SyncPlayerStatusS2C;
import com.yitianys.BlockZ.init.ModItems;
import com.yitianys.BlockZ.util.DayZPlayerStatusManager;
import com.yitianys.BlockZ.util.ProneManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.PacketDistributor;

import com.yitianys.BlockZ.network.SyncConfigS2C;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(
        modid = "blockz",
        bus = Bus.FORGE
)
public class ModEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CuriosIntegration.importToCapability(player);
            syncPlayerState(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CommonModEvents.clearStatusSyncCache(player);
        }
        if (event.getEntity().level().isClientSide) {
            BlockZConfigs.clearSyncedValues();
            DayZZombieConfig.clearSyncedValues();
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CuriosIntegration.importToCapability(player);
            syncPlayerState(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer trackingPlayer)) {
            return;
        }
        if (event.getTarget() instanceof Player trackedPlayer) {
            ProneManager.syncStateTo(trackingPlayer, trackedPlayer);
        }
    }

    private static void syncPlayerState(ServerPlayer player) {
        DayZPlayerStatusManager.ensureInitialized(player);
        player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
            for (int i = 0; i < 4; i++) {
                ItemStack stack = cap.getInventory().getStackInSlot(i);
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncBackpackS2C(i, stack));
            }
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DayzToggleStateS2C(cap.isDayzEnabled()));
        });

        boolean allowed = BlockZConfigs.getAllowPlayerToggleDayz() || player.hasPermissions(2);
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DayzTogglePermissionS2C(allowed));
        syncServerConfigs(player);
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlayerStatusS2C(
                DayZPlayerStatusManager.getHealthPointsRatio(player),
                DayZPlayerStatusManager.getHealthRatio(player),
                DayZPlayerStatusManager.getStaminaRatio(player),
                DayZPlayerStatusManager.getInfectionRatio(player)
        ));
        ProneManager.syncStateTo(player, player);
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other != player) {
                ProneManager.syncStateTo(player, other);
            }
        }
    }

    public static void syncServerConfigs(ServerPlayer player) {
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), SyncGridRulesS2C.createServerSnapshot());
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncConfigS2C());
    }

    public static void broadcastServerConfigs(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncServerConfigs(player);
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

        if (!event.isWasDeath()) {
            DayZPlayerStatusManager.copyPersistentStatus(event.getOriginal(), event.getEntity());
            ProneManager.copyPersistentState(event.getOriginal(), event.getEntity());
        } else {
            DayZPlayerStatusManager.reset(event.getEntity());
            ProneManager.reset(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!event.getEntity().level().isClientSide) {
                boolean corpseEnabled = BlockZConfigs.getEnableCorpse();
                boolean keepInventory = player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);

                if (!corpseEnabled) {
                    if (!keepInventory) {
                        dropBackpackCapabilityItems(player);
                    }
                    return;
                }

                if (keepInventory) {
                    return;
                }

                CorpseEntity corpse = new CorpseEntity(player.level(), player);
                player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
                    IItemHandler inv = cap.getInventory();
                    corpse.setItem(0, inv.getStackInSlot(0).copy());
                    corpse.setItem(1, inv.getStackInSlot(1).copy());
                    corpse.setItem(7, inv.getStackInSlot(3).copy());
                    corpse.setItem(8, inv.getStackInSlot(2).copy());

                    if (inv instanceof ItemStackHandler h) {
                        for (int i = 0; i < h.getSlots(); i++) {
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

                int pocketCount = player.getInventory().items.size() - 9; // 获取玩家所有非快捷栏格子 (通常是 27 个或更多)
                int maxCorpsePockets = corpse.getContainerSize() - 18;
                int transferCount = Math.min(pocketCount, maxCorpsePockets);
                for (int i = 0; i < transferCount; i++) {
                    corpse.setItem(18 + i, player.getInventory().items.get(9 + i).copy());
                }

                player.level().addFreshEntity(corpse);
                player.getInventory().clearContent();
            }
        }
    }

    private static void dropBackpackCapabilityItems(ServerPlayer player) {
        player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
            ItemStackHandler handler = cap.getInventory();
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    player.spawnAtLocation(stack.copy(), 0.0F);
                    handler.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        });
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity living = event.getEntity();
        if (living instanceof DayZZombieEntity zombie) {
            if (living.level().isClientSide || DayZZombieConfig.getCorpseStayDuration() <= 0) {
                return;
            }

            List<ItemStack> corpseLoot = new ArrayList<>();
            for (ItemEntity itemEntity : event.getDrops()) {
                ItemStack stack = itemEntity.getItem();
                if (!stack.isEmpty()) {
                    corpseLoot.add(stack.copy());
                }
            }

            zombie.absorbCorpseLoot(corpseLoot);
            event.getDrops().clear();
            return;
        }

        if (living instanceof Player player) {
            boolean keepInventory = player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
            if (!player.level().isClientSide && !keepInventory) {
                event.getDrops().removeIf(itemEntity -> itemEntity.getItem().is(ModItems.LOCK_ITEM.get()));
                if (BlockZConfigs.getEnableCorpse()) {
                    event.getDrops().clear();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!(event.getEntity() instanceof DayZZombieEntity)) {
            return;
        }
        if (DayZZombieConfig.isNaturalSpawnEnabled()) {
            return;
        }

        MobSpawnType spawnType = event.getSpawnType();
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            event.setSpawnCancelled(true);
        }
    }
}

package com.yitianys.BlockZ.event;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.capability.PlayerBackpack;
import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import com.yitianys.BlockZ.config.BlockZConfigs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.yitianys.BlockZ.network.NetworkHandler;
import com.yitianys.BlockZ.network.DayzTogglePermissionS2C;
import com.yitianys.BlockZ.network.DayzToggleStateS2C;
import com.yitianys.BlockZ.network.SyncBackpackS2C;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.Container;
import net.minecraftforge.network.NetworkHooks;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;

@Mod.EventBusSubscriber(modid = BlockZ.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof ServerPlayer player) {
            BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
            if (be instanceof Container) {
                // 如果点击的是容器，取消默认行为并打开我们的自定义界面
                event.setCanceled(true);
                NetworkHooks.openScreen(player, new SimpleMenuProvider(
                    (id, inv, p) -> new DayZInventoryMenu(id, inv, event.getPos()),
                    Component.translatable("container.dayz_inventory")
                ), buf -> {
                    buf.writeBoolean(true);
                    buf.writeBlockPos(event.getPos());
                });
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
                // 同步所有装备槽位
                for (int i = 0; i < PlayerBackpack.SLOT_COUNT; i++) {
                    ItemStack stack = cap.getInventory().getStackInSlot(i);
                    NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncBackpackS2C(i, stack));
                }
                // 同步 DayZ UI 状态
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DayzToggleStateS2C(cap.isDayzEnabled()));
            });
            boolean allowed = BlockZConfigs.allowPlayerToggleDayz.get() || player.hasPermissions(2);
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DayzTogglePermissionS2C(allowed));
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
                // 同步所有装备槽位
                for (int i = 0; i < PlayerBackpack.SLOT_COUNT; i++) {
                    ItemStack stack = cap.getInventory().getStackInSlot(i);
                    NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncBackpackS2C(i, stack));
                }
                // 同步 DayZ UI 状态
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DayzToggleStateS2C(cap.isDayzEnabled()));
            });
            boolean allowed = BlockZConfigs.allowPlayerToggleDayz.get() || player.hasPermissions(2);
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DayzTogglePermissionS2C(allowed));
        }
    }

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).isPresent()) {
                event.addCapability(new ResourceLocation(BlockZ.MODID + ":properties"), new PlayerBackpackProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        event.getOriginal().getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(oldStore -> {
            event.getEntity().getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(newStore -> {
                // 1. 始终同步 DayZ 设置
                newStore.setDayzEnabled(oldStore.isDayzEnabled());

                // 2. 只有在保留物品栏规则开启，或者不是因死亡导致的克隆（如从末地返回）时，才复制物品
                boolean keepInventory = event.getEntity().level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY);
                if (keepInventory || !event.isWasDeath()) {
                    for (int i = 0; i < PlayerBackpack.SLOT_COUNT; i++) {
                        newStore.getInventory().setStackInSlot(i, oldStore.getInventory().getStackInSlot(i).copy());
                    }
                }
            });
        });
    }

    @SubscribeEvent
    public static void onLivingDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (event.getEntity() instanceof Player player && !event.getEntity().level().isClientSide) {
            // 如果开启了保留物品栏，则不掉落自定义装备
            if (player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) {
                return;
            }

            player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
                net.minecraftforge.items.IItemHandler inv = cap.getInventory();
                for (int i = 0; i < inv.getSlots(); i++) {
                    ItemStack stack = inv.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                            player.level(), 
                            player.getX(), 
                            player.getY() + 0.5, 
                            player.getZ(), 
                            stack.copy()
                        );
                        itemEntity.setDefaultPickUpDelay();
                        event.getDrops().add(itemEntity);
                        // 注意：这里不需要清空 inv，因为该 player 实体即将被丢弃
                    }
                }
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

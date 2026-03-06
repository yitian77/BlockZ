package com.yitianys.BlockZ.event;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

import com.yitianys.BlockZ.capability.PlayerBackpack;
import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.init.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.entity.Entity;

import com.yitianys.BlockZ.util.InventoryUtils;
import com.yitianys.BlockZ.util.ItemHandlerContainer;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

@Mod.EventBusSubscriber(modid = BlockZ.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonModEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        Inventory inv = player.getInventory();
        
        // 全局清理：热栏(0-8)、副手(40)、护甲栏(36-39)等不应该出现锁定物品
        // 锁定物品只应该出现在原版背包的主存储区(9-35)的被锁定部分
        // 同时清理鼠标持有的锁定物品
        if (player.containerMenu != null && player.containerMenu.getCarried().is(ModItems.LOCK_ITEM.get())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
        
        // 检查热栏 (0-8)
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).is(ModItems.LOCK_ITEM.get())) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        
        boolean dayzEnabled = player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                .map(PlayerBackpack::isDayzEnabled)
                .orElse(true);
        boolean isAdmin = player.hasPermissions(2);
        boolean lockEnabled = BlockZConfigs.enableVanillaBackpackLock.get();

        int allowedSlots = 0;

        // 计算允许的槽位数
        if (!dayzEnabled && !isAdmin && lockEnabled) {
            // 基础口袋槽位 (5格, 对应原版 9-13)
            allowedSlots = BlockZConfigs.initialPocketSlots.get();

            // 获取装备提供的槽位 (在 Vanilla UI 模式下禁用)
            // allowedSlots += player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).map(cap -> {
            //     IItemHandler handler = cap.getInventory();
            //     int slots = 0;
            //     slots += BlockZConfigs.getBackpackSlots(handler.getStackInSlot(PlayerBackpack.SLOT_BACKPACK));
            //     slots += BlockZConfigs.getBackpackSlots(handler.getStackInSlot(PlayerBackpack.SLOT_VEST));
            //     return slots;
            // }).orElse(0);
            
            // allowedSlots += BlockZConfigs.getBackpackSlots(inv.getArmor(2)); // Shirt (Chestplate)
            // allowedSlots += BlockZConfigs.getBackpackSlots(inv.getArmor(1)); // Pants (Leggings)
        }

        int unlockedEndIndex = 9 + allowedSlots;
        // 限制最大范围 (防止超出原版背包 9-35)
        if (unlockedEndIndex > 36) unlockedEndIndex = 36;

        // 如果 DayZ 禁用且不是管理员，锁定超出容量的槽位
        if (!dayzEnabled && !isAdmin && lockEnabled) {
            for (int i = 9; i < 36; i++) {
                ItemStack stack = inv.getItem(i);
                if (i < unlockedEndIndex) {
                    // 应该解锁的区域：如果是锁定物品，清除
                    if (stack.is(ModItems.LOCK_ITEM.get())) {
                        inv.setItem(i, ItemStack.EMPTY);
                    }
                } else {
                    // 应该锁定的区域
                    if (stack.isEmpty()) {
                        // 如果是空，填充锁定物品
                        inv.setItem(i, new ItemStack(ModItems.LOCK_ITEM.get()));
                    } else if (!stack.is(ModItems.LOCK_ITEM.get())) {
                        // 如果有非锁定物品在这些被锁定的槽位，说明玩家强制放入了
                        // 尝试将其弹出 (drop)
                        player.drop(stack.copy(), true);
                        inv.setItem(i, new ItemStack(ModItems.LOCK_ITEM.get()));
                    }
                }
            }
        } else {
            // 如果 DayZ 启用或管理员，清除所有锁定物品
            for (int i = 9; i < 36; i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.is(ModItems.LOCK_ITEM.get())) {
                    inv.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.isSpectator()) return;

        boolean dayzEnabled = player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                .map(PlayerBackpack::isDayzEnabled)
                .orElse(true);

        // 如果 DayZ UI 被禁用，允许原版交互
        if (!dayzEnabled) return;

        if (clientTryPickup(event, player)) {
            return;
        }

        if (serverTryPickup(event, player)) {
            return;
        }

        BlockPos pos = event.getPos();
        Level level = event.getLevel();
        BlockEntity be = level.getBlockEntity(pos);
        
        // 特殊处理 Lootr
        IItemHandler lootrHandler = InventoryUtils.getLootrInventory(level, pos, player);
        boolean isLootr = lootrHandler != null;
        
        // 这里只针对我们“必须强制换成 DayZ 布局”的方块做拦截：
        // - Lootr：需要通过 Lootr API 获取虚拟容器
        // - 工作台 / 附魔台：需要自定义 DayZ 工作台/附魔布局
        // 普通箱子、一般容器交给原版或其它模组先打开，再由 openMenu Mixin / 客户端拦截转换为 DayZ UI，
        // 以避免抢占右键事件导致虚拟容器模组逻辑失效。
        boolean isLootrContainer = isLootr;
        boolean isCraftingTable = level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.CraftingTableBlock;
        boolean isEnchantingTable = level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.EnchantmentTableBlock;
        boolean isChest = level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.ChestBlock;

        if (isLootrContainer || isCraftingTable || isEnchantingTable || isChest) {
            event.setCanceled(true);
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);

            if (!level.isClientSide && event.getEntity() instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        if (isCraftingTable) return Component.translatable("container.crafting");
                        if (isEnchantingTable) return Component.translatable("container.enchant");
                        if (be instanceof net.minecraft.world.Nameable nameable) {
                            return nameable.getDisplayName();
                        }
                        return Component.translatable("container.inventory");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                        return new DayZInventoryMenu(id, inventory, pos);
                    }
                }, buf -> {
                    buf.writeInt(com.yitianys.BlockZ.config.BlockZConfigs.initialPocketSlots.get());
                    buf.writeBoolean(true); // Has Pos
                    buf.writeBlockPos(pos);
                });
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.isSpectator()) return;

        boolean dayzEnabled = player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                .map(PlayerBackpack::isDayzEnabled)
                .orElse(true);

        if (!dayzEnabled) return;
        if (clientTryPickup(event, player)) {
            return;
        }
        serverTryPickup(event, player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        Player player = event.getEntity();
        if (player.isSpectator()) return;

        boolean dayzEnabled = player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                .map(PlayerBackpack::isDayzEnabled)
                .orElse(true);

        if (!dayzEnabled) return;
        if (clientTryPickup(event, player)) {
            return;
        }
        serverTryPickup(event, player);
    }

    private static boolean clientTryPickup(PlayerInteractEvent event, Player player) {
        Level level = event.getLevel();
        if (!level.isClientSide) return false;

        Entity targeted = InventoryUtils.getTargetedItemEntity(player, 4.0);
        if (targeted instanceof ItemEntity item && item.isAlive()) {
            ItemStack stack = item.getItem();
            if (stack.isEmpty()) return false;

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (event instanceof PlayerInteractEvent.RightClickBlock blockEvent) {
                blockEvent.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
                blockEvent.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
            }
            return true;
        }
        return false;
    }

    private static boolean serverTryPickup(PlayerInteractEvent event, Player player) {
        Level level = event.getLevel();
        if (level.isClientSide) return false;

        Entity targeted = InventoryUtils.getTargetedItemEntity(player, 4.0);
        if (targeted instanceof ItemEntity item && item.isAlive()) {
            ItemStack stack = item.getItem();
            if (stack.isEmpty()) return false;

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (event instanceof PlayerInteractEvent.RightClickBlock blockEvent) {
                blockEvent.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
                blockEvent.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
            } else if (event instanceof PlayerInteractEvent.RightClickItem itemEvent) {
                itemEvent.setCancellationResult(InteractionResult.SUCCESS);
            }

            boolean added = InventoryUtils.addItemToDayZInventory(player.getInventory(), stack);
            if (added) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                        ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);

                if (stack.isEmpty()) {
                    item.discard();
                } else {
                    item.setItem(stack);
                }
            }
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) return;
        
        Player player = event.getEntity();
        boolean dayzEnabled = player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                .map(PlayerBackpack::isDayzEnabled)
                .orElse(true);

        if (!dayzEnabled) return;

        Entity target = event.getTarget();
        if (target instanceof ContainerEntity && target instanceof net.minecraft.world.Container c) {
            event.setCanceled(true);

            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return target.getDisplayName();
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                        DayZInventoryMenu menu = new DayZInventoryMenu(id, inventory, (BlockPos) null);
                        menu.activeContainer = c;
                        return menu;
                    }
                }, buf -> {
                    buf.writeInt(com.yitianys.BlockZ.config.BlockZConfigs.initialPocketSlots.get());
                    buf.writeBoolean(false); // No BlockPos
                    buf.writeByte(1); // Type 1: Entity
                    buf.writeInt(target.getId()); // Entity ID
                });
            }
        }
    }

    @SubscribeEvent
    public static void onItemPickup(net.minecraftforge.event.entity.player.EntityItemPickupEvent event) {
        if (event.getEntity().level().isClientSide) return;

        Player player = event.getEntity();
        boolean dayzEnabled = player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                .map(PlayerBackpack::isDayzEnabled)
                .orElse(true);

        // DayZ 模式下，禁用自然拾取 (走过物品时不拾取)
        if (dayzEnabled) {
            event.setCanceled(true);
        }
    }
}


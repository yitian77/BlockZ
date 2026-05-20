package com.yitianys.BlockZ.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import com.yitianys.BlockZ.BlockZ;
import java.lang.reflect.Method;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import java.util.List;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.lang.reflect.Proxy;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

public class InventoryUtils {

    private static Method lootrGetInventoryMethod = null;
    private static Class<?> lootFillerClass = null;
    private static boolean lootrChecked = false;
    private static Method getTileIdMethod = null;
    private static Method getOpenersMethod = null;
    private static Method updatePacketMethod = null;

    public static UUID getLootrTileId(BlockEntity be) {
        if (be == null) return null;
        try {
            if (getTileIdMethod == null) {
                getTileIdMethod = be.getClass().getMethod("getTileId");
            }
            return (UUID) getTileIdMethod.invoke(be);
        } catch (Exception e) {
            // 生产环境兼容性：如果反射失败，尝试查找 tileId 字段（针对混淆环境）
            try {
                java.lang.reflect.Field field = be.getClass().getDeclaredField("tileId");
                field.setAccessible(true);
                return (UUID) field.get(be);
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static void startOpenLootr(BlockEntity be, Player player) {
        if (be == null || !(player instanceof ServerPlayer sp)) return;
        boolean opened = false;
        try {
            Method startOpenMethod = ObfuscationReflectionHelper.findMethod(be.getClass(), "m_5856_", Player.class);
            startOpenMethod.invoke(be, player);
            opened = true;
        } catch (Exception e) {
            try {
                Method startOpenMethod = be.getClass().getMethod("startOpen", Player.class);
                startOpenMethod.invoke(be, player);
                opened = true;
            } catch (Exception ignored) {}
        }
        try {
            if (getOpenersMethod == null) {
                getOpenersMethod = be.getClass().getMethod("getOpeners");
            }
            @SuppressWarnings("unchecked")
            java.util.Set<UUID> openers = (java.util.Set<UUID>) getOpenersMethod.invoke(be);
            if (openers.add(sp.getUUID())) {
                be.setChanged();
            }
        } catch (Exception ignored) {}
        if (be.getLevel() != null && !be.getLevel().isClientSide) {
            Level level = be.getLevel();
            BlockPos pos = be.getBlockPos();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
        }
    }

    public static void stopOpenLootr(BlockEntity be, Player player) {
        if (be == null || !(player instanceof ServerPlayer)) return;
        if (be instanceof BaseContainerBlockEntity container) {
            container.stopOpen(player);
            container.setChanged();
        }
        if (be.getLevel() != null && !be.getLevel().isClientSide) {
            Level level = be.getLevel();
            BlockPos pos = be.getBlockPos();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
        }
    }

    public static IItemHandler getLootrInventory(Level level, BlockPos pos, Player player) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) return null;

        if (!lootrChecked) {
            if (ModList.get().isLoaded("lootr")) {
                try {
                    Class<?> apiClass = Class.forName("noobanidus.mods.lootr.api.LootrAPI");
                    lootFillerClass = Class.forName("noobanidus.mods.lootr.api.LootFiller");
                    
                    lootrGetInventoryMethod = apiClass.getMethod("getInventory", 
                        Level.class, UUID.class, BlockPos.class, ServerPlayer.class, 
                        RandomizableContainerBlockEntity.class, lootFillerClass, Supplier.class, LongSupplier.class);
                } catch (Exception e) {
                    BlockZ.LOGGER.error("Failed to find LootrAPI.getInventory with full signature", e);
                }
            }
            lootrChecked = true;
        }

        if (lootrGetInventoryMethod != null) {
            try {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof BaseContainerBlockEntity containerBE) {
                    final BlockEntity be = blockEntity; // 确保 effectively final 用于 lambda
                    UUID tileId = getLootrTileId(be);
                    if (tileId == null) return null;

                    if (player instanceof ServerPlayer sp) {
                        try {
                            if (getOpenersMethod == null) {
                                getOpenersMethod = be.getClass().getMethod("getOpeners");
                            }
                            @SuppressWarnings("unchecked")
                            java.util.Set<UUID> openers = (java.util.Set<UUID>) getOpenersMethod.invoke(be);
                            if (openers.add(sp.getUUID())) {
                                be.setChanged();
                            }
                            // 触发 Piglin 愤怒
                            net.minecraft.world.entity.monster.piglin.PiglinAi.angerNearbyPiglins(player, true);
                        } catch (Exception e) {
                            BlockZ.LOGGER.debug("Failed to trigger Lootr side effects", e);
                        }
                    }

                    // 1. 创建 LootFiller 代理
                    Object fillerProxy = Proxy.newProxyInstance(
                        lootFillerClass.getClassLoader(),
                        new Class<?>[]{lootFillerClass},
                        (proxy, method, args) -> {
                            if (method.getName().equals("unpackLootTable")) {
                                Player p = (Player) args[0];
                                Container inv = (Container) args[1];
                                ResourceLocation table = (ResourceLocation) args[2];
                                long seed = (long) args[3];
                                
                                try {
                                    try {
                                        Method unpackMethod = ObfuscationReflectionHelper.findMethod(be.getClass(), "m_59652_", Player.class, Container.class, ResourceLocation.class, long.class);
                                        unpackMethod.invoke(be, p, inv, table, seed);
                                    } catch (Exception e) {
                                        try {
                                            Method unpackMethod = be.getClass().getMethod("unpackLootTable", Player.class, Container.class, ResourceLocation.class, long.class);
                                            unpackMethod.invoke(be, p, inv, table, seed);
                                        } catch (NoSuchMethodException ex) {
                                            if (be instanceof RandomizableContainerBlockEntity rcbe) {
                                                rcbe.setLootTable(table, seed);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    BlockZ.LOGGER.error("Failed to invoke unpackLootTable on Lootr block entity", e);
                                }
                            }
                            return null;
                        }
                    );

                    // 2. 准备供应商
                    Supplier<ResourceLocation> tableSupplier = () -> {
                        try {
                            try {
                                Method getTableMethod = be.getClass().getMethod("getTable");
                                return (ResourceLocation) getTableMethod.invoke(be);
                            } catch (NoSuchMethodException e) {
                                if (be instanceof RandomizableContainerBlockEntity rcbe) {
                                    return ObfuscationReflectionHelper.getPrivateValue(RandomizableContainerBlockEntity.class, rcbe, "f_59605_");
                                }
                                return null;
                            }
                        } catch (Exception e) {
                            return null;
                        }
                    };

                    LongSupplier seedSupplier = () -> {
                        try {
                            try {
                                Method getSeedMethod = be.getClass().getMethod("getSeed");
                                return (long) getSeedMethod.invoke(be);
                            } catch (NoSuchMethodException e) {
                                if (be instanceof RandomizableContainerBlockEntity rcbe) {
                                    return ObfuscationReflectionHelper.getPrivateValue(RandomizableContainerBlockEntity.class, rcbe, "f_59606_");
                                }
                                return -1L;
                            }
                        } catch (Exception e) {
                            return -1L;
                        }
                    };

                    // 3. 调用 LootrAPI
                    Object result = lootrGetInventoryMethod.invoke(null, 
                        level, tileId, pos, serverPlayer, containerBE, fillerProxy, tableSupplier, seedSupplier);
                    
                    if (result != null) {
                        try {
                            Method startOpenMethod = ObfuscationReflectionHelper.findMethod(result.getClass(), "m_5856_", Player.class);
                            startOpenMethod.invoke(result, serverPlayer);
                        } catch (Exception e) {
                            try {
                                Method startOpenMethod = result.getClass().getMethod("startOpen", Player.class);
                                startOpenMethod.invoke(result, serverPlayer);
                            } catch (Exception ignored) {}
                        }
                        
                        if (result instanceof Container container && container.isEmpty()) {
                             try {
                                 Method unpackMethod = result.getClass().getMethod("unpackLootTable", Player.class);
                                 unpackMethod.invoke(result, serverPlayer);
                             } catch (Exception ignored) {}
                        }
                    }

                    if (result instanceof IItemHandler) {
                        return (IItemHandler) result;
                    } else if (result instanceof Container container) {
                        return new net.minecraftforge.items.wrapper.InvWrapper(container);
                    }
                }
            } catch (Exception e) {
                BlockZ.LOGGER.error("Failed to invoke LootrAPI.getInventory", e);
            }
        }
        return null;
    }

    public static Entity getTargetedItemEntity(Player player, double reach) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 viewVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(viewVec.scale(reach));
        
        // 1. 精确射线检测 (Precise Raytrace)
        // 扩大搜索范围以覆盖整个触达距离
        AABB searchBox = player.getBoundingBox().expandTowards(viewVec.scale(reach)).inflate(1.0D);
        
        EntityHitResult result = ProjectileUtil.getEntityHitResult(
                player,
                eyePos,
                endPos,
                searchBox,
                e -> e instanceof ItemEntity && e.isAlive(),
                reach * reach
        );
        
        if (result != null) {
            return result.getEntity();
        }

        // 2. 宽容射线检测 (Thick Raytrace / Cone Trace)
        // 如果精确检测失败 (通常因为 ItemEntity 碰撞箱太小)，尝试检测视线周围的物品
        // 这解决了 "准星对准物品却无法拾取" 的问题
        List<ItemEntity> nearby = player.level().getEntitiesOfClass(ItemEntity.class, searchBox);
        
        ItemEntity closest = null;
        double closestDistSq = Double.MAX_VALUE;
        double tolerance = 0.5; // 允许 0.5 格的误差范围

        for (ItemEntity item : nearby) {
            // 将物品碰撞箱临时扩大，检测视线是否穿过这个扩大的区域
            AABB expandedBox = item.getBoundingBox().inflate(tolerance);
            Optional<Vec3> clip = expandedBox.clip(eyePos, endPos);
            
            if (clip.isPresent()) {
                double d = eyePos.distanceToSqr(clip.get());
                if (d < closestDistSq && d < reach * reach) {
                    closestDistSq = d;
                    closest = item;
                }
            }
        }
        
        return closest;
    }

    /**
     * DayZ 模式下专用的物品添加逻辑
     * 仅允许添加到快捷栏 (0-8) 和口袋 (9-13)
     * @param inv 玩家背包
     * @param stack 要添加的物品
     * @return 是否成功添加（或部分添加）
     */
    public static boolean addItemToDayZInventory(Inventory inv, ItemStack stack) {
        boolean changed = false;
        int pocketCount = com.yitianys.BlockZ.config.BlockZConfigs.getInitialPocketSlots();
        int maxSlot = 9 + pocketCount; // 0-8 (Hotbar) + 9-(9+count-1) (Pockets)

        // 1. 尝试堆叠到现有物品
        for (int i = 0; i < maxSlot; i++) {
            ItemStack inSlot = inv.getItem(i);
            if (!inSlot.isEmpty() && ItemStack.isSameItemSameTags(inSlot, stack)) {
                int space = inSlot.getMaxStackSize() - inSlot.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, stack.getCount());
                    inSlot.grow(toAdd);
                    stack.shrink(toAdd);
                    changed = true;
                    if (stack.isEmpty()) return true;
                }
            }
        }

        // 2. 尝试放入空槽位
        for (int i = 0; i < maxSlot; i++) {
            if (inv.getItem(i).isEmpty()) {
                inv.setItem(i, stack.copy());
                stack.setCount(0);
                return true;
            }
        }

        return changed;
    }
}

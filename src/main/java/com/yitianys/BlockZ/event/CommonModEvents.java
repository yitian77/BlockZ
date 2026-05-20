package com.yitianys.BlockZ.event;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import com.yitianys.BlockZ.compat.CuriosIntegration;
import com.yitianys.BlockZ.client.ClientSettings;
import com.yitianys.BlockZ.client.gui.DayZInventoryScreen;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import com.yitianys.BlockZ.init.ModEffects;
import com.yitianys.BlockZ.network.NetworkHandler;
import com.yitianys.BlockZ.network.SyncPlayerStatusS2C;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.ForgeMod;

import net.minecraft.world.entity.Entity;

import com.yitianys.BlockZ.util.DayZPlayerStatusManager;
import com.yitianys.BlockZ.util.InventoryUtils;
import com.yitianys.BlockZ.util.ProneManager;
import net.minecraftforge.items.IItemHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import com.yitianys.BlockZ.util.PlayerMessageUtils;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;

import com.yitianys.BlockZ.capability.PlayerBackpack;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.init.ModItems;

@Mod.EventBusSubscriber(modid = BlockZ.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonModEvents {

    private static final float ZOMBIE_BLEEDING_BASE_CHANCE = 0.05F;
    private static final int INFINITE_DURATION = 9999999;
    private static final int FRACTURE_RECOVERY_TICKS = 20 * 60 * 5;

    private static final UUID ANALGESIC_FRACTURE_SPEED_UUID = UUID.fromString("6f7a038d-8c0c-4d92-9c26-9c1b032b3f7c");
    private static final AttributeModifier ANALGESIC_FRACTURE_SPEED_MODIFIER = new AttributeModifier(
            ANALGESIC_FRACTURE_SPEED_UUID,
            "blockz_analgesic_fracture_speed",
            1.2222222222D,
            AttributeModifier.Operation.MULTIPLY_TOTAL
    );

    private static final UUID SPLINT_FRACTURE_RECOVERY_SPEED_UUID = UUID.fromString("04da1c28-4fe7-4c6d-b0c6-69cd310df94e");
    private static final AttributeModifier SPLINT_FRACTURE_RECOVERY_SPEED_MODIFIER = new AttributeModifier(
            SPLINT_FRACTURE_RECOVERY_SPEED_UUID,
            "blockz_splint_fracture_recovery_speed",
            0.4444444444D,
            AttributeModifier.Operation.MULTIPLY_TOTAL
    );
    private static final String CREATIVE_FRACTURE_SPEED_OVERRIDE_TAG = "blockz_creative_fracture_speed_override";
    private static final String CREATIVE_FRACTURE_BASE_WALK_SPEED_TAG = "blockz_creative_fracture_base_walk_speed";
    private static final String CREATIVE_FRACTURE_BASE_FLY_SPEED_TAG = "blockz_creative_fracture_base_fly_speed";
    private static final float FRACTURE_CREATIVE_SPEED_SCALE = 0.45F;
    private static final float FRACTURE_RECOVERY_CREATIVE_SPEED_SCALE = 0.65F;
    private static final UUID LOW_HEALTH_SPEED_UUID = UUID.fromString("f732a2bb-9b9a-4b72-871d-2fd86a55b68b");
    private static final UUID PRONE_SPEED_UUID = UUID.fromString("4b9d5147-a6dc-4fa4-9fd1-88655c6d4086");
    private static final UUID PRONE_STEP_HEIGHT_UUID = UUID.fromString("f8442c31-a7da-455c-8d55-b3d86ab70f0b");
    private static final double PRONE_SPEED_MULTIPLIER = -0.4D;
    private static final double PRONE_STEP_HEIGHT_OFFSET = -0.6D;
    private static final int STATUS_SYNC_INTERVAL_TICKS = 20;
    private static final int STATUS_SYNC_SCALE = 1000;
    private static final Map<UUID, StatusSyncSnapshot> LAST_SYNCED_STATUS = new ConcurrentHashMap<>();

    // 跳跃体力消耗控制相关
    private static final String JUMP_TIME_TAG = "blockz_last_jump_time";
    private static final String JUMP_COUNT_TAG = "blockz_jump_count";
    private static final long JUMP_RESET_TICKS = 20L; // 1秒内没跳重置计数

    @SubscribeEvent
    public static void onPlayerJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            if (ProneManager.isServerProne(player)) {
                net.minecraft.world.phys.Vec3 movement = player.getDeltaMovement();
                player.setDeltaMovement(movement.x, Math.min(0.0D, movement.y), movement.z);
                player.hasImpulse = true;
                return;
            }
            long currentTime = player.level().getGameTime();
            CompoundTag tag = player.getPersistentData();
            
            long lastJumpTime = tag.getLong(JUMP_TIME_TAG);
            int jumpCount = tag.getInt(JUMP_COUNT_TAG);
            
            if (currentTime - lastJumpTime > JUMP_RESET_TICKS) {
                jumpCount = 0; // 重置计数
            }
            jumpCount++;
            tag.putLong(JUMP_TIME_TAG, currentTime);
            tag.putInt(JUMP_COUNT_TAG, jumpCount);
            
            // 只有当连续跳跃超过 2 次时（即第3次及以后），才开始计算并消耗体力
            if (jumpCount > 2) {
                float jumpCost = DayZPlayerStatusManager.getJumpStaminaCost();
                if (!DayZPlayerStatusManager.canJump(player, jumpCost)) {
                    net.minecraft.world.phys.Vec3 movement = player.getDeltaMovement();
                    player.setDeltaMovement(0.0D, Math.min(0.0D, movement.y), 0.0D);
                    player.setSprinting(false);
                    player.hasImpulse = true;
                    return;
                }
                DayZPlayerStatusManager.consumeStamina(player, jumpCost);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player.level().isClientSide) return;

        Player player = event.player;
        if (event.phase == TickEvent.Phase.START) {
            if (player instanceof ServerPlayer serverPlayer && ProneManager.validateServerState(serverPlayer)) {
                ProneManager.broadcastState(serverPlayer);
            }
            if (!DayZPlayerStatusManager.canSprint(player)) {
                if (player.isSprinting()) {
                    player.setSprinting(false);
                }
            }
            return;
        }

        Inventory inv = player.getInventory();
        boolean nursingEnabled = BlockZConfigs.isNursingEnabled();

        if (!DayZPlayerStatusManager.canSprint(player)) {
            if (player.isSprinting()) {
                player.setSprinting(false);
            }
        }
        if (ProneManager.isServerProne(player)) {
            player.setSprinting(false);
        }

        DayZPlayerStatusManager.tick(player);
        applyHealthMovementPenalty(player);
        if (player instanceof ServerPlayer serverPlayer && player.tickCount % 2 == 0) {
            syncDayZStatus(serverPlayer, false);
        }

        if (player.getAbilities().instabuild) {
            if (nursingEnabled && player.hasEffect(ModEffects.FRACTURE.get())) {
                player.removeEffect(ModEffects.FRACTURE.get());
            }
            if (nursingEnabled && player.hasEffect(ModEffects.BLEEDING.get())) {
                player.removeEffect(ModEffects.BLEEDING.get());
            }
            AttributeInstance creativeSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (creativeSpeed != null) {
                clearFractureSpeedModifiers(creativeSpeed);
                clearHealthSpeedModifier(creativeSpeed);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                restoreCreativeFractureMovement(serverPlayer);
            }
        }

        boolean brokenLegsEnabled = nursingEnabled && BlockZConfigs.isBrokenLegsEnabled();
        if (!nursingEnabled) {
            if (player.hasEffect(ModEffects.BLEEDING.get())) {
                player.removeEffect(ModEffects.BLEEDING.get());
            }
            if (player.hasEffect(ModEffects.FRACTURE.get())) {
                player.removeEffect(ModEffects.FRACTURE.get());
            }
            AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                clearFractureSpeedModifiers(speed);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                restoreCreativeFractureMovement(serverPlayer);
            }
        } else {
            if (!BlockZConfigs.isBleedingEnabled() && player.hasEffect(ModEffects.BLEEDING.get())) {
                player.removeEffect(ModEffects.BLEEDING.get());
            }
            if (!brokenLegsEnabled && player.hasEffect(ModEffects.FRACTURE.get())) {
                player.removeEffect(ModEffects.FRACTURE.get());
            }
        }

        if (nursingEnabled && brokenLegsEnabled) {
            MobEffectInstance fractureInstance = player.getEffect(ModEffects.FRACTURE.get());
            boolean fractured = fractureInstance != null;
            boolean analgesic = player.hasEffect(ModEffects.ANALGESIC.get());
            boolean recovering = fractured && fractureInstance.getDuration() <= FRACTURE_RECOVERY_TICKS;
            AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                if (player.getAbilities().instabuild) {
                    clearFractureSpeedModifiers(speed);
                } else if (!fractured) {
                    clearFractureSpeedModifiers(speed);
                } else if (analgesic) {
                    if (speed.getModifier(ANALGESIC_FRACTURE_SPEED_UUID) == null) {
                        speed.addTransientModifier(ANALGESIC_FRACTURE_SPEED_MODIFIER);
                    }
                    if (speed.getModifier(SPLINT_FRACTURE_RECOVERY_SPEED_UUID) != null) {
                        speed.removeModifier(SPLINT_FRACTURE_RECOVERY_SPEED_UUID);
                    }
                } else {
                    if (speed.getModifier(ANALGESIC_FRACTURE_SPEED_UUID) != null) {
                        speed.removeModifier(ANALGESIC_FRACTURE_SPEED_UUID);
                    }
                    if (recovering) {
                        if (speed.getModifier(SPLINT_FRACTURE_RECOVERY_SPEED_UUID) == null) {
                            speed.addTransientModifier(SPLINT_FRACTURE_RECOVERY_SPEED_MODIFIER);
                        }
                    } else if (speed.getModifier(SPLINT_FRACTURE_RECOVERY_SPEED_UUID) != null) {
                        speed.removeModifier(SPLINT_FRACTURE_RECOVERY_SPEED_UUID);
                    }
                }
            }
            updateCreativeFractureMovement(player, fractured, analgesic, recovering);
        } else if (player instanceof ServerPlayer serverPlayer) {
            restoreCreativeFractureMovement(serverPlayer);
        }
        
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
        boolean lockEnabled = BlockZConfigs.getEnableVanillaBackpackLock();

        int allowedSlots = 0;

        // 计算允许的槽位数
        if (!dayzEnabled && !isAdmin && lockEnabled) {
            // 基础口袋槽位 (5格, 对应原版 9-13)
            allowedSlots = BlockZConfigs.getInitialPocketSlots();

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

    private static void clearFractureSpeedModifiers(AttributeInstance speed) {
        if (speed.getModifier(ANALGESIC_FRACTURE_SPEED_UUID) != null) {
            speed.removeModifier(ANALGESIC_FRACTURE_SPEED_UUID);
        }
        if (speed.getModifier(SPLINT_FRACTURE_RECOVERY_SPEED_UUID) != null) {
            speed.removeModifier(SPLINT_FRACTURE_RECOVERY_SPEED_UUID);
        }
    }

    private static void clearHealthSpeedModifier(AttributeInstance speed) {
        if (speed.getModifier(LOW_HEALTH_SPEED_UUID) != null) {
            speed.removeModifier(LOW_HEALTH_SPEED_UUID);
        }
        if (speed.getModifier(PRONE_SPEED_UUID) != null) {
            speed.removeModifier(PRONE_SPEED_UUID);
        }
    }

    private static void clearProneStepHeightModifier(Player player) {
        AttributeInstance stepHeight = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (stepHeight != null && stepHeight.getModifier(PRONE_STEP_HEIGHT_UUID) != null) {
            stepHeight.removeModifier(PRONE_STEP_HEIGHT_UUID);
        }
    }

    private static void applyProneStepHeightModifier(Player player) {
        AttributeInstance stepHeight = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (stepHeight == null) {
            return;
        }
        if (stepHeight.getModifier(PRONE_STEP_HEIGHT_UUID) != null) {
            return;
        }
        stepHeight.addTransientModifier(new AttributeModifier(
                PRONE_STEP_HEIGHT_UUID,
                "blockz_prone_step_height",
                PRONE_STEP_HEIGHT_OFFSET,
                AttributeModifier.Operation.ADDITION
        ));
    }

    private static void applyHealthMovementPenalty(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            clearProneStepHeightModifier(player);
            return;
        }

        if (player.isSpectator() || player.getAbilities().instabuild) {
            clearHealthSpeedModifier(speed);
            clearProneStepHeightModifier(player);
            return;
        }

        clearHealthSpeedModifier(speed);
        if (ProneManager.shouldApplyMovementPenalty(player)) {
            speed.addTransientModifier(new AttributeModifier(
                    PRONE_SPEED_UUID,
                    "blockz_prone_speed",
                    PRONE_SPEED_MULTIPLIER,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
            applyProneStepHeightModifier(player);
        } else {
            clearProneStepHeightModifier(player);
        }
        double amount = DayZPlayerStatusManager.getMovementPenaltyMultiplier(player);
        if (amount >= 0.0D) {
            return;
        }

        speed.addTransientModifier(new AttributeModifier(
                LOW_HEALTH_SPEED_UUID,
                "blockz_low_health_speed",
                amount,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));
    }

    public static void clearStatusSyncCache(ServerPlayer player) {
        LAST_SYNCED_STATUS.remove(player.getUUID());
    }

    private static void syncDayZStatus(ServerPlayer player) {
        syncDayZStatus(player, true);
    }

    private static void syncDayZStatus(ServerPlayer player, boolean force) {
        float healthPointsRatio = DayZPlayerStatusManager.getHealthPointsRatio(player);
        float healthRatio = DayZPlayerStatusManager.getHealthRatio(player);
        float staminaRatio = DayZPlayerStatusManager.getStaminaRatio(player);
        float infectionRatio = DayZPlayerStatusManager.getInfectionRatio(player);
        long gameTime = player.serverLevel().getGameTime();
        UUID playerId = player.getUUID();

        StatusSyncSnapshot current = new StatusSyncSnapshot(
                quantizeStatus(healthPointsRatio),
                quantizeStatus(healthRatio),
                quantizeStatus(staminaRatio),
                quantizeStatus(infectionRatio),
                gameTime
        );
        StatusSyncSnapshot last = LAST_SYNCED_STATUS.get(playerId);
        boolean changed = last == null
                || last.healthPointsRatio != current.healthPointsRatio
                || last.healthRatio != current.healthRatio
                || last.staminaRatio != current.staminaRatio
                || last.infectionRatio != current.infectionRatio;
        boolean intervalElapsed = last == null || gameTime - last.gameTime >= STATUS_SYNC_INTERVAL_TICKS;
        if (!force && !changed && !intervalElapsed) {
            return;
        }

        LAST_SYNCED_STATUS.put(playerId, current);
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlayerStatusS2C(
                healthPointsRatio,
                healthRatio,
                staminaRatio,
                infectionRatio
        ));
    }

    private static int quantizeStatus(float value) {
        return Math.max(0, Math.min(STATUS_SYNC_SCALE, Math.round(value * STATUS_SYNC_SCALE)));
    }

    private static final class StatusSyncSnapshot {
        private final int healthPointsRatio;
        private final int healthRatio;
        private final int staminaRatio;
        private final int infectionRatio;
        private final long gameTime;

        private StatusSyncSnapshot(int healthPointsRatio, int healthRatio, int staminaRatio, int infectionRatio, long gameTime) {
            this.healthPointsRatio = healthPointsRatio;
            this.healthRatio = healthRatio;
            this.staminaRatio = staminaRatio;
            this.infectionRatio = infectionRatio;
            this.gameTime = gameTime;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isSpectator()) return;
        if (!BlockZConfigs.isNursingEnabled()) return;

        if (!BlockZConfigs.isBrokenLegsEnabled()) return;

        float distance = event.getDistance();
        if (distance <= 0.0F) return;
        if (distance < 6.5F) return;

        MobEffectInstance currentFracture = player.getEffect(ModEffects.FRACTURE.get());
        boolean recovering = currentFracture != null && currentFracture.getDuration() <= FRACTURE_RECOVERY_TICKS;
        if (currentFracture != null && !recovering) {
            return;
        }

        float baseChance = (float) (BlockZConfigs.getBrokenLegChanceMultiplier() * player.fallDistance / player.getMaxFallDistance());
        float maxChance = (float) BlockZConfigs.getBrokenLegMaxChance();
        float legBreakChance = Math.min(maxChance, Math.max(0.0F, baseChance));
        if (player.getRandom().nextFloat() < legBreakChance) {
            if (recovering) {
                player.addEffect(new MobEffectInstance(ModEffects.FRACTURE.get(), INFINITE_DURATION, 0, false, false, true));
                PlayerMessageUtils.sendActionbar(player, Component.translatable("msg.blockz.splint_required_again"));
            } else if (player.addEffect(new MobEffectInstance(ModEffects.FRACTURE.get(), INFINITE_DURATION, 0, false, false, true))) {
                PlayerMessageUtils.sendActionbar(player, Component.translatable("msg.blockz.fracture_from_fall"));
            }
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 100, 1, false, false, true));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (event.getAmount() <= 0.0F) return;

        DayZPlayerStatusManager.applyDamage(player, event.getAmount());
        syncDayZStatus(player);

        if (!BlockZConfigs.isNursingEnabled()) return;

        if (!BlockZConfigs.isBleedingEnabled()) return;

        DamageSource source = event.getSource();
        Entity directEntity = source.getDirectEntity();
        Entity attacker = source.getEntity();

        float chance = (float) (BlockZConfigs.getBaseBleedingChance() * event.getAmount());
        if (attacker instanceof Zombie || directEntity instanceof Zombie) {
            chance = Math.max(chance, ZOMBIE_BLEEDING_BASE_CHANCE * event.getAmount());
        }

        chance = Math.min(0.95F, Math.max(0.0F, chance));

        String msgId = source.getMsgId();
        boolean isExplosion = msgId != null && msgId.toLowerCase(java.util.Locale.ROOT).contains("explosion");

        if (!player.hasEffect(ModEffects.BLEEDING.get())
                && (directEntity != null || isExplosion)
                && player.getRandom().nextFloat() < chance
                && player.addEffect(new MobEffectInstance(ModEffects.BLEEDING.get(), INFINITE_DURATION, 0, false, false, true))) {
            PlayerMessageUtils.sendActionbar(player, Component.translatable("msg.blockz.wound_from_attack"));
        }
    }

    private static void updateCreativeFractureMovement(Player player, boolean fractured, boolean analgesic, boolean recovering) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (!player.getAbilities().instabuild) {
            restoreCreativeFractureMovement(serverPlayer);
            return;
        }

        if (!fractured) {
            restoreCreativeFractureMovement(serverPlayer);
            return;
        }

        if (!player.getPersistentData().getBoolean(CREATIVE_FRACTURE_SPEED_OVERRIDE_TAG)) {
            player.getPersistentData().putBoolean(CREATIVE_FRACTURE_SPEED_OVERRIDE_TAG, true);
            player.getPersistentData().putFloat(CREATIVE_FRACTURE_BASE_WALK_SPEED_TAG, player.getAbilities().getWalkingSpeed());
            player.getPersistentData().putFloat(CREATIVE_FRACTURE_BASE_FLY_SPEED_TAG, player.getAbilities().getFlyingSpeed());
        }

        float baseWalkSpeed = player.getPersistentData().getFloat(CREATIVE_FRACTURE_BASE_WALK_SPEED_TAG);
        float baseFlySpeed = player.getPersistentData().getFloat(CREATIVE_FRACTURE_BASE_FLY_SPEED_TAG);
        float speedScale = analgesic ? 1.0F : (recovering ? FRACTURE_RECOVERY_CREATIVE_SPEED_SCALE : FRACTURE_CREATIVE_SPEED_SCALE);
        float targetWalkSpeed = baseWalkSpeed * speedScale;
        float targetFlySpeed = baseFlySpeed * speedScale;

        if (Math.abs(player.getAbilities().getWalkingSpeed() - targetWalkSpeed) > 1.0E-4F
                || Math.abs(player.getAbilities().getFlyingSpeed() - targetFlySpeed) > 1.0E-4F) {
            player.getAbilities().setWalkingSpeed(targetWalkSpeed);
            player.getAbilities().setFlyingSpeed(targetFlySpeed);
            serverPlayer.onUpdateAbilities();
        }
    }

    private static void restoreCreativeFractureMovement(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean(CREATIVE_FRACTURE_SPEED_OVERRIDE_TAG)) {
            return;
        }

        float baseWalkSpeed = player.getPersistentData().getFloat(CREATIVE_FRACTURE_BASE_WALK_SPEED_TAG);
        float baseFlySpeed = player.getPersistentData().getFloat(CREATIVE_FRACTURE_BASE_FLY_SPEED_TAG);
        player.getAbilities().setWalkingSpeed(baseWalkSpeed);
        player.getAbilities().setFlyingSpeed(baseFlySpeed);
        player.onUpdateAbilities();
        player.getPersistentData().remove(CREATIVE_FRACTURE_SPEED_OVERRIDE_TAG);
        player.getPersistentData().remove(CREATIVE_FRACTURE_BASE_WALK_SPEED_TAG);
        player.getPersistentData().remove(CREATIVE_FRACTURE_BASE_FLY_SPEED_TAG);
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
        // 普通箱子、一般容器交给原版或其它模组先打开，再由客户端拦截转换为 DayZ UI，
        // 以避免抢占右键事件导致虚拟容器模组逻辑失效。
        boolean isLootrContainer = isLootr;
        boolean isCraftingTable = level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.CraftingTableBlock;
        boolean isEnchantingTable = level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.EnchantmentTableBlock;

        if (isLootrContainer || isCraftingTable || isEnchantingTable) {
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
                    buf.writeInt(com.yitianys.BlockZ.config.BlockZConfigs.getInitialPocketSlots());
                    buf.writeBoolean(true); // Has Pos
                    buf.writeBlockPos(pos);
                    CuriosIntegration.writeAdditionalDayZSlotRefs(serverPlayer, buf);
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
        if (isModernMayhemBackpackItem(event.getItemStack())) {
            cancelInteract(event, true);
            return;
        }
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

            cancelInteract(event, false);
            return true;
        }
        return false;
    }

    private static void cancelInteract(PlayerInteractEvent event, boolean allowItemOverride) {
        if (event.isCancelable()) {
            event.setCanceled(true);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event instanceof PlayerInteractEvent.RightClickBlock blockEvent) {
            if (blockEvent.isCancelable()) {
                blockEvent.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
                blockEvent.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
            }
        } else if (allowItemOverride && event instanceof PlayerInteractEvent.RightClickItem itemEvent) {
            itemEvent.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static boolean serverTryPickup(PlayerInteractEvent event, Player player) {
        Level level = event.getLevel();
        if (level.isClientSide) return false;

        Entity targeted = InventoryUtils.getTargetedItemEntity(player, 4.0);
        if (targeted instanceof ItemEntity item && item.isAlive()) {
            ItemStack stack = item.getItem();
            if (stack.isEmpty()) return false;

            cancelInteract(event, true);

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

    private static boolean isModernMayhemBackpackItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Class<?> currentClass = stack.getItem().getClass();
        while (currentClass != null) {
            if ("net.tkg.ModernMayhem.server.item.generic.GenericBackpackItem".equals(currentClass.getName())) {
                return true;
            }
            currentClass = currentClass.getSuperclass();
        }
        return false;
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


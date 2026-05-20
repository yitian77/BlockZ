package com.yitianys.BlockZ.entity;

import com.yitianys.BlockZ.compat.CuriosIntegration;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.config.DayZZombieConfig;
import com.yitianys.BlockZ.init.ModEffects;
import com.yitianys.BlockZ.init.ModSounds;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import com.yitianys.BlockZ.util.PlayerMessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class DayZZombieEntity extends Monster implements GeoEntity, net.minecraft.world.Container, MenuProvider {
    private static final String TAG_ATTACK_TICKS = "AttackAnimationTicks";
    private static final String TAG_CORPSE_TICKS = "CorpseTicks";
    private static final String TAG_CORPSE_INVENTORY = "CorpseInventory";
    private static final String TAG_CORPSE_LOOT_INITIALIZED = "CorpseLootInitialized";
    private static final int INFINITE_DURATION = 9999999;
    private static final String BLEEDING_MESSAGE_COOLDOWN = "blockz_dayz_zombie_bleeding";
    private static final int ATTACK_ANIMATION_DURATION = 10;
    private static final int DEATH_ANIMATION_DURATION = 25;
    private static final int CORPSE_CONTAINER_SIZE = 12;
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation_zonbies_idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation_zonbie_walk");
    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("animation_zonbie_attack");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("animation_zonbie_death");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final SimpleContainer corpseInventory = new SimpleContainer(CORPSE_CONTAINER_SIZE);
    private int attackAnimationTicks;
    private int corpseTicks;
    private int alertCooldown;
    private int targetSearchCooldown;
    private int lostTargetTicks;
    private boolean corpseLootInitialized;

    public DayZZombieEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public boolean isLootCorpseAccessible() {
        return this.isDeadOrDying() && this.deathTime >= DEATH_ANIMATION_DURATION && !this.isRemoved();
    }

    private @NotNull InteractionResult openCorpseMenu(@NotNull Player player, @NotNull InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !this.isLootCorpseAccessible()) {
            return InteractionResult.PASS;
        }

        if (this.level().isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, this, buf -> {
                buf.writeInt(BlockZConfigs.getInitialPocketSlots());
                buf.writeBoolean(false);
                buf.writeByte(1);
                buf.writeInt(this.getId());
                CuriosIntegration.writeAdditionalDayZSlotRefs(player, buf);
            });
            return InteractionResult.CONSUME;
        }

        return InteractionResult.SUCCESS;
    }

    public void absorbCorpseLoot(List<ItemStack> droppedStacks) {
        if (this.level().isClientSide || this.corpseLootInitialized) {
            return;
        }

        for (ItemStack droppedStack : droppedStacks) {
            if (droppedStack.isEmpty()) {
                continue;
            }

            ItemStack remaining = this.insertCorpseLoot(droppedStack.copy());
            if (!remaining.isEmpty()) {
                this.spawnAtLocation(remaining.copy(), 0.0F);
            }
        }

        this.corpseLootInitialized = true;
        this.corpseInventory.setChanged();
    }

    private ItemStack insertCorpseLoot(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        for (int i = 0; i < this.corpseInventory.getContainerSize(); i++) {
            ItemStack existing = this.corpseInventory.getItem(i);
            if (!canMergeCorpseLoot(existing, stack)) {
                continue;
            }

            int moveCount = Math.min(existing.getMaxStackSize() - existing.getCount(), stack.getCount());
            if (moveCount <= 0) {
                continue;
            }

            existing.grow(moveCount);
            stack.shrink(moveCount);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        for (int i = 0; i < this.corpseInventory.getContainerSize(); i++) {
            ItemStack existing = this.corpseInventory.getItem(i);
            if (!existing.isEmpty()) {
                continue;
            }

            int moveCount = Math.min(stack.getCount(), stack.getMaxStackSize());
            ItemStack inserted = stack.copy();
            inserted.setCount(moveCount);
            this.corpseInventory.setItem(i, inserted);
            stack.shrink(moveCount);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        return stack;
    }

    private static boolean canMergeCorpseLoot(ItemStack existing, ItemStack incoming) {
        return !existing.isEmpty()
                && ItemStack.isSameItemSameTags(existing, incoming)
                && existing.getCount() < existing.getMaxStackSize();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DayZZombieConfig.DEFAULT_MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, DayZZombieConfig.DEFAULT_MOVEMENT_SPEED)
                .add(Attributes.ATTACK_DAMAGE, DayZZombieConfig.DEFAULT_ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, DayZZombieConfig.DEFAULT_FOLLOW_RANGE)
                .add(Attributes.ARMOR, DayZZombieConfig.DEFAULT_ARMOR)
                .add(Attributes.KNOCKBACK_RESISTANCE, DayZZombieConfig.DEFAULT_KNOCKBACK_RESISTANCE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 20.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<Player>(this, Player.class, 10, true, false, living -> living instanceof Player player && this.isTargetablePlayer(player)));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            this.applyConfiguredAttributes();
        }

        if (this.attackAnimationTicks > 0) {
            this.attackAnimationTicks--;
        }

        if (this.alertCooldown > 0) {
            this.alertCooldown--;
        }

        if (this.targetSearchCooldown > 0) {
            this.targetSearchCooldown--;
        }

        if (this.isDeadOrDying()) {
            this.lostTargetTicks = 0;
            return;
        }

        if (!this.level().isClientSide) {
            this.updateDayzTargeting();
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.getLookControl().setLookAt(target, 75.0F, 40.0F);

            if (!this.level().isClientSide && this.alertCooldown <= 0) {
                this.alertNearbyZombies(target);
                this.alertCooldown = Math.max(1, DayZZombieConfig.getAlertInterval());
            }
        } else {
            this.alertCooldown = 0;
        }
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        
        if (this.deathTime == 12 && !this.level().isClientSide) {
            this.playSound(ModSounds.DAYZ_ZOMBIE_FALL.get(), this.getSoundVolume(), this.getVoicePitch());
        }

        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        this.hasImpulse = false;
        this.hurtTime = 0;
        this.hurtDuration = 0;

        if (this.deathTime >= DEATH_ANIMATION_DURATION) {
            this.setNoAi(true);

            if (!this.level().isClientSide && this.corpseTicks == 0) {
                this.dropExperience();
                if (DayZZombieConfig.getCorpseStayDuration() > 0) {
                    ZombieCorpseEntity corpse = new ZombieCorpseEntity(this.level(), this);
                    this.level().addFreshEntity(corpse);
                }
                this.remove(Entity.RemovalReason.KILLED);
                return;
            }
        }
    }

    private boolean isVisuallyMoving() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-5D || !this.getNavigation().isDone();
    }

    private void updateDayzTargeting() {
        if (!DayZZombieConfig.isCustomSenseEnabled()) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target != null) {
            if (this.canKeepTracking(target)) {
                this.lostTargetTicks = 0;
                return;
            }

            if (this.isWithinTrackingMemoryRange(target) && this.lostTargetTicks++ < Math.max(0, DayZZombieConfig.getTargetMemoryTicks())) {
                return;
            }

            this.setTarget(null);
            this.lostTargetTicks = 0;
        }

        if (this.targetSearchCooldown > 0) {
            return;
        }

        int randomDelay = Math.max(0, DayZZombieConfig.getTargetScanRandomDelay());
        this.targetSearchCooldown = Math.max(1, DayZZombieConfig.getTargetScanInterval()) + (randomDelay > 0 ? this.random.nextInt(randomDelay) : 0);
        Player playerTarget = this.findDayzTarget();
        if (playerTarget != null) {
            this.setTarget(playerTarget);
        }
    }

    private Player findDayzTarget() {
        double followRange = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        double verticalRange = DayZZombieConfig.getTargetSearchVerticalRange();
        AABB searchBox = this.getBoundingBox().inflate(followRange, verticalRange, followRange);
        Player bestTarget = null;
        double bestDistance = Double.MAX_VALUE;

        for (Player player : this.level().getEntitiesOfClass(Player.class, searchBox, this::isTargetablePlayer)) {
            double distanceToTarget = this.distanceToSqr(player);
            if (distanceToTarget < bestDistance && this.canAcquireTarget(player, distanceToTarget)) {
                bestDistance = distanceToTarget;
                bestTarget = player;
            }
        }

        return bestTarget;
    }

    private boolean isTargetablePlayer(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    private boolean canAcquireTarget(Player player, double distanceToTarget) {
        double detectionRange = Math.min(this.getAttributeValue(Attributes.FOLLOW_RANGE), this.getPlayerDetectionRange(player));
        if (distanceToTarget > detectionRange * detectionRange) {
            return false;
        }

        double closeDetectionRange = DayZZombieConfig.getCloseDetectionRange();
        return distanceToTarget <= closeDetectionRange * closeDetectionRange || this.hasLineOfSight(player);
    }

    private double getPlayerDetectionRange(Player player) {
        if (player.isCrouching()) {
            return DayZZombieConfig.getCrouchDetectionRange();
        }

        if (player.isSprinting()) {
            return DayZZombieConfig.getSprintDetectionRange();
        }

        if (player.getDeltaMovement().horizontalDistanceSqr() > 0.015D) {
            return DayZZombieConfig.getMovingDetectionRange();
        }

        return DayZZombieConfig.getWalkDetectionRange();
    }

    private boolean canKeepTracking(LivingEntity target) {
        if (!target.isAlive()) {
            return false;
        }

        if (target instanceof Player player && !this.isTargetablePlayer(player)) {
            return false;
        }

        double distanceToTarget = this.distanceToSqr(target);
        double closeDetectionRange = DayZZombieConfig.getCloseDetectionRange();
        if (distanceToTarget <= closeDetectionRange * closeDetectionRange) {
            return true;
        }

        double followRange = this.getAttributeValue(Attributes.FOLLOW_RANGE) + DayZZombieConfig.getTrackingExtraRange();
        return distanceToTarget <= followRange * followRange && this.hasLineOfSight(target);
    }

    private boolean isWithinTrackingMemoryRange(LivingEntity target) {
        double followRange = this.getAttributeValue(Attributes.FOLLOW_RANGE) + DayZZombieConfig.getTrackingMemoryExtraRange();
        return target.isAlive() && this.distanceToSqr(target) <= followRange * followRange;
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        this.attackAnimationTicks = ATTACK_ANIMATION_DURATION;
        boolean result = super.doHurtTarget(target);
        if (result && target instanceof Player player && BlockZConfigs.isNursingEnabled() && BlockZConfigs.isBleedingEnabled()) {
            double chance = Math.max(0.0D, Math.min(1.0D, DayZZombieConfig.getAttackBleedingChance()));
            if (!player.hasEffect(ModEffects.BLEEDING.get())
                    && this.random.nextDouble() < chance
                    && player.addEffect(new MobEffectInstance(ModEffects.BLEEDING.get(), INFINITE_DURATION, 0, false, false, true))) {
                PlayerMessageUtils.sendActionbarWithCooldown(player, Component.translatable("msg.blockz.wound_from_attack"), BLEEDING_MESSAGE_COOLDOWN, 40);
            }
        }
        return result;
    }

    @Override
    public @NotNull InteractionResult interactAt(@NotNull Player player, @NotNull Vec3 hitVec, @NotNull InteractionHand hand) {
        InteractionResult result = this.openCorpseMenu(player, hand);
        if (result.consumesAction()) {
            return result;
        }
        return super.interactAt(player, hitVec, hand);
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        InteractionResult result = this.openCorpseMenu(player, hand);
        if (result.consumesAction()) {
            return result;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void swing(@NotNull InteractionHand hand) {
        this.startAttackAnimation(hand);
        super.swing(hand);
    }

    @Override
    public void swing(@NotNull InteractionHand hand, boolean updateSelf) {
        this.startAttackAnimation(hand);
        super.swing(hand, updateSelf);
    }

    private void startAttackAnimation(InteractionHand hand) {
        if (!this.isDeadOrDying() && hand == InteractionHand.MAIN_HAND) {
            this.attackAnimationTicks = ATTACK_ANIMATION_DURATION;
        }
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new DayZInventoryMenu(id, playerInventory, (Entity) this);
    }

    @Override
    public int getContainerSize() {
        return this.corpseInventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return this.corpseInventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return this.corpseInventory.getItem(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return this.corpseInventory.removeItem(index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return this.corpseInventory.removeItemNoUpdate(index);
    }

    @Override
    public void setItem(int index, @NotNull ItemStack stack) {
        this.corpseInventory.setItem(index, stack);
    }

    @Override
    public void setChanged() {
        this.corpseInventory.setChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.isLootCorpseAccessible() && player.distanceToSqr(this) < 64.0D;
    }

    @Override
    public void clearContent() {
        this.corpseInventory.clearContent();
    }

    @Override
    public boolean isPickable() {
        return this.isLootCorpseAccessible() || super.isPickable();
    }

    @Override
    public boolean isPushable() {
        return !this.isLootCorpseAccessible() && super.isPushable();
    }

    @Override
    public void push(@NotNull Entity entity) {
        if (!this.isLootCorpseAccessible()) {
            super.push(entity);
        }
    }

    @Override
    protected void doPush(@NotNull Entity entity) {
        if (!this.isLootCorpseAccessible()) {
            super.doPush(entity);
        }
    }

    private void alertNearbyZombies(LivingEntity target) {
        double alertHorizontal = DayZZombieConfig.getAlertRangeHorizontal();
        double alertVertical = DayZZombieConfig.getAlertRangeVertical();
        AABB alertBox = this.getBoundingBox().inflate(alertHorizontal, alertVertical, alertHorizontal);

        for (DayZZombieEntity zombie : this.level().getEntitiesOfClass(DayZZombieEntity.class, alertBox, zombie -> zombie != this && zombie.isAlive())) {
            LivingEntity zombieTarget = zombie.getTarget();
            if ((zombieTarget == null || !zombieTarget.isAlive()) && zombie.isWithinTrackingMemoryRange(target)) {
                zombie.setTarget(target);
                zombie.targetSearchCooldown = Math.max(1, DayZZombieConfig.getTargetScanInterval());
            }
        }
    }

    private void applyConfiguredAttributes() {
        double movementSpeed = DayZZombieConfig.getMovementSpeed();
        double attackDamage = DayZZombieConfig.getAttackDamage();
        double followRange = DayZZombieConfig.getFollowRange();
        if (DayZZombieConfig.isNightBoostEnabled() && this.level().isNight()) {
            movementSpeed *= DayZZombieConfig.getNightMovementSpeedMultiplier();
            attackDamage *= DayZZombieConfig.getNightAttackDamageMultiplier();
            followRange *= DayZZombieConfig.getNightFollowRangeMultiplier();
        }
        this.setConfiguredBaseAttribute(Attributes.MAX_HEALTH, DayZZombieConfig.getMaxHealth(), true);
        this.setConfiguredBaseAttribute(Attributes.MOVEMENT_SPEED, movementSpeed, false);
        this.setConfiguredBaseAttribute(Attributes.ATTACK_DAMAGE, attackDamage, false);
        this.setConfiguredBaseAttribute(Attributes.FOLLOW_RANGE, followRange, false);
        this.setConfiguredBaseAttribute(Attributes.ARMOR, DayZZombieConfig.getArmor(), false);
        this.setConfiguredBaseAttribute(Attributes.KNOCKBACK_RESISTANCE, DayZZombieConfig.getKnockbackResistance(), false);
    }

    private void setConfiguredBaseAttribute(Attribute attribute, double value, boolean clampHealth) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (Math.abs(instance.getBaseValue() - value) > 1.0E-6D) {
            instance.setBaseValue(value);
            if (clampHealth && this.getHealth() > this.getMaxHealth()) {
                this.setHealth(this.getMaxHealth());
            }
        }
    }

    @Override
    public int getExperienceReward() {
        return this.xpReward;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return switch (this.random.nextInt(3)) {
            case 0 -> ModSounds.DAYZ_ZOMBIE_IDLE_1.get();
            case 1 -> ModSounds.DAYZ_ZOMBIE_IDLE_2.get();
            default -> ModSounds.DAYZ_ZOMBIE_IDLE_3.get();
        };
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource source) {
        return switch (this.random.nextInt(3)) {
            case 0 -> ModSounds.DAYZ_ZOMBIE_HURT_1.get();
            case 1 -> ModSounds.DAYZ_ZOMBIE_HURT_2.get();
            default -> ModSounds.DAYZ_ZOMBIE_HURT_3.get();
        };
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.random.nextBoolean() ? ModSounds.DAYZ_ZOMBIE_DEATH_1.get() : ModSounds.DAYZ_ZOMBIE_DEATH_2.get();
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        this.playSound(SoundEvents.ZOMBIE_STEP, 0.15F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    @Override
    protected float getSoundVolume() {
        return 0.9F;
    }

    @Override
    public float getVoicePitch() {
        return 0.8F + this.random.nextFloat() * 0.15F;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_ATTACK_TICKS, this.attackAnimationTicks);
        tag.putInt(TAG_CORPSE_TICKS, this.corpseTicks);
        tag.putBoolean(TAG_CORPSE_LOOT_INITIALIZED, this.corpseLootInitialized);

        ListTag inventoryTag = new ListTag();
        for (int i = 0; i < this.corpseInventory.getContainerSize(); i++) {
            ItemStack stack = this.corpseInventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            CompoundTag itemTag = new CompoundTag();
            itemTag.putByte("Slot", (byte) i);
            stack.save(itemTag);
            inventoryTag.add(itemTag);
        }
        tag.put(TAG_CORPSE_INVENTORY, inventoryTag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.attackAnimationTicks = tag.getInt(TAG_ATTACK_TICKS);
        this.corpseTicks = tag.getInt(TAG_CORPSE_TICKS);
        this.corpseLootInitialized = tag.getBoolean(TAG_CORPSE_LOOT_INITIALIZED);

        this.corpseInventory.clearContent();
        if (tag.contains(TAG_CORPSE_INVENTORY)) {
            ListTag inventoryTag = tag.getList(TAG_CORPSE_INVENTORY, 10);
            for (int i = 0; i < inventoryTag.size(); i++) {
                CompoundTag itemTag = inventoryTag.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot >= 0 && slot < this.corpseInventory.getContainerSize()) {
                    this.corpseInventory.setItem(slot, ItemStack.of(itemTag));
                }
            }
        }
    }

    @Override
    public void registerControllers(@NotNull AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 0, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(DEATH_ANIMATION);
            }

            if (this.attackAnimationTicks > 0 || this.swinging) {
                return state.setAndContinue(ATTACK_ANIMATION);
            }

            if (this.isVisuallyMoving()) {
                return state.setAndContinue(WALK_ANIMATION);
            }

            return state.setAndContinue(IDLE_ANIMATION);
        }));
    }

    @Override
    public @NotNull AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}

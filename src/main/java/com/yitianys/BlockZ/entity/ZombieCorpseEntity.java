package com.yitianys.BlockZ.entity;

import com.yitianys.BlockZ.compat.CuriosIntegration;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.config.DayZZombieConfig;
import com.yitianys.BlockZ.init.ModEntities;
import com.yitianys.BlockZ.item.ClothingItem;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import com.yitianys.BlockZ.util.ItemGridOccupancy;
import com.yitianys.BlockZ.util.ItemSizeManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ZombieCorpseEntity extends Monster implements GeoEntity, Container, MenuProvider {
    private static final String TAG_DESPAWN_TIMER = "DespawnTimer";
    private static final String TAG_INVENTORY = "Inventory";
    private static final EntityDataAccessor<Integer> VARIANT_SEED = SynchedEntityData.defineId(ZombieCorpseEntity.class, EntityDataSerializers.INT);
    private static final int EQUIPMENT_SLOT_COUNT = 9;
    private static final int LOOT_SLOT_START = EQUIPMENT_SLOT_COUNT;
    private static final int LOOT_SLOT_COUNT = 12;
    private static final int LOOT_COLS = 3;
    private static final int CONTAINER_SIZE = EQUIPMENT_SLOT_COUNT + LOOT_SLOT_COUNT;
    private static final RawAnimation CORPSE_ANIMATION = RawAnimation.begin().thenPlayAndHold("animation_zonbie_corpse_pose");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final SimpleContainer inventory = new SimpleContainer(CONTAINER_SIZE);
    private final InvWrapper inventoryWrapper = new InvWrapper(this.inventory);
    private int despawnTimer;

    public ZombieCorpseEntity(EntityType<? extends ZombieCorpseEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoAi(true);
        this.setSilent(true);
    }

    public ZombieCorpseEntity(Level level, DayZZombieEntity zombie) {
        this(ModEntities.DAYZ_ZOMBIE_CORPSE.get(), level);
        this.moveTo(zombie.getX(), zombie.getY(), zombie.getZ(), zombie.getYRot(), zombie.getXRot());
        this.setYBodyRot(zombie.yBodyRot);
        this.setYHeadRot(zombie.yHeadRot);
        this.entityData.set(VARIANT_SEED, zombie.getUUID().hashCode());
        this.despawnTimer = Math.max(0, DayZZombieConfig.getCorpseStayDuration());
        this.setHealth(this.getMaxHealth());
        this.copyEquipmentFromZombie(zombie);
        this.copyLootFromZombie(zombie);
        this.mergeExtraLoot(zombie.getItemBySlot(EquipmentSlot.OFFHAND).copy());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0D);
    }

    public int getVariantSeed() {
        return this.entityData.get(VARIANT_SEED);
    }

    private void copyEquipmentFromZombie(DayZZombieEntity zombie) {
        this.setMappedEquipmentItem(zombie.getItemBySlot(EquipmentSlot.HEAD).copy(), EquipmentSlot.HEAD);
        this.setMappedEquipmentItem(zombie.getItemBySlot(EquipmentSlot.CHEST).copy(), EquipmentSlot.CHEST);
        this.setMappedEquipmentItem(zombie.getItemBySlot(EquipmentSlot.LEGS).copy(), EquipmentSlot.LEGS);
        this.setMappedEquipmentItem(zombie.getItemBySlot(EquipmentSlot.FEET).copy(), EquipmentSlot.FEET);
    }

    private void copyLootFromZombie(DayZZombieEntity zombie) {
        List<ItemStack> lootStacks = new ArrayList<>();
        for (int i = 0; i < Math.min(LOOT_SLOT_COUNT, zombie.getContainerSize()); i++) {
            ItemStack stack = zombie.getItem(i).copy();
            if (!stack.isEmpty()) {
                lootStacks.add(stack);
            }
        }
        this.packLootStacks(lootStacks);
    }

    private void packLootStacks(List<ItemStack> lootStacks) {
        if (lootStacks.isEmpty()) {
            return;
        }
        lootStacks.sort(Comparator
                .comparingInt((ItemStack stack) -> getLootFootprint(stack)).reversed()
                .thenComparingInt(ItemStack::getCount).reversed());

        for (ItemStack stack : lootStacks) {
            if (!this.insertLootStack(stack.copy())) {
                this.spawnAtLocation(stack);
            }
        }
    }

    private int getLootFootprint(ItemStack stack) {
        ItemSizeManager.ItemSize size = ItemSizeManager.getSize(stack);
        return Math.max(1, size.width()) * Math.max(1, size.height());
    }

    private boolean insertLootStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        for (int i = LOOT_SLOT_START; i < this.inventory.getContainerSize(); i++) {
            ItemStack existing = this.inventory.getItem(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                int moveCount = Math.min(existing.getMaxStackSize() - existing.getCount(), stack.getCount());
                if (moveCount > 0) {
                    existing.grow(moveCount);
                    stack.shrink(moveCount);
                    if (stack.isEmpty()) {
                        return true;
                    }
                }
            }
        }

        int anchorSlot = this.findLootAnchorSlot(stack);
        if (anchorSlot == -1) {
            return false;
        }
        this.inventory.setItem(anchorSlot, stack.copy());
        return true;
    }

    private int findLootAnchorSlot(ItemStack stack) {
        ItemSizeManager.ItemSize size = ItemSizeManager.getSize(stack);
        boolean[] blocked = ItemGridOccupancy.computeBlocked(this.inventoryWrapper, LOOT_COLS, LOOT_SLOT_START, LOOT_SLOT_COUNT);
        for (int relIndex = 0; relIndex < LOOT_SLOT_COUNT; relIndex++) {
            if (ItemGridOccupancy.canPlaceAt(blocked, LOOT_COLS, LOOT_SLOT_COUNT, relIndex, size)) {
                return LOOT_SLOT_START + relIndex;
            }
        }
        return -1;
    }

    private void mergeExtraLoot(ItemStack stack) {
        if (!this.insertLootStack(stack.copy())) {
            this.spawnAtLocation(stack);
        }
    }

    private void setMappedEquipmentItem(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) {
            return;
        }
        if (stack.getItem() instanceof ClothingItem clothingItem) {
            switch (clothingItem.getType()) {
                case BACKPACK -> this.inventory.setItem(0, stack);
                case VEST -> this.inventory.setItem(1, stack);
                case SHIRT -> this.inventory.setItem(2, stack);
                case PANTS -> this.inventory.setItem(3, stack);
                case HAT -> this.inventory.setItem(4, stack);
                case SHOES -> this.inventory.setItem(5, stack);
                case MASK -> this.inventory.setItem(7, stack);
                case GLOVES -> this.inventory.setItem(8, stack);
            }
            return;
        }
        switch (slot) {
            case HEAD -> this.inventory.setItem(4, stack);
            case CHEST -> this.inventory.setItem(2, stack);
            case LEGS -> this.inventory.setItem(3, stack);
            case FEET -> this.inventory.setItem(5, stack);
            default -> {
            }
        }
    }

    private @NotNull InteractionResult openCorpseMenu(@NotNull Player player, @NotNull InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
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

    @Override
    protected void registerGoals() {
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT_SEED, 0);
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        this.hasImpulse = false;
        if (!this.level().isClientSide) {
            if (this.despawnTimer > 0) {
                this.despawnTimer--;
            } else {
                this.discard();
            }
        }
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
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        this.despawnTimer = tag.getInt(TAG_DESPAWN_TIMER);
        if (tag.contains("VariantSeed")) {
            this.entityData.set(VARIANT_SEED, tag.getInt("VariantSeed"));
        }
        this.inventory.clearContent();
        if (tag.contains(TAG_INVENTORY)) {
            ListTag list = tag.getList(TAG_INVENTORY, 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag itemTag = list.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot >= 0 && slot < this.inventory.getContainerSize()) {
                    this.inventory.setItem(slot, ItemStack.of(itemTag));
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt(TAG_DESPAWN_TIMER, this.despawnTimer);
        tag.putInt("VariantSeed", this.entityData.get(VARIANT_SEED));
        ListTag list = new ListTag();
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag itemTag = new CompoundTag();
            itemTag.putByte("Slot", (byte) i);
            stack.save(itemTag);
            list.add(itemTag);
        }
        tag.put(TAG_INVENTORY, list);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(@NotNull Entity entity) {
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("entity.blockz.dayz_zombie_corpse");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new DayZInventoryMenu(id, playerInventory, (Entity) this);
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return java.util.List.of(
                this.getItem(5),
                this.getItem(3),
                this.getItem(2),
                this.getItem(4)
        );
    }

    @Override
    public ItemStack getItemBySlot(@NotNull net.minecraft.world.entity.EquipmentSlot slot) {
        int index = switch (slot) {
            case HEAD -> 4;
            case CHEST -> 2;
            case LEGS -> 3;
            case FEET -> 5;
            case OFFHAND -> 6;
            default -> -1;
        };
        return index >= 0 ? this.getItem(index) : ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(@NotNull net.minecraft.world.entity.EquipmentSlot slot, @NotNull ItemStack stack) {
        int index = switch (slot) {
            case HEAD -> 4;
            case CHEST -> 2;
            case LEGS -> 3;
            case FEET -> 5;
            case OFFHAND -> 6;
            default -> -1;
        };
        if (index >= 0) {
            this.setItem(index, stack);
        }
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public int getContainerSize() {
        return this.inventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return this.inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return this.inventory.getItem(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return this.inventory.removeItem(index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return this.inventory.removeItemNoUpdate(index);
    }

    @Override
    public void setItem(int index, @NotNull ItemStack stack) {
        this.inventory.setItem(index, stack);
    }

    @Override
    public void setChanged() {
        this.inventory.setChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return !this.isRemoved() && player.distanceToSqr(this) < 64.0D;
    }

    @Override
    public void clearContent() {
        this.inventory.clearContent();
    }

    @Override
    public void registerControllers(@NotNull AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "corpse_controller", 0, state -> state.setAndContinue(CORPSE_ANIMATION)));
    }

    @Override
    public @NotNull AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}

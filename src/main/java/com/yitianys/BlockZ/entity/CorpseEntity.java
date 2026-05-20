package com.yitianys.BlockZ.entity;

import com.yitianys.BlockZ.compat.CuriosIntegration;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.init.ModEntities;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public class CorpseEntity extends LivingEntity implements Container, MenuProvider {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.STRING);
    // private static final EntityDataAccessor<Integer> DESPAWN_TIMER = SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.INT); // Removed for optimization
    private static final EntityDataAccessor<Float> ROTATION = SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.FLOAT);

    private final SimpleContainer inventory = new SimpleContainer(256);
    private int despawnTimer;

    public CorpseEntity(EntityType<? extends CorpseEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public CorpseEntity(Level level, Player player) {
        this(com.yitianys.BlockZ.init.ModEntities.CORPSE.get(), level);
        this.setPos(player.getX(), player.getY(), player.getZ());
        this.setYRot(player.getYRot());
        this.setYBodyRot(player.yBodyRot);
        this.setYHeadRot(player.yHeadRot);
        // this.setPose(Pose.SLEEPING); // Removed to prevent hitbox shrinking
        
        this.entityData.set(OWNER_UUID, Optional.of(player.getUUID()));
        this.entityData.set(OWNER_NAME, player.getName().getString());
        this.entityData.set(ROTATION, player.getYRot());
        
        this.despawnTimer = BlockZConfigs.getCorpseDespawnTime() * 20; // Seconds to ticks
        
        this.setHealth(20.0f);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 20.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(OWNER_NAME, "");
        // this.entityData.define(DESPAWN_TIMER, 3600 * 20);
        this.entityData.define(ROTATION, 0f);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ROTATION.equals(key)) {
            float rot = this.entityData.get(ROTATION);
            this.setYBodyRot(rot);
            this.setYHeadRot(rot);
            this.setYRot(rot);
            this.yBodyRotO = rot;
            this.yHeadRotO = rot;
            this.yRotO = rot;
        }
    }

    @Override
    public void tick() {
        super.tick(); // Handles movement and physics
        
        // Prevent horizontal movement but allow gravity (Y-axis)
        // Only stop X and Z movement, keep Y movement (gravity)
        Vec3 delta = this.getDeltaMovement();
        this.setDeltaMovement(0, delta.y, 0);
        
        // 强制重置动画状态，确保在逻辑层面也不产生摆动
        this.walkAnimation.setSpeed(0);
        this.walkAnimation.position(0);
        this.attackAnim = 0;
        
        if (!this.level().isClientSide) {
            // Despawn logic
            if (this.despawnTimer > 0) {
                this.despawnTimer--;
            } else {
                this.discard();
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.isAlive() && !this.level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            NetworkHooks.openScreen((ServerPlayer) player, this, buf -> {
                buf.writeInt(BlockZConfigs.getInitialPocketSlots()); // Pocket Count for DayZInventoryMenu.fromNetwork
                buf.writeBoolean(false); // hasPos = false
                buf.writeByte(1); // Type 1: Entity
                buf.writeInt(this.getId()); // Entity ID
                CuriosIntegration.writeAdditionalDayZSlotRefs(player, buf);
            });
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new DayZInventoryMenu(id, playerInv, (net.minecraft.world.entity.Entity) this);
    }

    @Override
    public Component getDisplayName() {
        String name = this.entityData.get(OWNER_NAME);
        if (name == null || name.isEmpty()) {
            return super.getDisplayName();
        }
        return Component.literal(name);
    }

    public float getCorpseRotation() {
        return this.entityData.get(ROTATION);
    }
    
    // --- LivingEntity Implementation (Dummy) ---
    
    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return java.util.List.of(
            getItem(5), // FEET
            getItem(3), // LEGS
            getItem(2), // CHEST
            getItem(4)  // HEAD
        );
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        int index = -1;
        switch (slot) {
            case HEAD -> index = 4;
            case CHEST -> index = 2;
            case LEGS -> index = 3;
            case FEET -> index = 5;
            case OFFHAND -> index = 6;
        }
        return index != -1 ? getItem(index) : ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        // Do nothing or map to inventory?
        // For rendering, we might want to map:
        // HEAD -> slot 4
        // CHEST -> slot 2
        // LEGS -> slot 3
        // FEET -> slot 5
        // MAINHAND -> empty
        // OFFHAND -> slot 6
        int index = -1;
        switch (slot) {
            case HEAD -> index = 4;
            case CHEST -> index = 2;
            case LEGS -> index = 3;
            case FEET -> index = 5;
            case OFFHAND -> index = 6;
        }
        if (index != -1) {
            setItem(index, stack);
        }
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    // --- Container Implementation ---

    @Override
    public int getContainerSize() {
        return inventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return inventory.getItem(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return inventory.removeItem(index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return inventory.removeItemNoUpdate(index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        inventory.setItem(index, stack);
    }

    @Override
    public void setChanged() {
        inventory.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return !this.isRemoved() && player.distanceToSqr(this) < 64.0;
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }

    // --- Save/Load ---

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("OwnerUUID")) {
            this.entityData.set(OWNER_UUID, Optional.of(tag.getUUID("OwnerUUID")));
        }
        if (tag.contains("OwnerName")) {
            this.entityData.set(OWNER_NAME, tag.getString("OwnerName"));
        }
        if (tag.contains("DespawnTimer")) {
            this.despawnTimer = tag.getInt("DespawnTimer");
        }
        if (tag.contains("Rotation")) {
            this.entityData.set(ROTATION, tag.getFloat("Rotation"));
        }
        if (tag.contains("Inventory")) {
            this.inventory.removeAllItems();
            net.minecraft.nbt.ListTag list = tag.getList("Inventory", 10);
            for(int i = 0; i < list.size(); ++i) {
                CompoundTag itemTag = list.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot >= 0 && slot < this.inventory.getContainerSize()) {
                    this.inventory.setItem(slot, ItemStack.of(itemTag));
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.entityData.get(OWNER_UUID).ifPresent(uuid -> tag.putUUID("OwnerUUID", uuid));
        tag.putString("OwnerName", this.entityData.get(OWNER_NAME));
        tag.putInt("DespawnTimer", this.despawnTimer);
        tag.putFloat("Rotation", this.entityData.get(ROTATION));
        
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for(int i = 0; i < this.inventory.getContainerSize(); ++i) {
            ItemStack stack = this.inventory.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte)i);
                stack.save(itemTag);
                list.add(itemTag);
            }
        }
        tag.put("Inventory", list);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
    
    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(OWNER_UUID);
    }
    
    public String getOwnerName() {
        return this.entityData.get(OWNER_NAME);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }
    
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
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
    public void push(Entity entity) {
        // Do nothing
    }

    @Override
    protected void doPush(Entity entity) {
        // Do nothing
    }
}

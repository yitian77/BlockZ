package com.yitianys.BlockZ.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MedicalItem extends Item {
    public static final String MEDICAL_EFFECT_TAG = "MedicalEffect";
    public static final String MEDICAL_EFFECT_DESC_TAG = "MedicalEffectDesc";

    private final String effectId;
    private final String effectDescription;

    public MedicalItem(Properties properties, String effectId, String effectDescription) {
        super(properties);
        this.effectId = effectId;
        this.effectDescription = effectDescription;
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        ensureMedicalTags(stack);
        return stack;
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        ensureMedicalTags(stack);
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        ensureMedicalTags(stack);
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        tooltipComponents.add(Component.literal("§7作用: " + effectDescription));
    }

    protected final void ensureMedicalTags(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!effectId.equals(tag.getString(MEDICAL_EFFECT_TAG))) {
            tag.putString(MEDICAL_EFFECT_TAG, effectId);
        }
        if (!effectDescription.equals(tag.getString(MEDICAL_EFFECT_DESC_TAG))) {
            tag.putString(MEDICAL_EFFECT_DESC_TAG, effectDescription);
        }
    }
}

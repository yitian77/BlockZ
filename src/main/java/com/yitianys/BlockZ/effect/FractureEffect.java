package com.yitianys.BlockZ.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.yitianys.BlockZ.init.ModItems;
import com.yitianys.BlockZ.config.BlockZConfigs;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.yitianys.BlockZ.util.PlayerMessageUtils;

public class FractureEffect extends MobEffect {
    private static final UUID MOVEMENT_SPEED_MODIFIER = UUID.fromString("021beaa1-498f-4d7b-933e-f0fa0b88b9d1");
    public static final String SUPPRESS_FRACTURE_RECOVERED_MESSAGE_TAG = "blockz_suppress_fracture_recovered_message";

    public FractureEffect(MobEffectCategory category, int color) {
        super(category, color);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_MODIFIER.toString(), -0.55D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return;
        }
        super.addAttributeModifiers(entity, attributeMap, amplifier);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        if (entity.level().isClientSide) {
            return;
        }
        if (entity.getPersistentData().getBoolean(SUPPRESS_FRACTURE_RECOVERED_MESSAGE_TAG)) {
            entity.getPersistentData().remove(SUPPRESS_FRACTURE_RECOVERED_MESSAGE_TAG);
            return;
        }
        if (!BlockZConfigs.isNursingEnabled() || !BlockZConfigs.isBrokenLegsEnabled()) {
            return;
        }
        if (entity instanceof Player player) {
            PlayerMessageUtils.sendActionbar(player, Component.translatable("msg.blockz.fracture_recovered"));
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        List<ItemStack> items = new ArrayList<>();
        items.add(new ItemStack(ModItems.SPLINT.get()));
        return items;
    }
}

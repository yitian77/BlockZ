package com.yitianys.BlockZ.effect;

import java.util.ArrayList;
import java.util.List;

import com.yitianys.BlockZ.init.ModItems;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class BleedingEffect extends MobEffect {
    private static final int MAXIMUM_DELAY = 20 * 6;
    private static final int MINIMUM_DELAY = 10;
    private static final int DELAY_REDUCTION_PER_LEVEL = 20;

    public BleedingEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.getHealth() > 1.0F) {
            if (entity.hurt(entity.damageSources().generic(), 1.0F) && entity.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                        entity.getX(),
                        entity.getY() + (entity.getBbHeight() * 0.5D),
                        entity.getZ(),
                        12,
                        0.3D, 0.4D, 0.3D,
                        0.02D
                );
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        int ticksBetweenHits = MAXIMUM_DELAY - (amplifier * DELAY_REDUCTION_PER_LEVEL);
        return duration % Math.max(ticksBetweenHits, MINIMUM_DELAY) == 0;
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        List<ItemStack> items = new ArrayList<>();
        items.add(new ItemStack(ModItems.BANDAGE.get()));
        items.add(new ItemStack(ModItems.RAGS.get()));
        return items;
    }
}

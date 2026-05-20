package com.yitianys.BlockZ.item;

import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.init.ModEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class MorphineSyringeItem extends MedicalItem {
    private static final int USE_DURATION = 24;

    public MorphineSyringeItem(Properties properties) {
        super(properties, "pain_relief_strong", "强效止痛，短时间提高抗性并缓慢回复");
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!BlockZConfigs.isNursingEnabled()) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity) {
        if (!(livingEntity instanceof Player player)) {
            return stack;
        }
        if (level.isClientSide) {
            return stack;
        }
        ensureMedicalTags(stack);
        player.addEffect(new MobEffectInstance(ModEffects.ANALGESIC.get(), 20 * 60 * 4, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 45, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 12, 1, false, false, true));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }
}

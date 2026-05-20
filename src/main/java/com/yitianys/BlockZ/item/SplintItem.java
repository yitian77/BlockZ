package com.yitianys.BlockZ.item;

import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.effect.FractureEffect;
import com.yitianys.BlockZ.init.ModEffects;
import com.yitianys.BlockZ.util.PlayerMessageUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class SplintItem extends MedicalItem {
    private static final int SPLINT_USE_DURATION = 32;
    private static final int FRACTURE_RECOVERY_TICKS = 20 * 60 * 5;

    public SplintItem(Properties properties) {
        super(properties, "fracture_support", "用于固定骨折并开始恢复");
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        ensureMedicalTags(stack);
        if (!BlockZConfigs.isNursingEnabled() || !BlockZConfigs.isBrokenLegsEnabled()) {
            return InteractionResultHolder.fail(stack);
        }
        if (!player.hasEffect(ModEffects.FRACTURE.get())) {
            if (!level.isClientSide) {
                PlayerMessageUtils.sendActionbarWithCooldown(player, Component.translatable("msg.blockz.splint_no_fracture"), "blockz_splint_no_fracture", 20 * 3);
            }
            return InteractionResultHolder.fail(stack);
        }

        var instance = player.getEffect(ModEffects.FRACTURE.get());
        if (instance != null && instance.getDuration() <= FRACTURE_RECOVERY_TICKS) {
            if (!level.isClientSide) {
                PlayerMessageUtils.sendActionbarWithCooldown(player, Component.translatable("msg.blockz.splint_already_applied"), "blockz_splint_already_applied", 20 * 3);
            }
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return SPLINT_USE_DURATION;
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

        if (!player.hasEffect(ModEffects.FRACTURE.get())) {
            PlayerMessageUtils.sendActionbarWithCooldown(player, Component.translatable("msg.blockz.splint_no_fracture"), "blockz_splint_no_fracture", 20 * 3);
            return stack;
        }

        player.getPersistentData().putBoolean(FractureEffect.SUPPRESS_FRACTURE_RECOVERED_MESSAGE_TAG, true);
        player.removeEffect(ModEffects.FRACTURE.get());
        player.addEffect(new MobEffectInstance(ModEffects.FRACTURE.get(), FRACTURE_RECOVERY_TICKS, 0, false, false, true));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        PlayerMessageUtils.sendActionbar(player, Component.translatable("msg.blockz.splint_applied"));
        return stack;
    }
}

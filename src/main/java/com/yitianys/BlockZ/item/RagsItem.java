package com.yitianys.BlockZ.item;

import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.init.ModEffects;
import com.yitianys.BlockZ.util.PlayerMessageUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class RagsItem extends MedicalItem {
    private static final int USE_DURATION = 32;

    public RagsItem(Properties properties) {
        super(properties, "rags", "可作为基础包扎材料");
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        ensureMedicalTags(stack);
        if (!BlockZConfigs.isNursingEnabled() || !BlockZConfigs.isBleedingEnabled()) {
            return InteractionResultHolder.fail(stack);
        }
        if (!player.hasEffect(ModEffects.BLEEDING.get())) {
            if (!level.isClientSide) {
                PlayerMessageUtils.sendActionbarWithCooldown(player, Component.translatable("msg.blockz.rags_no_wound"), "blockz_rags_no_wound", 20 * 3);
            }
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

        if (!player.hasEffect(ModEffects.BLEEDING.get())) {
            PlayerMessageUtils.sendActionbarWithCooldown(player, Component.translatable("msg.blockz.rags_no_wound"), "blockz_rags_no_wound", 20 * 3);
            return stack;
        }

        boolean success = player.getRandom().nextFloat() < 0.65F;
        if (success) {
            player.removeEffect(ModEffects.BLEEDING.get());
            PlayerMessageUtils.sendActionbar(player, Component.translatable("msg.blockz.rags_applied"));
        } else {
            PlayerMessageUtils.sendActionbar(player, Component.translatable("msg.blockz.rags_failed"));
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }
}

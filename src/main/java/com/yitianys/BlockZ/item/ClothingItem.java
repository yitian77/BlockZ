package com.yitianys.BlockZ.item;

import com.yitianys.BlockZ.init.ModItems;
import com.yitianys.BlockZ.util.PlayerMessageUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ClothingItem extends Item {
    private final ClothingType type;
    private static final int TEAR_DURATION = 32;

    public ClothingItem(Properties properties, ClothingType type) {
        super(properties);
        this.type = type;
    }

    public ClothingType getType() {
        return type;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!canTearIntoRags(stack)) {
            return InteractionResultHolder.pass(stack);
        }
        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return canTearIntoRags(stack) ? TEAR_DURATION : 0;
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
        if (!canTearIntoRags(stack)) {
            return stack;
        }
        if (!level.isClientSide) {
            int ragCount = 2 + player.getRandom().nextInt(2);
            ItemStack rags = new ItemStack(ModItems.RAGS.get(), ragCount);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (!player.addItem(rags)) {
                player.drop(rags, false);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                PlayerMessageUtils.sendActionbar(serverPlayer, Component.translatable("msg.blockz.rags_created", ragCount));
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

        if (!stack.hasTag()) return;
        CompoundTag tag = stack.getTag();
        if (!tag.contains("Inventory")) return;
        
        CompoundTag invTag = tag.getCompound("Inventory");
        if (!invTag.contains("Items")) return;

        ListTag items = invTag.getList("Items", 10);
        if (items.isEmpty()) return;

        tooltipComponents.add(Component.literal("§7Contents:"));
        
        int count = 0;
        for (int i = 0; i < items.size(); i++) {
            if (count >= 5) { // Limit to 5 items
                tooltipComponents.add(Component.literal("§7... and " + (items.size() - count) + " more"));
                break;
            }
            
            CompoundTag itemTag = items.getCompound(i);
            ItemStack itemStack = ItemStack.of(itemTag);
            
            if (!itemStack.isEmpty()) {
                tooltipComponents.add(Component.literal("§8- " + itemStack.getHoverName().getString() + " x" + itemStack.getCount()));
                count++;
            }
        }
    }

    private boolean canTearIntoRags(ItemStack stack) {
        return switch (type) {
            case SHIRT, PANTS, VEST -> isInventoryEmpty(stack);
            default -> false;
        };
    }

    private boolean isInventoryEmpty(ItemStack stack) {
        if (!stack.hasTag()) {
            return true;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("Inventory")) {
            return true;
        }

        CompoundTag invTag = tag.getCompound("Inventory");
        if (!invTag.contains("Items")) {
            return true;
        }

        ListTag items = invTag.getList("Items", 10);
        for (int i = 0; i < items.size(); i++) {
            ItemStack itemStack = ItemStack.of(items.getCompound(i));
            if (!itemStack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public enum ClothingType {
        VEST,
        GLOVES,
        MASK,
        HAT,
        SHIRT,
        PANTS,
        SHOES,
        BACKPACK
    }
}

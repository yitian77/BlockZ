package com.yitianys.BlockZ.mixin;

import com.yitianys.BlockZ.compat.TaczAmmoCompat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Pseudo
@Mixin(targets = "com.tacz.guns.item.ModernKineticGunScriptAPI")
public abstract class MixinModernKineticGunScriptAPI {
    @Inject(method = "consumeAmmoFromPlayer", at = @At("RETURN"), cancellable = true, remap = false)
    private void blockz$consumeAmmoFromStorage(int neededAmount, CallbackInfoReturnable<Integer> cir) {
        int extracted = cir.getReturnValueI();
        if (extracted >= neededAmount) {
            return;
        }

        Player player = blockz$getPlayer(this);
        ItemStack gunStack = blockz$getItemStack(this);
        if (player == null || gunStack.isEmpty()) {
            return;
        }

        int extra = TaczAmmoCompat.extractCompatibleAmmo(player, gunStack, neededAmount - extracted);
        if (extra > 0) {
            cir.setReturnValue(extracted + extra);
        }
    }

    @Inject(method = "hasAmmoToConsume", at = @At("RETURN"), cancellable = true, remap = false)
    private void blockz$hasAmmoToConsume(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }

        Player player = blockz$getPlayer(this);
        ItemStack gunStack = blockz$getItemStack(this);
        if (player != null && !gunStack.isEmpty() && TaczAmmoCompat.hasCompatibleAmmo(player, gunStack)) {
            cir.setReturnValue(true);
        }
    }

    private static Player blockz$getPlayer(Object instance) {
        Object value = blockz$readField(instance, "shooter");
        return value instanceof Player player ? player : null;
    }

    private static ItemStack blockz$getItemStack(Object instance) {
        Object value = blockz$readField(instance, "itemStack");
        return value instanceof ItemStack stack ? stack : ItemStack.EMPTY;
    }

    private static Object blockz$readField(Object instance, String fieldName) {
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(instance);
        } catch (Exception ignored) {
            return null;
        }
    }
}

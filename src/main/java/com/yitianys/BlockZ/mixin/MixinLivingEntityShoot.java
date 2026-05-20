package com.yitianys.BlockZ.mixin;

import com.yitianys.BlockZ.compat.TaczAmmoCompat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "com.tacz.guns.entity.shooter.LivingEntityShoot")
public abstract class MixinLivingEntityShoot {
    @Inject(method = "consumeAmmoFromPlayer", at = @At("HEAD"), cancellable = true, remap = false)
    private void blockz$consumeAmmoFromStorage(int neededAmount, ItemStack itemStack, boolean needCheckAmmo, CallbackInfo ci) {
        if (neededAmount <= 0 || itemStack.isEmpty()) {
            ci.cancel();
            return;
        }

        LivingEntity shooter = blockz$getShooter(this);
        if (shooter == null || !needCheckAmmo) {
            ci.cancel();
            return;
        }

        Object gunItem = itemStack.getItem();
        if (blockz$invokeBoolean(gunItem, "useDummyAmmo", new Class<?>[]{ItemStack.class}, itemStack)) {
            blockz$invokeInt(gunItem, "findAndExtractDummyAmmo", new Class<?>[]{ItemStack.class, int.class}, itemStack, neededAmount);
            ci.cancel();
            return;
        }

        int extracted = shooter.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
                .map(cap -> blockz$invokeInt(gunItem, "findAndExtractInventoryAmmo", new Class<?>[]{IItemHandler.class, ItemStack.class, int.class}, cap, itemStack, neededAmount))
                .orElse(0);

        if (extracted < neededAmount && shooter instanceof Player player) {
            TaczAmmoCompat.extractCompatibleAmmo(player, itemStack, neededAmount - extracted);
        }
        ci.cancel();
    }

    private static LivingEntity blockz$getShooter(Object instance) {
        Object value = blockz$readField(instance, "shooter");
        return value instanceof LivingEntity living ? living : null;
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

    private static boolean blockz$invokeBoolean(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            Object value = method.invoke(target, args);
            return value instanceof Boolean result && result;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int blockz$invokeInt(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            Object value = method.invoke(target, args);
            return value instanceof Number number ? number.intValue() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }
}

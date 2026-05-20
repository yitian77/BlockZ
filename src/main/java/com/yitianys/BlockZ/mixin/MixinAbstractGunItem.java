package com.yitianys.BlockZ.mixin;

import com.yitianys.BlockZ.compat.TaczAmmoCompat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.tacz.guns.api.item.gun.AbstractGunItem")
public abstract class MixinAbstractGunItem {
    @Inject(method = "hasInventoryAmmo", at = @At("RETURN"), cancellable = true, remap = false)
    private void blockz$hasInventoryAmmo(LivingEntity shooter, ItemStack gun, boolean needCheckAmmo, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }
        if (shooter instanceof Player player && TaczAmmoCompat.hasCompatibleAmmo(player, gun)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canReload", at = @At("RETURN"), cancellable = true, remap = false)
    private void blockz$canReload(LivingEntity shooter, ItemStack gunItem, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }
        if (shooter instanceof Player player && TaczAmmoCompat.hasCompatibleAmmo(player, gunItem)) {
            cir.setReturnValue(true);
        }
    }
}

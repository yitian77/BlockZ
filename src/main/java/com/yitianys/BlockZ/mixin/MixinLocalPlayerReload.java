package com.yitianys.BlockZ.mixin;

import com.yitianys.BlockZ.compat.TaczAmmoCompat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.tacz.guns.client.gameplay.LocalPlayerReload")
public abstract class MixinLocalPlayerReload {
    @Redirect(
            method = "reload",
            at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/gun/AbstractGunItem;canReload(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z"),
            remap = false,
            require = 0
    )
    private boolean blockz$canReloadFromStorage(Object gunItem, LivingEntity shooter, ItemStack gunStack) {
        return TaczAmmoCompat.canReloadWithCompatibleAmmo(gunItem, shooter, gunStack);
    }
}

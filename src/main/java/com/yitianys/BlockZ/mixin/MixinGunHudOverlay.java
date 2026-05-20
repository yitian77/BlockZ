package com.yitianys.BlockZ.mixin;

import com.yitianys.BlockZ.compat.TaczAmmoCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.tacz.guns.client.gui.overlay.GunHudOverlay")
public abstract class MixinGunHudOverlay {
    @Shadow(remap = false)
    private static int cacheInventoryAmmoCount;

    @Redirect(
            method = "handleCacheCount",
            at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/gui/overlay/GunHudOverlay;handleInventoryAmmo(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Inventory;)V"),
            remap = false,
            require = 0
    )
    private static void blockz$handleInventoryAmmoWithStorage(ItemStack stack, Inventory inventory) {
        cacheInventoryAmmoCount = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack inventoryItem = inventory.getItem(i);
            int ammoCount = TaczAmmoCompat.countAmmoStack(inventoryItem, stack);
            if (ammoCount >= 9999) {
                cacheInventoryAmmoCount = 9999;
                return;
            }
            cacheInventoryAmmoCount += ammoCount;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            cacheInventoryAmmoCount = Math.min(9999, cacheInventoryAmmoCount + TaczAmmoCompat.countCompatibleAmmo(player, stack));
        }
    }
}

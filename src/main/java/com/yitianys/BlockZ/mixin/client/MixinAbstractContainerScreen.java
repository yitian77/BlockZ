package com.yitianys.BlockZ.mixin.client;

import com.yitianys.BlockZ.client.gui.DayZInventoryScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen {

    @Shadow @Nullable protected Slot clickedSlot;
    @Shadow @Nullable protected ItemStack draggingItem;

    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void onRenderSlot(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        if ((Object) this instanceof DayZInventoryScreen dayZScreen) {
            // Ensure draggingItem is not null to prevent crashes if it hasn't been initialized
            ItemStack safeDraggingItem = this.draggingItem == null ? ItemStack.EMPTY : this.draggingItem;
            
            if (dayZScreen.renderCustomSlot(graphics, slot, this.clickedSlot, safeDraggingItem)) {
                ci.cancel();
            }
        }
    }
}

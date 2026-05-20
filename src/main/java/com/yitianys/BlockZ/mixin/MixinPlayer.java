package com.yitianys.BlockZ.mixin;

import com.yitianys.BlockZ.capability.BlockZPlayerItemHandler;
import com.yitianys.BlockZ.util.LeanManager;
import com.yitianys.BlockZ.util.ProneManager;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayer {
    @Unique
    private BlockZPlayerItemHandler blockz$itemHandler;

    @Unique
    private LazyOptional<IItemHandler> blockz$itemHandlerCap = LazyOptional.empty();

    @Inject(method = "getCapability", at = @At("HEAD"), cancellable = true, remap = false)
    private <T> void blockz$getCapability(Capability<T> cap, Direction side, CallbackInfoReturnable<LazyOptional<T>> cir) {
        if (cap != ForgeCapabilities.ITEM_HANDLER || side != null) {
            return;
        }
        if (!blockz$itemHandlerCap.isPresent()) {
            Player player = (Player) (Object) this;
            blockz$itemHandler = new BlockZPlayerItemHandler(player);
            blockz$itemHandlerCap = LazyOptional.of(() -> blockz$itemHandler);
        }
        cir.setReturnValue(blockz$itemHandlerCap.cast());
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void blockz$tick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide()) {
            LeanManager.tickServerLeanProgress(player);
        }
        LeanManager.alignBodyToHead(player);
        ProneManager.tickPlayer(player);

        if (blockz$itemHandler != null) {
            blockz$itemHandler.syncNestedStorages();
        }
    }
}

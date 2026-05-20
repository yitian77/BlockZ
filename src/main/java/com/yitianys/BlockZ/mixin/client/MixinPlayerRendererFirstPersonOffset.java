package com.yitianys.BlockZ.mixin.client;

import com.yitianys.BlockZ.client.renderer.FirstPersonBodyRenderHelper;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
public class MixinPlayerRendererFirstPersonOffset {
    @Inject(method = "getRenderOffset", at = @At("RETURN"), cancellable = true)
    private void blockz$offsetFirstPersonBody(AbstractClientPlayer player, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        cir.setReturnValue(FirstPersonBodyRenderHelper.getPlayerRenderOffset(player, partialTick, cir.getReturnValue()));
    }
}

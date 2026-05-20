package com.yitianys.BlockZ.mixin.client;

import com.tacz.guns.network.message.ClientMessagePlayerShoot;
import com.yitianys.BlockZ.client.camera.ThirdPersonShootAimHelper;
import com.yitianys.BlockZ.compat.TaczShootAimOverrideAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.tacz.guns.network.message.ClientMessagePlayerShoot", remap = false)
public abstract class MixinClientMessagePlayerShootClient {
    @Inject(method = "encode", at = @At("HEAD"), remap = false)
    private static void blockz$applyThirdPersonAimOverride(ClientMessagePlayerShoot message, FriendlyByteBuf buf, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        Vec3 aimTarget = ThirdPersonShootAimHelper.resolveThirdPersonAimTarget(minecraft, player, 1.0F, 256.0D);
        if (aimTarget == null) {
            return;
        }
        ThirdPersonShootAimHelper.AimAngles aimAngles = ThirdPersonShootAimHelper.resolveAimAnglesFromPlayerToTarget(player, aimTarget);
        if (aimAngles == null) {
            return;
        }
        ((TaczShootAimOverrideAccess) message).blockz$setShootAimOverride(aimAngles.pitch(), aimAngles.yaw());
    }
}

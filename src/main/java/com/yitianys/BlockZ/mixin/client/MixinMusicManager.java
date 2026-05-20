package com.yitianys.BlockZ.mixin.client;

import com.yitianys.BlockZ.config.BlockZConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicManager.class)
public class MixinMusicManager {
    @Shadow private net.minecraft.client.resources.sounds.SoundInstance currentMusic;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void blockz$onTick(CallbackInfo ci) {
        if (shouldBlockMusic()) {
            if (this.currentMusic != null) {
                ((MusicManager)(Object)this).stopPlaying();
            }
            ci.cancel(); 
        }
    }

    @Inject(method = "startPlaying", at = @At("HEAD"), cancellable = true)
    private void blockz$onStartPlaying(net.minecraft.sounds.Music pMusic, CallbackInfo ci) {
        if (shouldBlockMusic()) {
            ci.cancel();
        }
    }

    private boolean shouldBlockMusic() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null && BlockZConfigs.isCustomMainMenuEnabled();
    }
}

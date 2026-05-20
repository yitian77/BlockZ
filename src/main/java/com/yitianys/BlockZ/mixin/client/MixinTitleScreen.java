package com.yitianys.BlockZ.mixin.client;

import com.yitianys.BlockZ.client.gui.mainmenu.DayZMainMenuScreen;
import com.yitianys.BlockZ.config.BlockZConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void blockz$onInit(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!BlockZConfigs.isCustomMainMenuEnabled()) {
            return;
        }
        if (mc != null) {
            mc.setScreen(new DayZMainMenuScreen());
            ci.cancel();
        }
    }
}

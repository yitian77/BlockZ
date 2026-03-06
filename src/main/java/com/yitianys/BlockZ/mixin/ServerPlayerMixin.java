package com.yitianys.BlockZ.mixin;

import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraftforge.network.NetworkHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "openMenu", at = @At("HEAD"), cancellable = true)
    public void onOpenMenu(MenuProvider provider, CallbackInfoReturnable<OptionalInt> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        
        // 检查 DayZ 是否启用
        if (provider == null) return;
        boolean dayzEnabled = player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                .map(bp -> bp.isDayzEnabled())
                .orElse(true);
        
        if (!dayzEnabled) return;
        
        // 当前版本：箱子等容器的 DayZ 换皮完全交给客户端 ScreenEvent.Opening + 网络包处理
        // 这里不再拦截 openMenu，避免多次 startOpen/stopOpen 导致重复音效和计数异常
    }
}

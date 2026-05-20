package com.yitianys.BlockZ.client.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.config.BlockZConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * DayZ 废土风格电影感暗角 / 黑边处理器。
 *
 * 设计原则：
 * - SRP：只负责屏幕边缘视觉压暗，不处理世界雾气、HUD 数据等。
 * - 挂载在 HOTBAR overlay 之后：该 overlay 只在游戏运行中触发，
 *   绝不会在任意打开的 Screen（背包、菜单、主菜单）下渲染，
 *   天然规避"暗角覆盖 GUI"的问题，不需要额外状态检查。
 * - 使用顶/底两条黑色垂直渐变模拟电影 Letterbox，比径向暗角更接近
 *   DayZ 的"压抑 + 荒凉"观感，同时不遮挡中央操作区。
 */
@Mod.EventBusSubscriber(modid = BlockZ.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WastelandVignetteHandler {

    // 黑边占屏幕高度的比例 (0~1)。不做配置，保持视觉统一；
    // 如需更窄/更宽，修改此常量即可。
    private static final float BAND_RATIO = 0.22F;

    private WastelandVignetteHandler() {}

    @SubscribeEvent
    public static void onOverlayPost(RenderGuiOverlayEvent.Post event) {
        // 仅在 HOTBAR 之后渲染一次：HOTBAR 是游戏内稳定存在的 overlay
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            return;
        }
        if (!BlockZConfigs.getEnableVignette()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return; // 玩家按 F1 隐藏 HUD 时也隐藏暗角，保留截图干净观感
        }

        float strength = (float) BlockZConfigs.getVignetteStrength();
        if (strength <= 0.0F) {
            return;
        }

        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();
        int band = Math.max(1, (int) (h * BAND_RATIO));

        // ARGB 颜色：强度映射到 alpha (0~200 之间)，RGB 为 0 表示纯黑
        int alpha = Math.max(0, Math.min(255, (int) (strength * 200.0F)));
        int darkColor = alpha << 24;
        int transparent = 0;

        RenderSystem.enableBlend();

        // 顶部：从黑色渐变到透明 (0..band)
        g.fillGradient(0, 0, w, band, darkColor, transparent);
        // 底部：从透明渐变到黑色 (h-band..h)
        g.fillGradient(0, h - band, w, h, transparent, darkColor);
    }
}

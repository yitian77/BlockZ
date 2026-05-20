package com.yitianys.BlockZ.client.event;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.config.BlockZConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * DayZ 风格的冷蓝雾气氛围处理器。
 *
 * 设计原则：
 * - SRP：只负责雾气色调与浓度，不参与 HUD / 主菜单渲染。
 * - 仅在玩家未浸没（空气）且主维度地表时生效，避免污染水下/熔岩/下界/末地。
 * - 使用 {@link EventPriority#LOW} 以便在原版和其他模组基础上再叠加色调，
 *   保持与生物群系/时间/天气色的兼容，不会完全覆盖。
 */
@Mod.EventBusSubscriber(modid = BlockZ.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FogEffectHandler {

    // DayZ 冷灰绿基准色 (橄榄灰蓝，偏低饱和，更接近真实废土氛围而非纯蓝)
    private static final float TARGET_R = 0.46F;
    private static final float TARGET_G = 0.50F;
    private static final float TARGET_B = 0.52F;

    private FogEffectHandler() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!shouldApply(event.getCamera().getFluidInCamera())) {
            return;
        }

        float tintStrength = (float) BlockZConfigs.getBlueFogTintStrength();
        float desat = (float) BlockZConfigs.getWorldDesaturation();
        if (tintStrength <= 0.0F && desat <= 0.0F) {
            return;
        }

        float r = event.getRed();
        float g = event.getGreen();
        float b = event.getBlue();

        // 1) 先做去饱和：按 Rec.709 亮度系数算灰度，再把原色往灰度拉
        //    这一步模拟"世界褪色"，是废土感的核心
        if (desat > 0.0F) {
            float lum = 0.2126F * r + 0.7152F * g + 0.0722F * b;
            r = lerp(r, lum, desat);
            g = lerp(g, lum, desat);
            b = lerp(b, lum, desat);
        }

        // 2) 再叠加冷色基调
        if (tintStrength > 0.0F) {
            r = lerp(r, TARGET_R, tintStrength);
            g = lerp(g, TARGET_G, tintStrength);
            b = lerp(b, TARGET_B, tintStrength);
        }

        event.setRed(r);
        event.setGreen(g);
        event.setBlue(b);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!shouldApply(event.getCamera().getFluidInCamera())) {
            return;
        }

        float density = (float) BlockZConfigs.getBlueFogDensity();
        density = Math.max(0.2F, Math.min(1.0F, density));

        float baseStart = event.getNearPlaneDistance();
        float baseEnd = event.getFarPlaneDistance();
        float newFar = baseEnd * density;
        // 近平面同步轻微拉近，确保过渡自然
        float newNear = Math.min(event.getNearPlaneDistance(), newFar * 0.25F);

        event.setFarPlaneDistance(newFar);
        event.setNearPlaneDistance(newNear);
        event.setCanceled(true); // 必须 cancel 才能让修改生效 (Forge 规范)
    }

    /**
     * 判断当前场景是否应该应用蓝雾效果。
     * 仅在空气中（非水下/熔岩/细雪）且当前在主世界时生效，避免破坏其他维度氛围。
     */
    private static boolean shouldApply(FogType fluidInCamera) {
        if (fluidInCamera != FogType.NONE) {
            return false;
        }
        if (!BlockZConfigs.getEnableBlueFog()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return false;
        }
        // 只在主世界生效，下界/末地有自己特有的氛围，不应破坏
        return level.dimension() == net.minecraft.world.level.Level.OVERWORLD;
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}

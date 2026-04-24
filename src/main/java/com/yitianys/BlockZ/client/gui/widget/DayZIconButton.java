package com.yitianys.BlockZ.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

public class DayZIconButton extends Button {
    private final ResourceLocation texture;
    private final int textureWidth;
    private final int textureHeight;

    public DayZIconButton(int x, int y, int width, int height, ResourceLocation texture, OnPress onPress) {
        this(x, y, width, height, texture, onPress, null);
    }

    public DayZIconButton(int x, int y, int width, int height, ResourceLocation texture, OnPress onPress, Component tooltip) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.textureWidth = width;
        this.textureHeight = height;
        if (tooltip != null) {
            this.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tooltip));
        }
    }

    @Override
    public void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.isHoveredOrFocused()) {
            // 悬停时稍微变红或变亮 (DayZ 风格)
            RenderSystem.setShaderColor(1.0F, 0.2F, 0.2F, 1.0F);
        } else {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        RenderSystem.enableBlend();
        guiGraphics.blit(texture, this.getX(), this.getY(), 0, 0, this.width, this.height, textureWidth, textureHeight);
        
        // 重置颜色，避免影响后续渲染
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}

package com.yitianys.BlockZ.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;

public class DayZTextButton extends Button {
    private final int alignment; // 0: Left, 1: Center, 2: Right
    private final float fontSizeScale;
    private final int normalColor;
    private final int hoverColor;

    public DayZTextButton(int x, int y, int width, int height, Component message, OnPress onPress, int alignment, float fontSizeScale) {
        this(x, y, width, height, message, onPress, alignment, fontSizeScale, null);
    }

    public DayZTextButton(int x, int y, int width, int height, Component message, OnPress onPress, int alignment, float fontSizeScale, Component tooltip) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.alignment = alignment;
        this.fontSizeScale = fontSizeScale;
        this.normalColor = 0xFFCCCCCC; 
        this.hoverColor = 0xFFFFFFFF;
        if (tooltip != null) {
            this.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tooltip));
        }
    }

    @Override
    public void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        
        boolean hovered = this.isHoveredOrFocused();
        int color = hovered ? hoverColor : normalColor;
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.getX(), this.getY() + (this.height - 8 * fontSizeScale) / 2.0f, 0);
        guiGraphics.pose().scale(fontSizeScale, fontSizeScale, 1.0f);
        
        String text = this.getMessage().getString();
        if (text == null) text = "";
        int textWidth = (int)(font.width(text) * fontSizeScale);
        
        int drawX = 0;
        if (alignment == 1) { // Center
            drawX = (int)((this.width - textWidth) / (2.0f * fontSizeScale));
        } else if (alignment == 2) { // Right
            drawX = (int)((this.width - textWidth) / fontSizeScale);
        }
        
        guiGraphics.drawString(font, this.getMessage(), drawX, 0, color, true);
        
        // 如果悬停，画一条红色的下划线（DayZ 经典风格）
        if (hovered) {
            int lineY = (int)(9 * fontSizeScale);
            int lineXStart = drawX;
            int lineXEnd = drawX + (int)(font.width(text));
            guiGraphics.fill(lineXStart, lineY, lineXEnd, lineY + 1, 0xFFFF0000);
        }
        
        guiGraphics.pose().popPose();
    }
}

package com.yitianys.BlockZ.client.hud;

import com.yitianys.BlockZ.client.ClientSettings;
import com.yitianys.BlockZ.client.gui.UITextures;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class DayZHudOverlay {
    public static final IGuiOverlay OVERLAY = (gui, guiGraphics, partialTick, width, height) -> {
        if (!ClientSettings.dayzEnabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        // 如果打开了非聊天界面，则不渲染 HUD (防止重叠)
        if (!(mc.screen == null || mc.screen instanceof ChatScreen)) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // 1. Top Left: Mod Info (更简洁)
        String modInfo = "BlockZ";
        guiGraphics.drawString(mc.font, modInfo, 8, 8, 0x60FFFFFF, true);

        // 2. Bottom Center: Hotbar
        int hotbarSlots = 9;
        int slotSize = 18; // 稍微缩小一点
        int gap = 2;       // 槽位间距
        int totalWidth = hotbarSlots * (slotSize + gap) - gap;
        int startX = (width - totalWidth) / 2;
        int hotbarY = height - 20;

        int selected = mc.player.getInventory().selected;

        for (int i = 0; i < hotbarSlots; i++) {
            int x = startX + i * (slotSize + gap);
            
            // 绘制槽位背景 (半透明黑)
            guiGraphics.fill(x, hotbarY, x + slotSize, hotbarY + slotSize, 0x40000000);
            
            // 如果是选中的槽位，绘制一个高亮边框
            if (i == selected) {
                // 绘制外边框
                guiGraphics.renderOutline(x - 1, hotbarY - 1, slotSize + 2, slotSize + 2, 0x80FFFFFF);
                // 内部稍微提亮
                guiGraphics.fill(x, hotbarY, x + slotSize, hotbarY + slotSize, 0x20FFFFFF);
            } else {
                // 未选中槽位的细微边框
                guiGraphics.renderOutline(x, hotbarY, slotSize, slotSize, 0x20FFFFFF);
            }
            
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                // 物品在槽位中居中渲染
                guiGraphics.renderItem(stack, x + 1, hotbarY + 1);
                guiGraphics.renderItemDecorations(mc.font, stack, x + 1, hotbarY + 1);
            }
        }

        // 3. Bottom Right: DayZ Style HUD
        int iconSize = 18; // 缩小图标尺寸
        int spacing = 22;  // 缩小间距
        int hudX = width - 25; 
        int hudY = height - 22; 

        // Health
        float healthRatio = mc.player.getHealth() / mc.player.getMaxHealth();
        drawDayZIcon(guiGraphics, hudX, hudY, UITextures.HUD_HEALTH_OUTLINE, UITextures.HUD_HEALTH_VALUE, healthRatio, iconSize);

        // Hunger
        float foodRatio = mc.player.getFoodData().getFoodLevel() / 20.0f;
        drawDayZIcon(guiGraphics, hudX - spacing, hudY, UITextures.HUD_HUNGER_OUTLINE, UITextures.HUD_HUNGER_VALUE, foodRatio, iconSize);

        // Thirst (暂时 100%)
        drawDayZIcon(guiGraphics, hudX - spacing * 2, hudY, UITextures.HUD_THIRST_OUTLINE, UITextures.HUD_THIRST_VALUE, 1.0f, iconSize);

        // Armor
        float armorValue = mc.player.getArmorValue();
        if (armorValue > 0) {
            drawDayZIcon(guiGraphics, hudX - spacing * 3, hudY, UITextures.HUD_ARMOR_OUTLINE, UITextures.HUD_ARMOR_VALUE, Math.min(1.0f, armorValue / 20.0f), iconSize);
        }

        // 4. Held Item Info (移到快捷栏上方)
        ItemStack held = mc.player.getMainHandItem();
        if (!held.isEmpty()) {
            String itemName = held.getHoverName().getString();
            int nameWidth = mc.font.width(itemName);
            int centerX = width / 2;
            guiGraphics.drawString(mc.font, itemName, centerX - nameWidth / 2, height - 40, 0xFFFFFFFF, true);
        }

        RenderSystem.disableBlend();
    };

    private static void drawDayZIcon(GuiGraphics graphics, int x, int y, ResourceLocation outline, ResourceLocation value, float ratio, int size) {
        // 1. 先绘制填充 (Value)
        if (ratio > 0) {
            float clampedRatio = Math.max(0, Math.min(1, ratio));
            int displayHeight = (int)(size * clampedRatio);
            int screenYOffset = size - displayHeight;
            
            int texHeight = (int)(300 * clampedRatio);
            int texVOffset = 300 - texHeight;
            
            graphics.blit(value, x, y + screenYOffset, size, displayHeight, 0.0f, (float)texVOffset, 300, texHeight, 300, 300);
        }

        // 2. 后绘制外框 (Outline)
        graphics.blit(outline, x, y, size, size, 0.0f, 0.0f, 300, 300, 300, 300);
    }
}

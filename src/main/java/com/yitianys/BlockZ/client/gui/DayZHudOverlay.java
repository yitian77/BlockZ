package com.yitianys.BlockZ.client.gui;

import com.yitianys.BlockZ.client.ClientSettings;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.init.ModEffects;
import com.yitianys.BlockZ.init.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.yitianys.BlockZ.item.ClothingItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.common.capabilities.Capability;

public class DayZHudOverlay {
    private static final ResourceLocation BLOOD_OVERLAY_1 = new ResourceLocation("blockz", "textures/gui/visual/blood.png");
    private static final ResourceLocation BLOOD_OVERLAY_2 = new ResourceLocation("blockz", "textures/gui/visual/blood_2.png");
    private static Capability<?> CACHED_THIRST_CAP = null;
    private static boolean reflectionAttempted = false;

    private static Capability<?> getThirstCapability() {
        if (CACHED_THIRST_CAP == null && !reflectionAttempted) {
            reflectionAttempted = true;
            try {
                Class<?> clazz = Class.forName("dev.ghen.thirst.foundation.common.capability.ModCapabilities");
                java.lang.reflect.Field field = clazz.getField("PLAYER_THIRST");
                CACHED_THIRST_CAP = (Capability<?>) field.get(null);
            } catch (Exception e) {
                // Ignore or log if needed
            }
        }
        return CACHED_THIRST_CAP;
    }
    public static final IGuiOverlay HUD_OVERLAY = (gui, guiGraphics, partialTick, width, height) -> {
        if (!ClientSettings.dayzEnabled || !BlockZConfigs.showDayzHud.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        var player = mc.player;
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

        int selected = player.getInventory().selected;

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
            
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                // 物品在槽位中居中渲染
                guiGraphics.renderItem(stack, x + 1, hotbarY + 1);
                guiGraphics.renderItemDecorations(mc.font, stack, x + 1, hotbarY + 1);
            }
        }

        // 3. Bottom Right: DayZ Style HUD
        // 进一步缩小图标尺寸并调整位置，使其更加精简 (参考原版 DayM)
        // User requested even smaller size (0.25x). Previous was 10. New size 8 (approx 0.5x of original 16).
        // Update: User requested "Larger by 0.5x" (1.5x of previous?). 
        // Let's bump it up to 12 (was 8, 8*1.5 = 12).
        int iconSize = 12; 
        int spacing = 15; 
        int statsHudX = width - (spacing * 3 + iconSize) - 4;
        
        // 1. Align with Hotbar Y or slightly lower
        // User requested "further down". Hotbar is at height - 20.
        // Let's align roughly with the bottom area.
        int hudY = height - 16; 

        boolean nursingEnabled = BlockZConfigs.enableNursingSystem.get();
        boolean wounded = nursingEnabled && player.hasEffect(ModEffects.BLEEDING.get());
        boolean fractured = nursingEnabled && player.hasEffect(ModEffects.FRACTURE.get());
        boolean medicated = nursingEnabled && player.hasEffect(ModEffects.ANALGESIC.get());

        int activeInjuryIcons = 0;
        if (wounded) {
            activeInjuryIcons++;
        }
        if (fractured) {
            activeInjuryIcons++;
        }
        if (medicated) {
            activeInjuryIcons++;
        }

        int hudX = statsHudX - spacing * activeInjuryIcons;
        int injuryIndex = 0;

        if (wounded) {
            drawSimpleIcon(guiGraphics, hudX + spacing * injuryIndex, hudY, UITextures.HUD_WOUND, iconSize);
            injuryIndex++;
        }
        if (fractured) {
            drawSimpleIcon(guiGraphics, hudX + spacing * injuryIndex, hudY, UITextures.HUD_FRACTURES, iconSize);
            injuryIndex++;
        }
        if (medicated) {
            drawSimpleIcon(guiGraphics, hudX + spacing * injuryIndex, hudY, UITextures.HUD_MEDICINE, iconSize);
            injuryIndex++;
        }

        // Health
        float healthRatio = player.getHealth() / player.getMaxHealth();
        drawDayZIcon(guiGraphics, hudX + spacing * injuryIndex, hudY, UITextures.HUD_HEALTH_OUTLINE, UITextures.HUD_HEALTH_VALUE, healthRatio, iconSize);

        // Hunger
        float foodRatio = player.getFoodData().getFoodLevel() / 20.0f;
        drawDayZIcon(guiGraphics, hudX + spacing * (injuryIndex + 1), hudY, UITextures.HUD_HUNGER_OUTLINE, UITextures.HUD_HUNGER_VALUE, foodRatio, iconSize);

        // Thirst
        // 获取 Thirst Mod 的饮水值
        float thirstRatioValue = 1.0f;
        Capability<?> thirstCap = getThirstCapability();
        if (thirstCap != null) {
            java.util.concurrent.atomic.AtomicReference<Float> ratioRef = new java.util.concurrent.atomic.AtomicReference<>(1.0f);
            player.getCapability((Capability) thirstCap).ifPresent(cap -> {
                try {
                    java.lang.reflect.Method getThirst = cap.getClass().getMethod("getThirst");
                    Object valObj = getThirst.invoke(cap);
                    if (valObj instanceof Number) {
                        float val = ((Number) valObj).floatValue();
                        ratioRef.set(val / 20.0f);
                    }
                } catch (Exception e) {
                    // Ignore
                }
            });
            thirstRatioValue = ratioRef.get();
        }
        drawDayZIcon(guiGraphics, hudX + spacing * (injuryIndex + 2), hudY, UITextures.HUD_THIRST_OUTLINE, UITextures.HUD_THIRST_VALUE, thirstRatioValue, iconSize);

        // Armor
        float armorValue = player.getArmorValue();
        drawDayZIcon(guiGraphics, hudX + spacing * (injuryIndex + 3), hudY, UITextures.HUD_ARMOR_OUTLINE, UITextures.HUD_ARMOR_VALUE, armorValue / 20.0f, iconSize);

        ItemStack usingItem = player.getUseItem();
        if (nursingEnabled && !usingItem.isEmpty() && player.isUsingItem()) {
            boolean isNursingUse = usingItem.is(ModItems.SPLINT.get())
                    || usingItem.is(ModItems.BANDAGE.get())
                    || usingItem.is(ModItems.RAGS.get())
                    || usingItem.is(ModItems.MORPHINE_SYRINGE.get())
                    || usingItem.is(ModItems.CODEINE_PILLS.get())
                    || (usingItem.getItem() instanceof ClothingItem clothing && (clothing.getType() == ClothingItem.ClothingType.SHIRT
                        || clothing.getType() == ClothingItem.ClothingType.PANTS
                        || clothing.getType() == ClothingItem.ClothingType.VEST));

            if (isNursingUse) {
                int maxUse = usingItem.getUseDuration();
                int remainingUse = player.getUseItemRemainingTicks();
                float progress = maxUse <= 0 ? 0.0F : 1.0F - (remainingUse / (float) maxUse);
                drawProgressBar(guiGraphics, width / 2 - 40, height - 52, 80, 6, progress, 0xCCF0E7C0, 0xCC201A12);
            }
        }

        if (player.hasEffect(ModEffects.BLEEDING.get())) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.78F);
            guiGraphics.blit(BLOOD_OVERLAY_1, 0, 0, 0, 0, width, height, width, height);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.45F);
            guiGraphics.blit(BLOOD_OVERLAY_2, 0, 0, 0, 0, width, height, width, height);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        float remainingFlashTicks = 0.0F;
        long flashStart = ClientSettings.lastFractureFlashGameTime;
        if (mc.level != null && flashStart >= 0) {
            long elapsed = mc.level.getGameTime() - flashStart;
            remainingFlashTicks = ClientSettings.FRACTURE_FLASH_DURATION_TICKS - elapsed;
            if (remainingFlashTicks <= 0) {
                ClientSettings.lastFractureFlashGameTime = -1L;
                ClientSettings.fractureFlashTicks = 0;
                remainingFlashTicks = 0.0F;
            }
        } else if (ClientSettings.fractureFlashTicks > 0) {
            remainingFlashTicks = ClientSettings.fractureFlashTicks;
            ClientSettings.fractureFlashTicks = Math.max(0, ClientSettings.fractureFlashTicks - 1);
            if (ClientSettings.fractureFlashTicks == 0) {
                ClientSettings.lastFractureFlashGameTime = -1L;
            }
        }

        if (remainingFlashTicks > 0) {
            float flashRatio = remainingFlashTicks / ClientSettings.FRACTURE_FLASH_DURATION_TICKS;
            int alpha = Math.max(0, Math.min(180, (int) (flashRatio * 180.0F)));
            guiGraphics.fill(0, 0, width, height, (alpha << 24));
        }

        // 4. Held Item Info (移到快捷栏上方)
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty()) {
            String itemName = held.getHoverName().getString();
            int nameWidth = mc.font.width(itemName);
            int centerX = width / 2;
            guiGraphics.drawString(mc.font, itemName, centerX - nameWidth / 2, height - 40, 0xFFFFFFFF, true);
        }

        RenderSystem.disableBlend();
    };

    private static void drawDayZIcon(GuiGraphics graphics, int x, int y, ResourceLocation outline, ResourceLocation value, float ratio, int size) {
        // 默认为 256x256，如果贴图比例不对，可能需要调整此值
        int texSize = 256; 

        if (ratio > 0) {
            float clampedRatio = Math.max(0, Math.min(1, ratio));
            int displayHeight = (int)(size * clampedRatio);
            int screenYOffset = size - displayHeight;
            
            int texHeight = (int)(texSize * clampedRatio);
            int texVOffset = texSize - texHeight;

            // 使用带拉伸/缩放的 blit：将 texSize x texHeight 的贴图区域，渲染到 size x displayHeight 的屏幕区域
            graphics.blit(value, x, y + screenYOffset, size, displayHeight, 0, (float)texVOffset, texSize, texHeight, texSize, texSize);
        }

        // 绘制外框，同样使用缩放 blit
        graphics.blit(outline, x, y, size, size, 0, 0, texSize, texSize, texSize, texSize);
    }

    private static void drawSimpleIcon(GuiGraphics graphics, int x, int y, ResourceLocation texture, int size) {
        int texSize = 256;
        graphics.blit(texture, x, y, size, size, 0, 0, texSize, texSize, texSize, texSize);
    }

    private static void drawProgressBar(GuiGraphics graphics, int x, int y, int width, int height, float progress, int fillColor, int backgroundColor) {
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        graphics.fill(x, y, x + width, y + height, backgroundColor);
        graphics.fill(x + 1, y + 1, x + 1 + (int) ((width - 2) * clamped), y + height - 1, fillColor);
        graphics.renderOutline(x, y, width, height, 0x80FFFFFF);
    }
}

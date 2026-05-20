package com.yitianys.BlockZ.client.gui;

import com.yitianys.BlockZ.client.ClientSettings;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.init.ModEffects;
import com.yitianys.BlockZ.init.ModItems;
import com.yitianys.BlockZ.util.ProneManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.yitianys.BlockZ.item.ClothingItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.common.capabilities.Capability;

import java.util.concurrent.atomic.AtomicReference;

public class DayZHudOverlay {
    private static final ResourceLocation BLOOD_OVERLAY_1 = new ResourceLocation("blockz", "textures/gui/visual/blood.png");
    private static final ResourceLocation BLOOD_OVERLAY_2 = new ResourceLocation("blockz", "textures/gui/visual/blood_2.png");
    private static final int STAMINA_ICON_TEX_WIDTH = 300;
    private static final int STAMINA_ICON_TEX_HEIGHT = 300;
    private static final int STAMINA_ICON_U = 23;
    private static final int STAMINA_ICON_V = 51;
    private static final int STAMINA_ICON_W = 237;
    private static final int STAMINA_ICON_H = 224;
    private static final int STAMINA_BAR_TEX_WIDTH = 1024;
    private static final int STAMINA_BAR_TEX_HEIGHT = 146;
    private static final int STAMINA_BAR_OUTLINE_U = 15;
    private static final int STAMINA_BAR_OUTLINE_V = 23;
    private static final int STAMINA_BAR_OUTLINE_W = 992;
    private static final int STAMINA_BAR_OUTLINE_H = 80;
    private static final int STAMINA_BAR_FILL_U = 15;
    private static final int STAMINA_BAR_FILL_V = 23;
    private static final int STAMINA_BAR_FILL_W = 991;
    private static final int STAMINA_BAR_FILL_H = 80;
    private static final int TREND_SAMPLE_TICKS = 20;
    private static final int TREND_DISPLAY_TICKS = 100;
    private static final float TREND_EPSILON = 0.0025F;
    private static final float TREND_MEDIUM = 0.0125F;
    private static final float TREND_STRONG = 0.03F;
    private static long lastTrendSampleGameTime = -1L;
    private static float lastFoodRatio = -1.0F;
    private static float lastThirstRatio = -1.0F;
    private static float lastHealthRatio = -1.0F;
    private static float lastHealthPointsRatio = -1.0F;
    private static int foodTrendDir = 0;
    private static int foodTrendLevel = 0;
    private static long foodTrendExpire = -1L;
    private static int thirstTrendDir = 0;
    private static int thirstTrendLevel = 0;
    private static long thirstTrendExpire = -1L;
    private static int healthTrendDir = 0;
    private static int healthTrendLevel = 0;
    private static long healthTrendExpire = -1L;
    private static int healthPointsTrendDir = 0;
    private static int healthPointsTrendLevel = 0;
    private static long healthPointsTrendExpire = -1L;
    private static Capability<?> CACHED_THIRST_CAP = null;
    private static boolean reflectionAttempted = false;

    private static Capability<?> getThirstCapability() {
        if (CACHED_THIRST_CAP == null && !reflectionAttempted) {
            reflectionAttempted = true;
            try {
                Class<?> clazz = Class.forName("dev.ghen.thirst.foundation.common.capability.ModCapabilities");
                java.lang.reflect.Field field = clazz.getField("PLAYER_THIRST");
                CACHED_THIRST_CAP = (Capability<?>) field.get(null);
            } catch (Exception ignored) {
            }
        }
        return CACHED_THIRST_CAP;
    }

    public static final IGuiOverlay HUD_OVERLAY = (gui, guiGraphics, partialTick, width, height) -> {
        if (!BlockZConfigs.getShowDayzHud() || !ClientSettings.dayzHudEnabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        var player = mc.player;
        CameraType cameraType = mc.options.getCameraType();
        boolean showThirdPersonDot = cameraType == CameraType.THIRD_PERSON_BACK;
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
        int iconSize = 10;
        int legacyIconSize = 10;
        int injuryIconSize = 12;
        int injurySpacing = 13;
        int metricSpacing = 2;
        int legacySpacing = 2;
        int staminaWidth = 64;
        int staminaHeight = 8;
        int infectionSize = 10;
        int noiseSize = 9;
        int leftHudY = hotbarY + 6;
        int rightHudY = hotbarY + 3;

        boolean nursingEnabled = BlockZConfigs.isNursingEnabled();
        boolean wounded = nursingEnabled && player.hasEffect(ModEffects.BLEEDING.get());
        boolean fractured = nursingEnabled && player.hasEffect(ModEffects.FRACTURE.get());
        boolean medicated = nursingEnabled && player.hasEffect(ModEffects.ANALGESIC.get());
        boolean healthSystemEnabled = BlockZConfigs.isHealthSystemEnabled();
        float staminaRatio = ClientSettings.staminaRatio;
        float infectionRatio = ClientSettings.infectionRatio;
        boolean showInfection = infectionRatio > 0.01F;
        float foodRatio = player.getFoodData().getFoodLevel() / 20.0f;
        float armorRatio = player.getArmorValue() / 20.0f;
        float thirstRatioValue = getThirstRatio(player);
        float healthRatioValue = ClientSettings.healthRatio;
        float healthPointsRatioValue = ClientSettings.healthPointsRatio > 0.0F ? ClientSettings.healthPointsRatio : player.getHealth() / Math.max(1.0F, player.getMaxHealth());
        long gameTime = mc.level != null ? mc.level.getGameTime() : 0L;
        updateTrends(gameTime, foodRatio, thirstRatioValue, healthRatioValue, healthPointsRatioValue);

        int activeInjuryIcons = 0;
        if (showInfection) {
            activeInjuryIcons++;
        }
        if (wounded) {
            activeInjuryIcons++;
        }
        if (fractured) {
            activeInjuryIcons++;
        }
        if (medicated) {
            activeInjuryIcons++;
        }

        int leftStaminaX = Math.max(4, startX - staminaWidth - 48);
        int staminaIconHeight = staminaHeight;
        int staminaIconWidth = Math.max(1, Math.round(staminaIconHeight * (STAMINA_ICON_W / (float) STAMINA_ICON_H)));
        ResourceLocation staminaIconTexture = ProneManager.isProne(player)
                ? UITextures.HUD_STANCE_PRONE
                : (player.isCrouching() ? UITextures.HUD_SQUAT : UITextures.HUD_STAMINA_ICON);
        drawSimpleIcon(guiGraphics, leftStaminaX, leftHudY, staminaIconTexture, staminaIconWidth, 1.0F, STAMINA_ICON_TEX_WIDTH);
        if (BlockZConfigs.isStaminaEnabled()) {
            drawHorizontalBar(
                    guiGraphics,
                    leftStaminaX,
                    leftHudY,
                    UITextures.HUD_STAMINA_ICON,
                    UITextures.HUD_STAMINA_OUTLINE,
                    UITextures.HUD_STAMINA_VALUE,
                    staminaRatio,
                    staminaWidth,
                    staminaHeight,
                    false
            );
        }

        int noiseLevel = 0;
        if (player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D) {
            if (player.isCrouching()) {
                noiseLevel = 1;
            } else if (player.isSprinting()) {
                noiseLevel = 3;
            } else {
                noiseLevel = 2;
            }
        }
        if (noiseLevel > 0) {
            ResourceLocation noiseTex = UITextures.HUD_NOISE_1;
            switch (noiseLevel) {
                case 2: noiseTex = UITextures.HUD_NOISE_2; break;
                case 3: noiseTex = UITextures.HUD_NOISE_3; break;
            }
            drawSimpleIcon(guiGraphics, leftStaminaX - noiseSize - 1, leftHudY + (staminaHeight - noiseSize) / 2, noiseTex, noiseSize, 0.8F, 300);
        }

        int rightPadding = 3;
        int currentRightX = width - rightPadding;

        if (healthSystemEnabled) {
            int healthX = currentRightX - iconSize;
            drawTieredIcon(
                    guiGraphics,
                    healthX,
                    rightHudY,
                    healthRatioValue,
                    UITextures.HUD_HEALTH_OUTLINE,
                    UITextures.HUD_HEALTH_OUTLINE_DECREASED,
                    UITextures.HUD_HEALTH_OUTLINE_CRITICAL,
                    UITextures.HUD_HEALTH_VALUE,
                    UITextures.HUD_HEALTH_VALUE_DECREASED,
                    UITextures.HUD_HEALTH_VALUE_CRITICAL,
                    iconSize
            );
            drawTrendIcon(guiGraphics, healthX, rightHudY, iconSize, healthTrendDir, healthTrendLevel, healthTrendExpire, gameTime);
            currentRightX = healthX - metricSpacing;

            int healthPointsX = currentRightX - iconSize;
            drawTieredIcon(
                    guiGraphics,
                    healthPointsX,
                    rightHudY,
                    healthPointsRatioValue,
                    UITextures.HUD_HEALTH_POINTS_OUTLINE,
                    UITextures.HUD_HEALTH_POINTS_OUTLINE_DECREASED,
                    UITextures.HUD_HEALTH_POINTS_OUTLINE_CRITICAL,
                    UITextures.HUD_HEALTH_POINTS_VALUE,
                    UITextures.HUD_HEALTH_POINTS_VALUE_DECREASED,
                    UITextures.HUD_HEALTH_POINTS_VALUE_CRITICAL,
                    iconSize
            );
            drawTrendIcon(guiGraphics, healthPointsX, rightHudY, iconSize, healthPointsTrendDir, healthPointsTrendLevel, healthPointsTrendExpire, gameTime);
            currentRightX = healthPointsX - metricSpacing;
        }

        int armorX = currentRightX - legacyIconSize;
        drawVerticalFillIcon(guiGraphics, armorX, rightHudY, UITextures.HUD_ARMOR_OUTLINE, UITextures.HUD_ARMOR_VALUE, armorRatio, legacyIconSize, 256);
        currentRightX = armorX - legacySpacing;

        int hungerX = currentRightX - legacyIconSize;
        drawTieredHungerIcon(
                guiGraphics,
                hungerX,
                rightHudY,
                foodRatio,
                UITextures.HUD_HUNGER_OUTLINE,
                UITextures.HUD_HUNGER_OUTLINE_DECREASED,
                UITextures.HUD_HUNGER_OUTLINE_CRITICAL,
                UITextures.HUD_HUNGER_VALUE,
                UITextures.HUD_HUNGER_VALUE_DECREASED,
                UITextures.HUD_HUNGER_VALUE_CRITICAL,
                legacyIconSize
        );
        drawTrendIcon(guiGraphics, hungerX, rightHudY, legacyIconSize, foodTrendDir, foodTrendLevel, foodTrendExpire, gameTime);
        currentRightX = hungerX - legacySpacing;

        int thirstX = currentRightX - legacyIconSize;
        drawTieredIcon(
                guiGraphics,
                thirstX,
                rightHudY,
                thirstRatioValue,
                UITextures.HUD_THIRST_OUTLINE,
                UITextures.HUD_THIRST_OUTLINE_DECREASED,
                UITextures.HUD_THIRST_OUTLINE_CRITICAL,
                UITextures.HUD_THIRST_VALUE,
                UITextures.HUD_THIRST_VALUE_DECREASED,
                UITextures.HUD_THIRST_VALUE_CRITICAL,
                legacyIconSize
        );
        drawTrendIcon(guiGraphics, thirstX, rightHudY, legacyIconSize, thirstTrendDir, thirstTrendLevel, thirstTrendExpire, gameTime);
        currentRightX = thirstX;

        int buffStartX = currentRightX - (activeInjuryIcons > 0 ? injurySpacing * activeInjuryIcons : 0);
        int injuryIndex = 0;
        if (showInfection) {
            drawSimpleIcon(guiGraphics, buffStartX + injurySpacing * injuryIndex, rightHudY, UITextures.HUD_INFECTION, infectionSize, 1.0F, 300);
            injuryIndex++;
        }
        if (wounded) {
            drawSimpleIcon(guiGraphics, buffStartX + injurySpacing * injuryIndex, rightHudY - 1, UITextures.HUD_WOUND, injuryIconSize, 1.0F, 256);
            injuryIndex++;
        }
        if (fractured) {
            drawSimpleIcon(guiGraphics, buffStartX + injurySpacing * injuryIndex, rightHudY - 1, UITextures.HUD_FRACTURES, injuryIconSize, 1.0F, 256);
            injuryIndex++;
        }
        if (medicated) {
            drawSimpleIcon(guiGraphics, buffStartX + injurySpacing * injuryIndex, rightHudY - 1, UITextures.HUD_MEDICINE, injuryIconSize, 1.0F, 256);
        }

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

        if (showThirdPersonDot) {
            drawThirdPersonCrosshairDot(guiGraphics, width / 2, height / 2);
        }

        RenderSystem.disableBlend();
    };

    private static void drawTieredIcon(GuiGraphics graphics, int x, int y, float ratio,
                                       ResourceLocation outlineNormal, ResourceLocation outlineDecreased, ResourceLocation outlineCritical,
                                       ResourceLocation valueNormal, ResourceLocation valueDecreased, ResourceLocation valueCritical,
                                       int size) {
        drawTieredIcon(graphics, x, y, ratio, outlineNormal, outlineDecreased, outlineCritical, valueNormal, valueDecreased, valueCritical, size, 1.0F);
    }

    private static void drawTieredHungerIcon(GuiGraphics graphics, int x, int y, float ratio,
                                             ResourceLocation outlineNormal, ResourceLocation outlineDecreased, ResourceLocation outlineCritical,
                                             ResourceLocation valueNormal, ResourceLocation valueDecreased, ResourceLocation valueCritical,
                                             int size) {
        ResourceLocation outline = outlineNormal;
        ResourceLocation value = valueNormal;
        if (ratio <= 0.25F) {
            outline = outlineCritical;
            value = valueCritical;
        } else if (ratio <= 0.55F) {
            outline = outlineDecreased;
            value = valueDecreased;
        }
        drawHungerFillIcon(graphics, x, y, outline, value, ratio, size, 300);
    }

    private static void drawTieredIcon(GuiGraphics graphics, int x, int y, float ratio,
                                       ResourceLocation outlineNormal, ResourceLocation outlineDecreased, ResourceLocation outlineCritical,
                                       ResourceLocation valueNormal, ResourceLocation valueDecreased, ResourceLocation valueCritical,
                                       int size, float fillMultiplier) {
        ResourceLocation outline = outlineNormal;
        ResourceLocation value = valueNormal;
        if (ratio <= 0.25F) {
            outline = outlineCritical;
            value = valueCritical;
        } else if (ratio <= 0.55F) {
            outline = outlineDecreased;
            value = valueDecreased;
        }
        drawVerticalFillIcon(graphics, x, y, outline, value, ratio, size, 300, fillMultiplier);
    }

    private static void updateTrends(long gameTime, float foodRatio, float thirstRatio, float healthRatio, float healthPointsRatio) {
        if (gameTime < 0L) {
            return;
        }
        if (lastTrendSampleGameTime < 0L || gameTime < lastTrendSampleGameTime) {
            lastTrendSampleGameTime = gameTime;
            lastFoodRatio = foodRatio;
            lastThirstRatio = thirstRatio;
            lastHealthRatio = healthRatio;
            lastHealthPointsRatio = healthPointsRatio;
            return;
        }
        if (gameTime - lastTrendSampleGameTime < TREND_SAMPLE_TICKS) {
            return;
        }
        lastTrendSampleGameTime = gameTime;
        updateTrendState(0, foodRatio - lastFoodRatio, gameTime);
        updateTrendState(1, thirstRatio - lastThirstRatio, gameTime);
        updateTrendState(2, healthRatio - lastHealthRatio, gameTime);
        updateTrendState(3, healthPointsRatio - lastHealthPointsRatio, gameTime);
        lastFoodRatio = foodRatio;
        lastThirstRatio = thirstRatio;
        lastHealthRatio = healthRatio;
        lastHealthPointsRatio = healthPointsRatio;
    }

    private static void updateTrendState(int type, float delta, long gameTime) {
        float magnitude = Math.abs(delta);
        int dir = 0;
        int level = 0;
        if (magnitude >= TREND_EPSILON) {
            dir = delta > 0.0F ? 1 : -1;
            if (magnitude >= TREND_STRONG) {
                level = 3;
            } else if (magnitude >= TREND_MEDIUM) {
                level = 2;
            } else {
                level = 1;
            }
        }
        if (type == 0) {
            foodTrendDir = dir;
            foodTrendLevel = level;
            foodTrendExpire = level > 0 ? gameTime + TREND_DISPLAY_TICKS : -1L;
        } else if (type == 1) {
            thirstTrendDir = dir;
            thirstTrendLevel = level;
            thirstTrendExpire = level > 0 ? gameTime + TREND_DISPLAY_TICKS : -1L;
        } else if (type == 2) {
            healthTrendDir = dir;
            healthTrendLevel = level;
            healthTrendExpire = level > 0 ? gameTime + TREND_DISPLAY_TICKS : -1L;
        } else if (type == 3) {
            healthPointsTrendDir = dir;
            healthPointsTrendLevel = level;
            healthPointsTrendExpire = level > 0 ? gameTime + TREND_DISPLAY_TICKS : -1L;
        }
    }

    private static void drawTrendIcon(GuiGraphics graphics, int x, int y, int size, int trendDir, int trendLevel, long expireTime, long gameTime) {
        if (trendDir == 0 || trendLevel <= 0 || expireTime < 0L || gameTime > expireTime) {
            return;
        }
        ResourceLocation texture;
        if (trendDir > 0) {
            texture = trendLevel >= 3 ? UITextures.HUD_TREND_UP_3 : (trendLevel == 2 ? UITextures.HUD_TREND_UP_2 : UITextures.HUD_TREND_UP_1);
        } else {
            texture = trendLevel >= 3 ? UITextures.HUD_TREND_DOWN_3 : (trendLevel == 2 ? UITextures.HUD_TREND_DOWN_2 : UITextures.HUD_TREND_DOWN_1);
        }
        int trendSize = Math.max(8, Math.round(size * 0.9F));
        int trendX = x + (size - trendSize) / 2;
        if (trendDir < 0) {
            trendX += 1;
        }

        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int trendY = trendDir > 0 ? y - trendSize - 1 : y + size + 1;
        trendX = Math.max(0, Math.min(trendX, screenWidth - trendSize));
        trendY = Math.max(0, Math.min(trendY, screenHeight - trendSize));
        drawSimpleIcon(graphics, trendX, trendY, texture, trendSize, 1.0F, 300);
    }

    private static void drawVerticalFillIcon(GuiGraphics graphics, int x, int y, ResourceLocation outline, ResourceLocation value, float ratio, int size, int texSize) {
        drawVerticalFillIcon(graphics, x, y, outline, value, ratio, size, texSize, 1.0F);
    }

    private static void drawVerticalFillIcon(GuiGraphics graphics, int x, int y, ResourceLocation outline, ResourceLocation value, float ratio, int size, int texSize, float fillMultiplier) {
        float clampedRatio = Math.max(0.0F, Math.min(1.0F, ratio));
        if (clampedRatio > 0.0F) {
            Minecraft mc = Minecraft.getInstance();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            double scale = mc.getWindow().getGuiScale();
            
            // 对图标使用非线性裁剪，避免线性双倍裁剪在半格时直接见底
            float adjustedRatio = (float) Math.pow(clampedRatio, Math.max(0.0F, fillMultiplier));
            double rawHeight = size * Math.max(0.0F, Math.min(1.0F, adjustedRatio));
            
            int scissorX = (int) (x * scale);
            int scissorY = (int) ((screenHeight - y - size) * scale);
            int scissorW = (int) (size * scale);
            int scissorH = (int) (rawHeight * scale);
            
            RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
            graphics.blit(value, x, y, size, size, 0, 0, texSize, texSize, texSize, texSize);
            RenderSystem.disableScissor();
        }
        graphics.blit(outline, x, y, size, size, 0, 0, texSize, texSize, texSize, texSize);
    }

    private static void drawHungerFillIcon(GuiGraphics graphics, int x, int y, ResourceLocation outline, ResourceLocation value, float ratio, int size, int texSize) {
        float clampedRatio = Math.max(0.0F, Math.min(1.0F, ratio));
        if (clampedRatio > 0.0F) {
            float visualRatio = (float) Math.pow(clampedRatio, 1.5D);

            Minecraft mc = Minecraft.getInstance();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            double scale = mc.getWindow().getGuiScale();

            int contentTop = Math.round(size * 0.12F);
            int contentBottom = Math.round(size * 0.98F);
            int contentHeight = Math.max(1, contentBottom - contentTop);
            int visibleHeight = Math.max(0, Math.round(contentHeight * visualRatio));
            int fillTop = y + contentBottom - visibleHeight;

            int scissorX = (int) (x * scale);
            int scissorY = (int) ((screenHeight - (fillTop + visibleHeight)) * scale);
            int scissorW = (int) Math.ceil(size * scale);
            int scissorH = (int) Math.ceil(visibleHeight * scale);

            if (scissorW > 0 && scissorH > 0) {
                RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
                graphics.blit(value, x, y, size, size, 0, 0, texSize, texSize, texSize, texSize);
                RenderSystem.disableScissor();
            }
        }
        graphics.blit(outline, x, y, size, size, 0, 0, texSize, texSize, texSize, texSize);
    }

    private static void drawHorizontalBar(GuiGraphics graphics, int x, int y, ResourceLocation icon, ResourceLocation outline, ResourceLocation value, float ratio, int width, int height, boolean drawIcon) {
        float clampedRatio = Math.max(0.0F, Math.min(1.0F, ratio));
        int iconHeight = height;
        int iconWidth = Math.max(1, Math.round(iconHeight * (STAMINA_ICON_W / (float) STAMINA_ICON_H)));
        int barGap = 3;
        int barWidth = Math.max(16, width - iconWidth - barGap);
        int barHeight = Math.max(4, Math.min(height - 2, Math.round(barWidth * (STAMINA_BAR_OUTLINE_H / (float) STAMINA_BAR_OUTLINE_W))));
        int barX = x + iconWidth + barGap;
        int barY = y + (iconHeight - barHeight) / 2;

        if (drawIcon) {
            graphics.blit(icon, x, y, iconWidth, iconHeight, STAMINA_ICON_U, STAMINA_ICON_V, STAMINA_ICON_W, STAMINA_ICON_H, STAMINA_ICON_TEX_WIDTH, STAMINA_ICON_TEX_HEIGHT);
        }
        
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
        graphics.blit(outline, barX, barY, barWidth, barHeight, STAMINA_BAR_OUTLINE_U, STAMINA_BAR_OUTLINE_V, STAMINA_BAR_OUTLINE_W, STAMINA_BAR_OUTLINE_H, STAMINA_BAR_TEX_WIDTH, STAMINA_BAR_TEX_HEIGHT);
        int fillWidth = Math.round(barWidth * clampedRatio);
        if (fillWidth > 0) {
            int texFillWidth = Math.max(1, Math.round(STAMINA_BAR_FILL_W * clampedRatio));
            graphics.blit(value, barX, barY, fillWidth, barHeight, STAMINA_BAR_FILL_U, STAMINA_BAR_FILL_V, texFillWidth, STAMINA_BAR_FILL_H, STAMINA_BAR_TEX_WIDTH, STAMINA_BAR_TEX_HEIGHT);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static float getThirstRatio(Player player) {
        Capability<?> thirstCap = getThirstCapability();
        if (thirstCap == null) {
            return 1.0F;
        }

        AtomicReference<Float> ratioRef = new AtomicReference<>(1.0F);
        player.getCapability((Capability) thirstCap).ifPresent(cap -> {
            try {
                java.lang.reflect.Method getThirst = cap.getClass().getMethod("getThirst");
                Object valObj = getThirst.invoke(cap);
                if (valObj instanceof Number number) {
                    ratioRef.set(number.floatValue() / 20.0F);
                }
            } catch (Exception ignored) {
            }
        });
        return ratioRef.get();
    }

    private static void drawSimpleIcon(GuiGraphics graphics, int x, int y, ResourceLocation texture, int size) {
        drawSimpleIcon(graphics, x, y, texture, size, 1.0F, 300);
    }

    private static void drawSimpleIcon(GuiGraphics graphics, int x, int y, ResourceLocation texture, int size, float alpha) {
        drawSimpleIcon(graphics, x, y, texture, size, alpha, 300);
    }

    private static void drawSimpleIcon(GuiGraphics graphics, int x, int y, ResourceLocation texture, int size, float alpha, int texSize) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(texture, x, y, size, size, 0, 0, texSize, texSize, texSize, texSize);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawProgressBar(GuiGraphics graphics, int x, int y, int width, int height, float progress, int fillColor, int backgroundColor) {
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        graphics.fill(x, y, x + width, y + height, backgroundColor);
        graphics.fill(x + 1, y + 1, x + 1 + (int) ((width - 2) * clamped), y + height - 1, fillColor);
        graphics.renderOutline(x, y, width, height, 0x80FFFFFF);
    }

    private static void drawThirdPersonCrosshairDot(GuiGraphics graphics, int centerX, int centerY) {
        graphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, 0x90000000);
        graphics.fill(centerX, centerY, centerX + 1, centerY + 1, 0xFFFFFFFF);
    }
}

package com.yitianys.BlockZ.client.gui.mainmenu;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.yitianys.BlockZ.client.gui.UITextures;
import com.yitianys.BlockZ.client.gui.widget.DayZIconButton;
import com.yitianys.BlockZ.client.gui.widget.DayZTextButton;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.util.DayZStatsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nonnull;
import org.lwjgl.glfw.GLFW;

public class DayZMainMenuScreen extends Screen {
    private static final Component PLAY_TEXT = Component.literal("开始游戏");
    private static final Component CHANGE_SERVER_TEXT = Component.literal("更改服务器");

    private net.minecraft.client.model.PlayerModel<net.minecraft.world.entity.LivingEntity> dummyPlayerModel = null;
    private net.minecraft.client.model.PlayerModel<net.minecraft.world.entity.LivingEntity> dummySlimPlayerModel = null;

    private int currentPosterIndex = 0;
    private int prevPosterIndex = -1;
    private float transitionProgress = 1.0f;
    private int posterTimer = 0;
    private String feedbackMessage = "";
    private int feedbackTimer = 0;

    private int currentBackgroundIndex = 0;
    private int prevBackgroundIndex = -1;
    private float backgroundTransitionProgress = 1.0f;
    private int backgroundTimer = 0;
    private float cameraOffsetX = 0.0f;
    private float cameraOffsetY = 0.0f;
    private long lastSwayUpdateNanos = -1L;
    private static final int MENU_TRACK_COUNT = 3;
    private static net.minecraft.client.resources.sounds.SimpleSoundInstance menuMusicInstance = null;
    private static int lastMenuTrackIndex = -1;
    private float smokeTimer = 0.0f;

    private static class PosterData {
        final net.minecraft.resources.ResourceLocation texture;
        final java.util.function.Supplier<String> title;
        final java.util.function.Supplier<String> url;
        final java.util.function.Supplier<String> message;
        final java.util.function.Supplier<String> buttonText;

        PosterData(net.minecraft.resources.ResourceLocation texture, java.util.function.Supplier<String> title, 
                   java.util.function.Supplier<String> url, java.util.function.Supplier<String> message,
                   java.util.function.Supplier<String> buttonText) {
            this.texture = texture;
            this.title = title;
            this.url = url;
            this.message = message;
            this.buttonText = buttonText;
        }
    }

    private final PosterData[] posters = new PosterData[]{
            new PosterData(UITextures.MAIN_MENU_POSTER_0, () -> BlockZConfigs.getPosterTitle(0), () -> BlockZConfigs.getPosterUrl(0), () -> BlockZConfigs.getPosterMessage(0), () -> BlockZConfigs.getPosterButtonText(0)),
            new PosterData(UITextures.MAIN_MENU_POSTER_1, () -> BlockZConfigs.getPosterTitle(1), () -> BlockZConfigs.getPosterUrl(1), () -> BlockZConfigs.getPosterMessage(1), () -> BlockZConfigs.getPosterButtonText(1)),
            new PosterData(UITextures.MAIN_MENU_POSTER_2, () -> BlockZConfigs.getPosterTitle(2), () -> BlockZConfigs.getPosterUrl(2), () -> BlockZConfigs.getPosterMessage(2), () -> BlockZConfigs.getPosterButtonText(2))
    };

    private final net.minecraft.resources.ResourceLocation[] backgrounds = new net.minecraft.resources.ResourceLocation[]{
            UITextures.MAIN_MENU_BACKGROUND_0,
            UITextures.MAIN_MENU_BACKGROUND_1,
            UITextures.MAIN_MENU_BACKGROUND_2
    };

    public DayZMainMenuScreen() {
        super(Component.literal("DayZ Main Menu"));
        DayZStatsManager.load();
    }

    @Override
    protected void init() {
        super.init();
        tickMenuMusic();
        int panelWidth = 115;
        int panelHeight = 105;
        int panelX = this.width - panelWidth - 20; 
        int panelY = this.height - panelHeight - 20; 

        int buttonWidth = 105; 
        int buttonX = panelX + 5;

        // 1. 玩家名称
        String playerName = this.minecraft != null ? this.minecraft.getUser().getName() : "Survivor";
        Component playerNameComp = Component.literal("<   " + playerName + "   >");
        this.addRenderableWidget(new DayZTextButton(buttonX, panelY + 50, buttonWidth, 12, playerNameComp, 
            button -> {}, 1, 0.6f));

        // 2. 更改服务器
        this.addRenderableWidget(new DayZTextButton(buttonX, panelY + 62, buttonWidth, 12, CHANGE_SERVER_TEXT, 
            button -> {
                if (this.minecraft != null) this.minecraft.setScreen(new JoinMultiplayerScreen(this));
            }, 1, 0.6f));

        // 3. 开始游戏
        this.addRenderableWidget(new DayZTextButton(buttonX, panelY + 80, buttonWidth, 20, PLAY_TEXT, 
            button -> {
                if (this.minecraft != null) {
                    try {
                        String addressStr = BlockZConfigs.getServerAddress();
                        ServerData serverData = new ServerData(Component.literal("DayZ Server").getString(), addressStr, false);
                        net.minecraft.client.multiplayer.resolver.ServerAddress address = net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(addressStr);
                        
                        java.lang.reflect.Method startConnectMethod = null;
                        for (java.lang.reflect.Method m : net.minecraft.client.gui.screens.ConnectScreen.class.getDeclaredMethods()) {
                            if (m.getParameterCount() == 5 && java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                                Class<?>[] pts = m.getParameterTypes();
                                if (net.minecraft.client.gui.screens.Screen.class.isAssignableFrom(pts[0]) &&
                                    pts[1] == net.minecraft.client.Minecraft.class &&
                                    pts[4] == boolean.class) {
                                    startConnectMethod = m;
                                    break;
                                }
                            }
                        }

                        if (startConnectMethod != null) {
                            startConnectMethod.setAccessible(true);
                            startConnectMethod.invoke(null, this, this.minecraft, address, serverData, false);
                        } else {
                            this.minecraft.setScreen(new JoinMultiplayerScreen(this));
                        }
                    } catch (Exception e) {
                        this.minecraft.setScreen(new JoinMultiplayerScreen(this));
                    }
                }
            }, 1, 1.1f));

        // 右上角图标
        int iconSize = 14; 
        int iconGap = 10;
        int iconX = this.width - iconSize - 20;
        int iconY = 15; 

        this.addRenderableWidget(new DayZIconButton(iconX, iconY, iconSize, iconSize, UITextures.ICON_EXIT, 
            button -> { if (this.minecraft != null) this.minecraft.stop(); }, Component.translatable("gui.blockz.mainmenu.exit")));

        this.addRenderableWidget(new DayZIconButton(iconX - (iconSize + iconGap), iconY, iconSize, iconSize, UITextures.ICON_OPTIONS, 
            button -> { if (this.minecraft != null) this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options)); }, Component.translatable("gui.blockz.mainmenu.options")));

        this.addRenderableWidget(new DayZIconButton(iconX - (iconSize + iconGap) * 2, iconY, iconSize, iconSize, UITextures.ICON_MODS, 
            button -> {
                if (this.minecraft != null) {
                    try {
                        Class<?> modListClass = Class.forName("net.minecraftforge.client.gui.ModListScreen");
                        Screen modListScreen = (Screen) modListClass.getConstructor(Screen.class).newInstance(this);
                        this.minecraft.setScreen(modListScreen);
                    } catch (Exception ignored) {}
                }
            }, Component.translatable("gui.blockz.mainmenu.mods")));

        this.addRenderableWidget(new DayZIconButton(iconX - (iconSize + iconGap) * 3, iconY, iconSize, iconSize, UITextures.ICON_SINGLEPLAYER, 
            button -> { if (this.minecraft != null) this.minecraft.setScreen(new SelectWorldScreen(this)); }, Component.translatable("gui.blockz.mainmenu.singleplayer")));

        this.addRenderableWidget(new DayZIconButton(iconX - (iconSize + iconGap) * 4, iconY, iconSize, iconSize, UITextures.ICON_BACKERS,
            button -> { if (this.minecraft != null) this.minecraft.setScreen(new DayZBackersScreen(this)); }, Component.translatable("gui.blockz.mainmenu.backers")));
    }

    @Override
    public void tick() {
        super.tick();
        int bgRotationSpeed = BlockZConfigs.getMainMenuBackgroundRotationSpeed();
        if (bgRotationSpeed > 0) {
            backgroundTimer++;
            if (backgroundTimer >= bgRotationSpeed * 20) {
                startBackgroundTransition();
                backgroundTimer = 0;
            }
        }

        if (backgroundTransitionProgress < 1.0f) {
            float transitionStep = (float) BlockZConfigs.getMainMenuBackgroundTransitionStep();
            backgroundTransitionProgress += transitionStep;
            if (backgroundTransitionProgress > 1.0f) backgroundTransitionProgress = 1.0f;
        }

        // 宣传图轮换逻辑
        int rotationSpeed = BlockZConfigs.getPosterRotationSpeed();
        if (rotationSpeed > 0) {
            posterTimer++;
            if (posterTimer >= rotationSpeed * 20) { // 秒转 tick
                startPosterTransition(true);
                posterTimer = 0;
            }
        }

        if (transitionProgress < 1.0f) {
            transitionProgress += 0.05f; // 平滑切换速度
            if (transitionProgress > 1.0f) transitionProgress = 1.0f;
        }
        
        // 提示字幕倒计时
        if (feedbackTimer > 0) {
            feedbackTimer--;
            if (feedbackTimer == 0) feedbackMessage = "";
        }
    }

    private void startPosterTransition(boolean next) {
        prevPosterIndex = currentPosterIndex;
        if (next) {
            currentPosterIndex = (currentPosterIndex + 1) % posters.length;
        } else {
            currentPosterIndex = (currentPosterIndex - 1 + posters.length) % posters.length;
        }
        transitionProgress = 0.0f;
    }

    private void startBackgroundTransition() {
        if (backgrounds.length <= 1) return;
        prevBackgroundIndex = currentBackgroundIndex;
        currentBackgroundIndex = (currentBackgroundIndex + 1) % backgrounds.length;
        backgroundTransitionProgress = 0.0f;
    }

    private boolean isWindowHovered() {
        return this.minecraft != null
            && this.minecraft.getWindow() != null
            && GLFW.glfwGetWindowAttrib(this.minecraft.getWindow().getWindow(), GLFW.GLFW_HOVERED) == GLFW.GLFW_TRUE;
    }

    private int resolveMenuMouseX(int mouseX) {
        if (!isWindowHovered()) {
            return this.width / 2;
        }
        return Math.max(0, Math.min(this.width, mouseX));
    }

    private int resolveMenuMouseY(int mouseY) {
        if (!isWindowHovered()) {
            return this.height / 2;
        }
        return Math.max(0, Math.min(this.height, mouseY));
    }

    private void updateCameraSway(int mouseX, int mouseY) {
        float strength = (float) BlockZConfigs.getMainMenuCameraSwayStrength();
        float targetX = ((mouseX - (this.width * 0.5f)) / Math.max(1.0f, this.width * 0.5f)) * strength;
        float targetY = ((mouseY - (this.height * 0.5f)) / Math.max(1.0f, this.height * 0.5f)) * strength;

        long now = System.nanoTime();
        float dt = this.lastSwayUpdateNanos > 0L ? (now - this.lastSwayUpdateNanos) / 1_000_000_000.0f : (1.0f / 60.0f);
        this.lastSwayUpdateNanos = now;
        dt = Math.max(0.0f, Math.min(0.05f, dt));

        float smoothing = 1.0f - (float) Math.exp(-12.0f * dt);
        this.cameraOffsetX += (targetX - this.cameraOffsetX) * smoothing;
        this.cameraOffsetY += (targetY - this.cameraOffsetY) * smoothing;
    }

    private void renderRotatingBackground(@Nonnull GuiGraphics guiGraphics) {
        int margin = 16;
        float swayX = this.cameraOffsetX * 0.35f;
        float swayY = this.cameraOffsetY * 0.35f;
        int renderX = -margin;
        int renderY = -margin;
        int renderW = this.width + margin * 2;
        int renderH = this.height + margin * 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(swayX, swayY, 0.0f);

        if (backgrounds.length == 0) {
            guiGraphics.blit(UITextures.MAIN_MENU_BACKGROUND, renderX, renderY, 0, 0, renderW, renderH, renderW, renderH);
            guiGraphics.pose().popPose();
            return;
        }

        RenderSystem.enableBlend();
        if (backgroundTransitionProgress < 1.0f && prevBackgroundIndex != -1) {
            float prevAlpha = 1.0f - backgroundTransitionProgress;
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, prevAlpha);
            guiGraphics.blit(backgrounds[prevBackgroundIndex], renderX, renderY, 0, 0, renderW, renderH, renderW, renderH);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, backgroundTransitionProgress);
            guiGraphics.blit(backgrounds[currentBackgroundIndex], renderX, renderY, 0, 0, renderW, renderH, renderW, renderH);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            guiGraphics.blit(backgrounds[currentBackgroundIndex], renderX, renderY, 0, 0, renderW, renderH, renderW, renderH);
        }

        guiGraphics.pose().popPose();
    }

    private void renderSmokeOverlay(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.60f); // 稍微增加一点透明度使其更清晰
        
        // 静态渲染，覆盖整个背景区域
        guiGraphics.blit(UITextures.MAIN_MENU_BACKGROUND_SMOKE, x, y, 0, 0, w, h, w, h);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.depthMask(true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int posterW = 120;
        int posterX = 20;
        int posterY = this.height - 145; // 进一步上移，彻底解决与底部版本信息的重叠
        int posterH = 68;

        // 1. 点击左箭头
        if (mouseX >= posterX && mouseX <= posterX + 20 && mouseY >= posterY + 18 && mouseY <= posterY + 18 + posterH) {
            startPosterTransition(false);
            posterTimer = 0;
            return true;
        }

        // 2. 点击右箭头
        if (mouseX >= posterX + posterW - 20 && mouseX <= posterX + posterW && mouseY >= posterY + 18 && mouseY <= posterY + 18 + posterH) {
            startPosterTransition(true);
            posterTimer = 0;
            return true;
        }

        // 3. 点击底部的“动作按钮”区域
        if (mouseX >= posterX && mouseX <= posterX + posterW && mouseY >= posterY + posterH + 15 && mouseY <= posterY + posterH + 35) {
            PosterData data = posters[currentPosterIndex];
            String url = data.url.get();
            String msg = data.message.get();

            // 如果有链接则询问跳转
            if (url != null && !url.isEmpty()) {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmLinkScreen(confirm -> {
                        if (confirm) {
                            net.minecraft.Util.getPlatform().openUri(url);
                        }
                        this.minecraft.setScreen(this);
                    }, url, true));
                }
            }
            
            // 如果有提示则显示
            if (msg != null && !msg.isEmpty()) {
                this.feedbackMessage = msg;
                this.feedbackTimer = 60; 
            }
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int resolvedMouseX = resolveMenuMouseX(mouseX);
        int resolvedMouseY = resolveMenuMouseY(mouseY);

        updateCameraSway(resolvedMouseX, resolvedMouseY);
        renderRotatingBackground(guiGraphics);

        renderPlayerModel(guiGraphics, resolvedMouseX, resolvedMouseY);

        // 渲染烟雾叠层 (在玩家模型之后渲染，使其叠加在模型之上)
        renderSmokeOverlay(guiGraphics, 0, 0, this.width, this.height);

        int panelWidth = 115;
        int panelHeight = 105;
        int panelX = this.width - panelWidth - 20;
        int panelY = this.height - panelHeight - 20;
        guiGraphics.fill(panelX, panelY, this.width - 20, this.height - 20, 0x80000000);

        int logoSize = 128;
        // 使用加法混合模式处理黑白 Logo，使黑色部分透明
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.9F); // 稍微降一点点亮度，更显质感
        guiGraphics.blit(UITextures.MAIN_MENU_LOGO, 20, 20, 0, 0, logoSize, logoSize / 2, logoSize, logoSize / 2);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();

        int posterW = 120;
        int posterH = 68;
        int posterX = 20;
        int posterY = this.height - 145; // 进一步上移，彻底解决与底部版本信息的重叠

        // 绘制 DayZ 风格宣传图容器
        renderDayZPoster(guiGraphics, posterX, posterY, posterW, posterH);

        renderStatistics(guiGraphics);
        renderVersionInfo(guiGraphics);

        // 渲染提示字幕 (移至顶部防止被模型遮挡)
        if (!feedbackMessage.isEmpty() && feedbackTimer > 0) {
            float alpha = Math.min(1.0f, feedbackTimer / 10.0f);
            int color = ((int)(alpha * 255) << 24) | 0xFFFFFF;
            int textW = this.font.width(feedbackMessage);
            int textX = this.width / 2;
            int textY = 40; // 放置在 Logo 下方一点的顶部中心位置
            
            // 绘制半透明黑色背景条，增加可读性
            guiGraphics.fill(textX - textW / 2 - 10, textY - 5, textX + textW / 2 + 10, textY + 12, (int)(alpha * 0x80) << 24);
            guiGraphics.drawCenteredString(this.font, feedbackMessage, textX, textY, color);
        }

        super.render(guiGraphics, resolvedMouseX, resolvedMouseY, partialTick);
    }

    private void renderDayZPoster(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        // 1. 整体深色背景 (调整高度防止重叠)
        guiGraphics.fill(x, y, x + w, y + h + 35, 0xAA000000); 

        // 2. 顶部标题栏
        PosterData current = posters[currentPosterIndex];
        String title = current.title.get();
        if (title != null && !title.isEmpty()) {
            guiGraphics.fill(x + 5, y + 4, x + 7, y + 14, 0xFFFF0000); // DayZ 红色竖线
            guiGraphics.drawString(this.font, title, x + 10, y + 5, 0xFFFFFFFF, false);
        }

        // 3. 宣传图主体（平滑切换）
        RenderSystem.enableBlend();
        if (transitionProgress < 1.0f && prevPosterIndex != -1) {
            float prevAlpha = 1.0f - transitionProgress;
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, prevAlpha);
            guiGraphics.blit(posters[prevPosterIndex].texture, x, y + 18, 0, 0, w, h, w, h);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, transitionProgress);
            guiGraphics.blit(posters[currentPosterIndex].texture, x, y + 18, 0, 0, w, h, w, h);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            guiGraphics.blit(posters[currentPosterIndex].texture, x, y + 18, 0, 0, w, h, w, h);
        }

        // 4. 底部动态自定义文字
        String btnText = current.buttonText.get();
        if (btnText == null || btnText.isEmpty()) btnText = Component.translatable("gui.blockz.mainmenu.poster.learn_more").getString();
        
        int textW = this.font.width(btnText);
        guiGraphics.drawString(this.font, btnText, x + (w - textW) / 2, y + h + 22, 0xFFAAAAAA, false);
        
        // 只有带有链接时才显示红色装饰线
        String url = current.url.get();
        if (url != null && !url.isEmpty()) {
            guiGraphics.fill(x + (w - 30) / 2, y + h + 32, x + (w + 30) / 2, y + h + 33, 0xFFFF0000);
        }
        
        // 5. 左右切换箭头
        guiGraphics.drawString(this.font, "<", x + 5, y + 18 + h / 2 - 4, 0xAAFFFFFF, false);
        guiGraphics.drawString(this.font, ">", x + w - 10, y + 18 + h / 2 - 4, 0xAAFFFFFF, false);
    }

    private void renderPlayerModel(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.minecraft == null) return;
        
        if (this.minecraft.level == null) {
            // 获取玩家皮肤类型 (slim 或 normal)
            String skinModel = DefaultPlayerSkin.getSkinModelName(this.minecraft.getUser().getProfileId());
            boolean isSlim = "slim".equals(skinModel);

            // 初始化模型
            if (isSlim) {
                if (dummySlimPlayerModel == null) {
                    net.minecraft.client.model.geom.ModelPart modelPart = this.minecraft.getEntityModels().bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER_SLIM);
                    dummySlimPlayerModel = new net.minecraft.client.model.PlayerModel<>(modelPart, true);
                }
            } else {
                if (dummyPlayerModel == null) {
                    net.minecraft.client.model.geom.ModelPart modelPart = this.minecraft.getEntityModels().bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER);
                    dummyPlayerModel = new net.minecraft.client.model.PlayerModel<>(modelPart, false);
                }
            }

            net.minecraft.client.model.PlayerModel<net.minecraft.world.entity.LivingEntity> activeModel = isSlim ? dummySlimPlayerModel : dummyPlayerModel;
            activeModel.young = false;

            float centerX = (this.width * 0.5f) + (this.cameraOffsetX * 0.9f);
            float centerY = (this.height * 0.5f) - 10.0f + (this.cameraOffsetY * 0.6f);
            float scale = 70.0F; // 缩小模型 (90 -> 70)

            com.mojang.blaze3d.vertex.PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            
            poseStack.translate(centerX, centerY, 50.0D);
            poseStack.scale(scale, scale, -scale);
            // 移除 180 度旋转，因为 Z 轴镜像后模型已是正面
            
            float headY = (centerX - mouseX) / 15.0f;
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-headY * 0.12f));

            activeModel.head.xRot = 0.0F;
            activeModel.head.yRot = headY * ((float)Math.PI / 180F) * 0.55F;
            activeModel.hat.copyFrom(activeModel.head);

            activeModel.body.xRot = 0; activeModel.body.yRot = 0;
            activeModel.leftArm.xRot = 0; activeModel.leftArm.yRot = 0;
            activeModel.rightArm.xRot = 0; activeModel.rightArm.yRot = 0;
            activeModel.leftLeg.xRot = 0; activeModel.leftLeg.yRot = 0;
            activeModel.rightLeg.xRot = 0; activeModel.rightLeg.yRot = 0;

            activeModel.jacket.copyFrom(activeModel.body);
            activeModel.leftSleeve.copyFrom(activeModel.leftArm);
            activeModel.rightSleeve.copyFrom(activeModel.rightArm);
            activeModel.leftPants.copyFrom(activeModel.leftLeg);
            activeModel.rightPants.copyFrom(activeModel.rightLeg);

            net.minecraft.resources.ResourceLocation skin = DefaultPlayerSkin.getDefaultSkin(this.minecraft.getUser().getProfileId());
            
            RenderSystem.enableDepthTest();
            Lighting.setupForEntityInInventory();
            
            MultiBufferSource.BufferSource bufferSource = this.minecraft.renderBuffers().bufferSource();
            com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(skin));
            
            // 传入 15728880 表示全亮环境光
            activeModel.renderToBuffer(poseStack, vertexConsumer, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            bufferSource.endBatch();
            Lighting.setupFor3DItems();
            
            poseStack.popPose();
        } else {
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, this.width / 2, this.height / 2 + 80, 70, (float)(this.width / 2) - mouseX, (float)(this.height / 2 + 30) - mouseY, this.minecraft.player);
        }
    }

    private void renderStatistics(GuiGraphics guiGraphics) {
        int x = this.width - 25; 
        int panelY = this.height - 105 - 20;
        int panelWidth = 105;
        
        Component titleComp = Component.translatable("gui.blockz.mainmenu.stats.title");
        String title = titleComp.getString();
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, x - panelWidth + (panelWidth - titleWidth) / 2, panelY + 6, 0xFFFFFFFF, true);
        
        float statsScale = 0.55f;
        int lineHeight = 10;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, panelY + 18, 0);
        guiGraphics.pose().scale(statsScale, statsScale, 1.0f);
        
        Object[][] stats = {
            {Component.translatable("gui.blockz.mainmenu.stats.time_alive").getString(), DayZStatsManager.getFormattedTime()}, 
            {Component.translatable("gui.blockz.mainmenu.stats.players_killed").getString(), String.valueOf(DayZStatsManager.playersKilled)}, 
            {Component.translatable("gui.blockz.mainmenu.stats.zombies_killed").getString(), String.valueOf(DayZStatsManager.zombiesKilled)}, 
            {Component.translatable("gui.blockz.mainmenu.stats.distance").getString(), DayZStatsManager.getFormattedDistance()}
        };
        
        for (int i = 0; i < stats.length; i++) {
            int currentY = (int)((i * lineHeight) / statsScale);
            String label = (String)stats[i][0];
            String value = (String)stats[i][1];
            
            guiGraphics.drawString(this.font, label, (int)((x - 105) / statsScale), currentY, 0xFFAAAAAA, true);
            int valWidth = this.font.width(value);
            guiGraphics.drawString(this.font, value, (int)((x - valWidth * statsScale) / statsScale), currentY, 0xFFFFFFFF, true);
        }
        guiGraphics.pose().popPose();

        int lineY = panelY + 75;
        guiGraphics.fill(x - 105, lineY, x, lineY + 1, 0xFFFF0000);
    }

    private void renderVersionInfo(GuiGraphics guiGraphics) {
        String displayVersion = ModList.get().getModContainerById("blockz")
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
        String versionStr = Component.translatable("gui.blockz.mainmenu.version", displayVersion).getString();
        String copyrightStr = Component.translatable("gui.blockz.mainmenu.copyright").getString();
        
        guiGraphics.drawString(this.font, versionStr, 20, this.height - 30, 0x88FFFFFF, false);
        guiGraphics.drawString(this.font, copyrightStr, 20, this.height - 20, 0x88FFFFFF, false);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private static boolean shouldManageMenuMusic(Minecraft minecraft) {
        return minecraft != null && minecraft.level == null && BlockZConfigs.isCustomMainMenuEnabled();
    }

    private static int selectNextMenuTrackIndex() {
        if (MENU_TRACK_COUNT <= 1) {
            return 0;
        }
        if (lastMenuTrackIndex < 0 || lastMenuTrackIndex >= MENU_TRACK_COUNT) {
            return java.util.concurrent.ThreadLocalRandom.current().nextInt(MENU_TRACK_COUNT);
        }
        int offset = java.util.concurrent.ThreadLocalRandom.current().nextInt(MENU_TRACK_COUNT - 1);
        return offset >= lastMenuTrackIndex ? offset + 1 : offset;
    }

    private static net.minecraft.sounds.SoundEvent getMenuTrackByIndex(int index) {
        return switch (index) {
            case 1 -> com.yitianys.BlockZ.init.ModSounds.MUSIC_MENU_THEME_1.get();
            case 2 -> com.yitianys.BlockZ.init.ModSounds.MUSIC_MENU_THEME_2.get();
            default -> com.yitianys.BlockZ.init.ModSounds.MUSIC_MENU_THEME_0.get();
        };
    }

    public static void tickMenuMusic() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!shouldManageMenuMusic(minecraft)) {
            return;
        }

        if (menuMusicInstance != null && minecraft.getSoundManager().isActive(menuMusicInstance)) {
            return;
        }

        minecraft.getMusicManager().stopPlaying();
        int nextTrackIndex = selectNextMenuTrackIndex();
        lastMenuTrackIndex = nextTrackIndex;
        menuMusicInstance = net.minecraft.client.resources.sounds.SimpleSoundInstance.forMusic(getMenuTrackByIndex(nextTrackIndex));
        minecraft.getSoundManager().play(menuMusicInstance);
    }

    @Override
    public void removed() {
        super.removed();
    }

    public static void stopMenuMusic() {
        Minecraft mc = Minecraft.getInstance();
        if (menuMusicInstance != null && mc != null) {
            mc.getSoundManager().stop(menuMusicInstance);
            menuMusicInstance = null;
        }
        lastMenuTrackIndex = -1;
    }
}

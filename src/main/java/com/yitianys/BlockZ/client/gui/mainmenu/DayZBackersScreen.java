package com.yitianys.BlockZ.client.gui.mainmenu;

import com.yitianys.BlockZ.client.gui.widget.DayZTextButton;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class DayZBackersScreen extends Screen {
    private static final String SUPPORT_URL = "https://ifdian.net/a/yitianys";
    private static final String BACKERS_SECTION_TITLE = "特别鸣谢：";
    private static final List<String> FIXED_BACKERS = List.of("Jonathan", "yizhihuohuo");
    private static final String BACKERS_NOTICE = "排名不分先后";
    private final Screen parent;

    public DayZBackersScreen(Screen parent) {
        super(Component.translatable("gui.blockz.mainmenu.backers.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = 120;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int buttonY = this.height - 36;

        this.addRenderableWidget(new DayZTextButton(centerX - buttonWidth - 8, buttonY, buttonWidth, buttonHeight,
                Component.translatable("gui.blockz.mainmenu.backers.support"),
                button -> openSupportLink(), 1, 0.85F));

        this.addRenderableWidget(new DayZTextButton(centerX + 8, buttonY, buttonWidth, buttonHeight,
                Component.translatable("gui.blockz.mainmenu.backers.close"),
                button -> onClose(), 1, 0.85F));
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int panelWidth = Math.min(360, this.width - 40);
        int panelHeight = Math.min(220, this.height - 60);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 24;

        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xCC0A0A0A);
        guiGraphics.fill(panelX, panelY, panelX + 3, panelY + panelHeight, 0xCCB00000);
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0x66FFFFFF);
        guiGraphics.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, 0x44FFFFFF);

        int textX = panelX + 14;
        int currentY = panelY + 12;

        Component title = Component.translatable("gui.blockz.mainmenu.backers.title");
        guiGraphics.drawString(this.font, title, textX, currentY, 0xFFFFFFFF, true);
        currentY += 16;

        Component subtitle = Component.translatable("gui.blockz.mainmenu.backers.subtitle");
        guiGraphics.drawString(this.font, subtitle, textX, currentY, 0xFFB8B8B8, false);
        currentY += 16;

        for (FormattedCharSequence line : buildWrappedLines(this.font, Component.translatable("gui.blockz.mainmenu.backers.description").getString(), panelWidth - 28)) {
            guiGraphics.drawString(this.font, line, textX, currentY, 0xFFD8D8D8, false);
            currentY += 10;
        }

        currentY += 8;
        guiGraphics.fill(textX, currentY, panelX + panelWidth - 14, currentY + 1, 0x55FFFFFF);
        currentY += 8;

        guiGraphics.drawString(this.font, Component.literal(BACKERS_SECTION_TITLE), textX, currentY, 0xFFFFFFFF, false);
        currentY += 12;

        for (String backerName : FIXED_BACKERS) {
            guiGraphics.drawString(this.font, Component.literal("- " + backerName), textX, currentY, 0xFFFFFFFF, false);
            currentY += 10;
        }

        currentY += 4;
        for (FormattedCharSequence line : buildWrappedLines(this.font, BACKERS_NOTICE, panelWidth - 28)) {
            guiGraphics.drawString(this.font, line, textX, currentY, 0xFFFFFFFF, false);
            currentY += 10;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private void openSupportLink() {
        if (this.minecraft == null) {
            return;
        }
        this.minecraft.setScreen(new ConfirmLinkScreen(confirm -> {
            if (confirm) {
                Util.getPlatform().openUri(SUPPORT_URL);
            }
            this.minecraft.setScreen(this);
        }, SUPPORT_URL, true));
    }

    private static List<FormattedCharSequence> buildWrappedLines(Font font, String text, int maxWidth) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }
        String[] paragraphs = text.split("\\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                lines.add(FormattedCharSequence.forward("", net.minecraft.network.chat.Style.EMPTY));
                continue;
            }
            lines.addAll(font.split(Component.literal(paragraph), Math.max(40, maxWidth)));
        }
        return lines;
    }
}

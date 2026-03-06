package com.yitianys.BlockZ.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yitianys.BlockZ.client.ClientSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

public class DayZChestScreen extends AbstractContainerScreen<ChestMenu> {

    private static final int VIEW_HEIGHT = 188;
    
    // Slot 移动相关反射字段
    private static java.lang.reflect.Field SLOT_X_FIELD;
    private static java.lang.reflect.Field SLOT_Y_FIELD;

    static {
        try {
            try {
                SLOT_X_FIELD = ObfuscationReflectionHelper.findField(Slot.class, "f_40220_"); // x
                SLOT_Y_FIELD = ObfuscationReflectionHelper.findField(Slot.class, "f_40221_"); // y
            } catch (Exception e) {
                SLOT_X_FIELD = net.minecraft.world.inventory.Slot.class.getDeclaredField("x");
                SLOT_Y_FIELD = net.minecraft.world.inventory.Slot.class.getDeclaredField("y");
            }
            SLOT_X_FIELD.setAccessible(true);
            SLOT_Y_FIELD.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DayZChestScreen(ChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = UIConstants.GUI_WIDTH;
        this.imageHeight = UIConstants.GUI_HEIGHT;
    }

    @Override
    protected void init() {
        // 1. 设置新的总宽度
        // Vicinity (Wide) + Gap + Player (Normal) + Gap + Inventory (Normal)
        // 170 + 2 + 96 + 2 + 96 = 366
        this.imageWidth = getTotalWidth();
        this.imageHeight = UIConstants.HEIGHT;
        
        super.init();
        
        // 2. 重新定位所有槽位
        repositionSlots();
    }
    
    private int getVicinityPanelWidth() {
        return 9 * 18 + 8; // 170
    }
    
    private int getTotalWidth() {
        return getVicinityPanelWidth() + 2 + UIConstants.PANEL_W + 2 + UIConstants.PANEL_W;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        repositionSlots();
    }

    private void repositionSlots() {
        // 注意：Slot 的 x, y 是相对于 guiLeft, guiTop 的相对坐标！
        // 不要加 this.leftPos 和 this.topPos！
        
        int containerRows = this.menu.getRowCount();
        int containerSlots = containerRows * 9;
        
        // 1. Vicinity Panel (Container Slots) - 9 Columns
        // Panel starts at relative x = 0
        int vicX = 4; // Padding inside panel
        int vicY = UIConstants.PANEL_Y + 10;
        
        for (int i = 0; i < containerSlots; i++) {
            if (i >= this.menu.slots.size()) break;
            Slot slot = this.menu.slots.get(i);
            
            int col = i % 9;
            int row = i / 9;
            
            setSlotPos(slot, vicX + col * UIConstants.SLOT_PITCH, vicY + row * UIConstants.SLOT_PITCH);
        }

        // 2. Inventory Panel (Player Backpack 9-35)
        // Starts after Vicinity + Gap + Player + Gap
        int playerPanelStartRelX = getVicinityPanelWidth() + 2;
        int inventoryPanelStartRelX = playerPanelStartRelX + UIConstants.PANEL_W + 2;
        
        int invX = inventoryPanelStartRelX + 4;
        int invY = UIConstants.INVENTORY_SLOTS_Y; // Defined in UIConstants relative to PANEL_Y?
        // UIConstants.INVENTORY_SLOTS_Y = PANEL_Y + 10.
        
        int playerStart = containerSlots;
        // Player Inventory: 3 rows of 9 slots (Index 9-35 in Player Inventory)
        // But in Container, they are usually:
        // Slots 0-26: Main Inventory
        // Slots 27-35: Hotbar
        // Wait, ChestMenu adds player inventory first (9-35), then hotbar (0-8)?
        // No, standard ChestMenu adds chest slots, then player inventory (9-35), then hotbar (0-8).
        // Let's verify standard Minecraft logic.
        // Yes: 3 rows of 9 (Main), then 1 row of 9 (Hotbar).
        
        for (int i = 0; i < 27; i++) {
            int slotIndex = playerStart + i;
            if (slotIndex >= this.menu.slots.size()) break;
            Slot slot = this.menu.slots.get(slotIndex);
            
            // UIConstants uses 5 cols for inventory.
            // But standard player inventory is 9 cols.
            // User wants "DayZ Style".
            // DayZInventoryScreen uses 5 cols for backpack.
            // So we should stick to 5 cols layout for the right panel.
            
            int col = i % UIConstants.INVENTORY_COLS;
            int row = i / UIConstants.INVENTORY_COLS;
            
            setSlotPos(slot, invX + col * UIConstants.SLOT_PITCH, invY + row * UIConstants.SLOT_PITCH);
        }

        // 3. Hotbar Panel (Player Hotbar 0-8)
        // Located at bottom of Player Panel (Middle Panel)
        int hotbarStart = playerStart + 27;
        int hotbarRelX = playerPanelStartRelX + 4; // Inside Player Panel
        int hotbarRelY = UIConstants.HOTBAR_Y + 4; // Inside Hotbar area
        
        for (int i = 0; i < 9; i++) {
            int slotIndex = hotbarStart + i;
            if (slotIndex >= this.menu.slots.size()) break;
            Slot slot = this.menu.slots.get(slotIndex);
            
            // Hotbar 5 cols layout (as per DayZInventoryScreen)
            int col = i % 5;
            int row = i / 5;
            
            setSlotPos(slot, hotbarRelX + col * UIConstants.SLOT_PITCH, hotbarRelY + row * UIConstants.SLOT_PITCH);
        }
    }

    private void setSlotPos(Slot slot, int x, int y) {
        try {
            if (SLOT_X_FIELD != null) SLOT_X_FIELD.setInt(slot, x);
            if (SLOT_Y_FIELD != null) SLOT_Y_FIELD.setInt(slot, y);
        } catch (Exception e) {
            // Ignore
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        
        // 渲染玩家实体
        if (this.minecraft.player != null) {
            int playerPanelX = this.leftPos + getVicinityPanelWidth() + 2;
            int x = playerPanelX + UIConstants.PANEL_W / 2; // Center of Player Panel
            int y = this.topPos + UIConstants.PLAYER_MODEL_Y;
            
            graphics.pose().pushPose();
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x, y, 32, (float)x - mouseX, (float)y - 50 - mouseY, this.minecraft.player);
            graphics.pose().popPose();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        
        int vicW = getVicinityPanelWidth();
        int playerPanelX = x + vicW + 2;
        int inventoryPanelX = playerPanelX + UIConstants.PANEL_W + 2;

        // 1. Vicinity Panel (Container) - Wider
        drawPanel(graphics, x, y + UIConstants.PANEL_Y, vicW, UIConstants.PANEL_H, this.title.getString());

        // 2. Player Panel (Middle)
        int playerPanelH = UIConstants.PANEL_H - UIConstants.HOTBAR_H - 2;
        drawPanel(graphics, playerPanelX, y + UIConstants.PANEL_Y, UIConstants.PANEL_W, playerPanelH, "PLAYER");

        // 3. Hotbar Panel
        drawPanel(graphics, playerPanelX, y + UIConstants.HOTBAR_Y, UIConstants.HOTBAR_W, UIConstants.HOTBAR_H, "HOTBAR");

        // 4. Inventory Panel (Backpack)
        drawPanel(graphics, inventoryPanelX, y + UIConstants.PANEL_Y, UIConstants.PANEL_W, UIConstants.PANEL_H, "INVENTORY");

        // 渲染槽位背景
        renderSlotRangeBackground(graphics);
    }
    
    private void renderSlotRangeBackground(GuiGraphics graphics) {
        for (int i = 0; i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);
            if (!slot.isActive()) continue;

            int slotX = this.leftPos + slot.x - 1;
            int slotY = this.topPos + slot.y - 1;

            // 简单的背景渲染
            int bgColor = slot.hasItem() ? 0x30FFFFFF : 0x10FFFFFF;
            graphics.fill(slotX, slotY, slotX + 18, slotY + 18, bgColor);

            int outlineColor = slot.hasItem() ? 0x60FFFFFF : 0x20FFFFFF;
            graphics.renderOutline(slotX, slotY, 18, 18, outlineColor);
        }
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int w, int h, String title) {
        graphics.fill(x, y, x + w, y + h, 0xB0000000);
        graphics.fill(x, y - 1, x + w, y, 0xFF555555);
        graphics.fill(x, y + h, x + w, y + h + 1, 0xFF555555);
        graphics.fill(x - 1, y, x, y + h, 0xFF555555);
        graphics.fill(x + w, y, x + w + 1, y + h, 0xFF555555);

        graphics.fill(x, y - 12, x + w, y, 0xCC000000);
        // 截断过长的标题
        if (title.length() > 14) title = title.substring(0, 14);
        graphics.drawString(this.font, title, x + 4, y - 10, 0xFFDDDDDD, false);
    }
    
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 禁用原版 Label 渲染
    }
}

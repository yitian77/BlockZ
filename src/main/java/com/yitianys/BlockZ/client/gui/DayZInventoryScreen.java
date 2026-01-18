package com.yitianys.BlockZ.client.gui;

import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

public class DayZInventoryScreen extends AbstractContainerScreen<DayZInventoryMenu> {
    private int lastMouseX = 0;
    private int lastMouseY = 0;

    public DayZInventoryScreen(DayZInventoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = UIConstants.GUI_WIDTH;
        this.imageHeight = UIConstants.GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        // 计算居中偏移，使 UIConstants 中的坐标相对于屏幕居中
        this.leftPos = (this.width - UIConstants.WIDTH) / 2;
        this.topPos = (this.height - UIConstants.HEIGHT) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // 1. Vicinity Panel
        drawPanel(graphics, x + UIConstants.VICINITY_X, y + UIConstants.PANEL_Y, UIConstants.PANEL_W, UIConstants.PANEL_H, "VICINITY");
        
        // 2. Player Panel
        drawPanel(graphics, x + UIConstants.PLAYER_X, y + UIConstants.PANEL_Y, UIConstants.PANEL_W, UIConstants.PANEL_H, "PLAYER");
        if (this.minecraft.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x + UIConstants.PLAYER_X + 60, y + UIConstants.PANEL_Y + 100, 45, (float)(x + UIConstants.PLAYER_X + 60) - mouseX, (float)(y + UIConstants.PANEL_Y + 100 - 50) - mouseY, this.minecraft.player);
        }

        // 3. Inventory Panel
        drawPanel(graphics, x + UIConstants.INVENTORY_X, y + UIConstants.PANEL_Y, UIConstants.PANEL_W, UIConstants.PANEL_H, "INVENTORY");
        
        boolean hasBackpack = this.menu.hasBackpack();
        int backpackCapacity = this.menu.getBackpackCapacity();
        
        // 渲染 27 个背包槽位的背景 (根据是否有背包动态显示)
        // 第一行 (口袋) 始终显示 (索引 36-40)
        renderSlotRangeBackground(graphics, 36, 40); 
        
        // 其余 22 个槽位 (背包) 根据容量显示 (索引 41-62)
        if (hasBackpack && backpackCapacity > 0) {
            renderSlotRangeBackground(graphics, 41, 41 + backpackCapacity - 1);
        } else if (!hasBackpack) {
            // 提示玩家需要背包
            graphics.drawString(this.font, "NEED BACKPACK", x + UIConstants.INVENTORY_X + 15, y + UIConstants.PANEL_Y + 40, 0x40FFFFFF, false);
        }
        
        // 渲染 Vicinity 和 Armor + Offhand + BackpackEquip 槽位背景
        renderSlotRangeBackground(graphics, 0, 35);
        
        // 渲染快捷栏背景
        renderSlotRangeBackground(graphics, 63, 71);
        
        // 渲染图标
        renderSlotIcons(graphics);
    }

    private void renderSlotRangeBackground(GuiGraphics graphics, int start, int end) {
        for (int i = start; i <= end && i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);
            if (slot.isActive()) {
                int slotX = this.leftPos + slot.x - 1;
                int slotY = this.topPos + slot.y - 1;
                // 更精致的槽位背景
                graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0x30FFFFFF);
                graphics.renderOutline(slotX, slotY, 18, 18, 0x15FFFFFF);
            }
        }
    }

    private boolean isRangeNotEmpty(int start, int end) {
        for (int i = start; i <= end && i < this.menu.slots.size(); i++) {
            if (this.menu.slots.get(i).hasItem()) return true;
        }
        return false;
    }

    private boolean isVicinityEmpty() {
        for (int i = 0; i < 30; i++) {
            if (!this.menu.slots.get(i).getItem().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void renderSlotIcons(GuiGraphics graphics) {
        for (Slot slot : this.menu.slots) {
            if (slot.isActive() && !slot.hasItem()) {
                int slotX = this.leftPos + slot.x;
                int slotY = this.topPos + slot.y;
                
                ResourceLocation icon = getSlotIcon(slot);
                if (icon != null) {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.4F); // 半透明图标
                    graphics.blit(icon, slotX, slotY, 0, 0, 16, 16, 16, 16);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
        }
    }

    private ResourceLocation getSlotIcon(Slot slot) {
        // 索引重算:
        // Vicinity: 0-29
        // Armor: 30-33
        // Offhand: 34
        // BackpackEquip: 35
        // Inventory: 36-62
        // Hotbar: 63-71

        int index = slot.index;
        
        if (index >= 0 && index <= 29) return null;

        if (index == 30) return UITextures.SLOT_HEADWEAR;
        if (index == 31) return UITextures.SLOT_VEST;
        if (index == 32) return UITextures.SLOT_PANTS;
        if (index == 33) return UITextures.SLOT_SHOES;
        
        if (index == 34) return UITextures.SLOT_OFFHAND;
        if (index == 35) return UITextures.SLOT_BACKPACK;
        
        return null;
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int w, int h, String title) {
        // 仿 DayM 风格面板
        graphics.fill(x, y, x + w, y + h, 0x80000000); // 稍微加深背景
        graphics.fill(x, y - 1, x + w, y, 0xFF555555); // 上边框 (细线)
        graphics.fill(x, y + h, x + w, y + h + 1, 0xFF555555); // 下边框
        graphics.fill(x - 1, y, x, y + h, 0xFF555555); // 左边框
        graphics.fill(x + w, y, x + w + 1, y + h, 0xFF555555); // 右边框

        // 标题栏
        graphics.fill(x, y - 12, x + w, y, 0xCC000000);
        // 标题文字 - 使用小字体效果或调整颜色
        graphics.drawString(this.font, title, x + 4, y - 10, 0xFFDDDDDD, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // 渲染悬停物品的名称 (DayZ 风格)
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem() && this.menu.getCarried().isEmpty()) {
            ItemStack stack = this.hoveredSlot.getItem();
            String name = stack.getHoverName().getString();
            graphics.drawString(this.font, name, mouseX + 12, mouseY - 12, 0xFFFFFFFF, true);
        }
        
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 不渲染默认的 "Inventory" 和 "Title"
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // 处理“拖拽丢弃”逻辑
        // 如果释放鼠标时，鼠标在 VICINITY 面板区域内，且当前正在拖拽物品
        if (button == 0 && !this.menu.getCarried().isEmpty()) {
            int vicX = this.leftPos + UIConstants.VICINITY_X;
            int vicY = this.topPos + UIConstants.PANEL_Y;
            if (mouseX >= vicX && mouseX <= vicX + UIConstants.PANEL_W && 
                mouseY >= vicY && mouseY <= vicY + UIConstants.PANEL_H) {
                
                // 模拟点击 UI 外部以丢弃物品
                // 在 Minecraft 中，AbstractContainerScreen 处理点击 -999 (外部) 为丢弃
                this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId, -999, 0, ClickType.PICKUP, this.minecraft.player);
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 数字键1-9快速移动物品到快捷栏
        if (keyCode >= 49 && keyCode <= 57) {
            Slot hovered = this.hoveredSlot;
            if (hovered != null && hovered.hasItem()) {
                int hotbarIndex = keyCode - 49;
                Inventory inv = this.minecraft.player.getInventory();
                ItemStack stack = hovered.getItem();
                ItemStack existing = inv.items.get(hotbarIndex);
                inv.items.set(hotbarIndex, stack.copy());
                hovered.set(existing);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

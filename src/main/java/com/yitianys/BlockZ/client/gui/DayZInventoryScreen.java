package com.yitianys.BlockZ.client.gui;

import com.yitianys.BlockZ.client.key.ModKeyMappings;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import com.yitianys.BlockZ.network.NetworkHandler;
import com.yitianys.BlockZ.network.RotateItemC2S;
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
import net.minecraftforge.items.IItemHandler;
import com.yitianys.BlockZ.util.ItemSizeManager;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

public class DayZInventoryScreen extends AbstractContainerScreen<DayZInventoryMenu> {

    private float scrollOffs;
    private boolean isScrolling;
    // Viewport Height: Panel Height (232) - Header (~20) - Footer (~12) = ~200.
    // Let's check UIConstants.INVENTORY_SLOTS_Y (40?) + Viewport
    // Assuming UIConstants.PANEL_H is around 232.
    // If INVENTORY_SLOTS_Y is 40, and we want to stop before HOTBAR_Y (208).
    // 208 - 40 = 168 pixels visible.
    private static final int VIEW_HEIGHT = 168;
    private static final ResourceLocation LOCK_ICON = ResourceLocation.fromNamespaceAndPath("blockz", "textures/gui/inventory/lock.png");

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

        // 1. Vicinity Panel / Container Panel
        net.minecraft.world.Container activeContainer = this.menu.getActiveContainer();
        String vicinityTitle = "VICINITY";
        if (activeContainer instanceof net.minecraft.world.Nameable nameable && nameable.hasCustomName()) {
            vicinityTitle = nameable.getDisplayName().getString();
        } else if (activeContainer != null) {
             if (activeContainer instanceof net.minecraft.world.Nameable nameable) {
                 vicinityTitle = nameable.getDisplayName().getString().toUpperCase();
             } else {
                 vicinityTitle = "CONTAINER";
             }
        }
        drawPanel(graphics, x + UIConstants.VICINITY_X, y + UIConstants.PANEL_Y, UIConstants.PANEL_W, UIConstants.PANEL_H, vicinityTitle);
        
        // 2. Player Panel (Central Upper)
        int playerPanelH = UIConstants.PANEL_H - UIConstants.HOTBAR_H - 2;
        drawPanel(graphics, x + UIConstants.PLAYER_X, y + UIConstants.PANEL_Y, UIConstants.PANEL_W, playerPanelH, "PLAYER");
        
        // Use defined constant for Player Model position to avoid overlap with Crafting
        renderPlayerInInventory(graphics, x + UIConstants.PLAYER_MODEL_X, y + UIConstants.PLAYER_MODEL_Y, mouseX, mouseY);

        // 3. Hotbar Panel (Central Lower)
        drawPanel(graphics, x + UIConstants.HOTBAR_X, y + UIConstants.HOTBAR_Y, UIConstants.HOTBAR_W, UIConstants.HOTBAR_H, "HOTBAR");

        // 4. Inventory Panel
        drawPanel(graphics, x + UIConstants.INVENTORY_X, y + UIConstants.PANEL_Y, UIConstants.PANEL_W, UIConstants.PANEL_H, "INVENTORY");
        
        boolean hasBackpack = this.menu.hasBackpack();
        int backpackCapacity = this.menu.getBackpackCapacity();
        
        // 渲染所有槽位背景 (仅渲染激活的)
        // Vicinity: 0-29
        // Equipment: 30-38
        // Inventory: 39-88 (39-43 Pockets, 44-88 Backpack/Vest)
        // Hotbar: 89-97
        // Crafting: 98 (Result), 99-102 (Input)
        renderSlotRangeBackground(graphics, 0, 102);

        // 5. Crafting Label
        graphics.drawString(this.font, "CRAFTING", x + UIConstants.CRAFTING_X, y + UIConstants.CRAFTING_Y - 10, 0xFFDDDDDD, false);

        // Render Capacity Tooltips for Equipment
        renderEquipmentCapacity(graphics, mouseX, mouseY, 35, this.menu.backpackCapacity); // Backpack
        renderEquipmentCapacity(graphics, mouseX, mouseY, 36, this.menu.vestCapacity);     // Vest
        renderEquipmentCapacity(graphics, mouseX, mouseY, 31, this.menu.shirtCapacity);    // Shirt
        renderEquipmentCapacity(graphics, mouseX, mouseY, 32, this.menu.pantsCapacity);    // Pants

        if (!hasBackpack && this.menu.vestCapacity == 0 && this.menu.shirtCapacity == 0 && this.menu.pantsCapacity == 0) {
            // 提示玩家需要存储空间
            graphics.drawString(this.font, "NEED STORAGE", x + UIConstants.INVENTORY_X + 10, y + UIConstants.PANEL_Y + 40, 0x40FFFFFF, false);
        }
        
        // 渲染图标
        renderSlotIcons(graphics);

        renderTetrisFootprints(graphics);
    }

    private void renderEquipmentCapacity(GuiGraphics graphics, int mouseX, int mouseY, int slotIndex, int capacity) {
        if (capacity <= 0) return;
        if (slotIndex >= this.menu.slots.size()) return;

        Slot slot = this.menu.slots.get(slotIndex);
        if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
            String capStr = String.format("CAPACITY: %d", capacity);
            // Draw text above the slot (centered horizontally relative to slot if possible, or fixed offset)
            // Original offset: leftPos + bpSlot.x - 20, topPos + bpSlot.y - 10
            int textX = this.leftPos + slot.x - 20; 
            int textY = this.topPos + slot.y - 10;
            
            // Draw text with yellow color
            graphics.drawString(this.font, capStr, textX, textY, 0xFFFFFF00, true);
            
            // Draw yellow outline around the slot
            graphics.renderOutline(this.leftPos + slot.x - 1, this.topPos + slot.y - 1, 18, 18, 0xFFFFFF00);
        }
    }

    private void renderSlotRangeBackground(GuiGraphics graphics, int start, int end) {
        for (int i = start; i <= end && i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);
            if (slot.isActive()) {
                int slotX = this.leftPos + slot.x - 1;
                int slotY = this.topPos + slot.y - 1;
                // Dimmer background for empty slots, standard for occupied
                int bgColor = slot.hasItem() ? 0x30FFFFFF : 0x10FFFFFF; 
                graphics.fill(slotX, slotY, slotX + 18, slotY + 18, bgColor);
                
                // Dimmer outline for empty slots, Brighter for occupied
                // User requested "Occupied grid border more visible, not inventory border"
                int outlineColor = slot.hasItem() ? 0x60FFFFFF : 0x20FFFFFF;
                graphics.renderOutline(slotX, slotY, 18, 18, outlineColor);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Do not call super.renderLabels to avoid default title rendering
        
        int x = UIConstants.INVENTORY_SLOTS_X;
        int totalContentHeight = this.menu.totalContentHeight;
        int maxScroll = Math.max(0, totalContentHeight - VIEW_HEIGHT);
        int scrollPixels = (int) (this.scrollOffs * maxScroll);
        
        int guiMinY = UIConstants.INVENTORY_SLOTS_Y;
        int guiMaxY = guiMinY + VIEW_HEIGHT;

        renderSectionLabel(graphics, net.minecraft.network.chat.Component.translatable("screen.blockz.pockets"), x, this.menu.pocketsY, scrollPixels, guiMinY, guiMaxY);
        renderSectionLabel(graphics, net.minecraft.network.chat.Component.translatable("screen.blockz.backpack"), x, this.menu.backpackY, scrollPixels, guiMinY, guiMaxY);
        renderSectionLabel(graphics, net.minecraft.network.chat.Component.translatable("screen.blockz.vest"), x, this.menu.vestY, scrollPixels, guiMinY, guiMaxY);
        renderSectionLabel(graphics, net.minecraft.network.chat.Component.translatable("screen.blockz.shirt_pocket"), x, this.menu.shirtY, scrollPixels, guiMinY, guiMaxY);
        renderSectionLabel(graphics, net.minecraft.network.chat.Component.translatable("screen.blockz.pants_pocket"), x, this.menu.pantsY, scrollPixels, guiMinY, guiMaxY);

        // Render Scrollbar
        if (maxScroll > 0) {
            int scrollX = UIConstants.INVENTORY_X + UIConstants.PANEL_W - 14;
            int scrollY = UIConstants.INVENTORY_SLOTS_Y;
            int scrollH = VIEW_HEIGHT;

            graphics.fill(scrollX, scrollY, scrollX + 8, scrollY + scrollH, 0x20000000);
            
            int thumbH = Math.max(10, (int)(scrollH * (VIEW_HEIGHT / (float)totalContentHeight)));
            int thumbTrackH = scrollH - thumbH;
            int thumbY = scrollY + (int)(this.scrollOffs * thumbTrackH);
            
            graphics.fill(scrollX + 1, thumbY, scrollX + 7, thumbY + thumbH, 0xFF888888);
            graphics.fill(scrollX + 1, thumbY, scrollX + 6, thumbY + thumbH - 1, 0xFFCCCCCC);
            graphics.fill(scrollX + 2, thumbY + 1, scrollX + 7, thumbY + thumbH, 0xFF555555);
            graphics.fill(scrollX + 2, thumbY + 1, scrollX + 6, thumbY + thumbH - 1, 0xFFAAAAAA);
        }
    }

    private void renderSectionLabel(GuiGraphics graphics, net.minecraft.network.chat.Component text, int x, int baseY, int scrollPixels, int minY, int maxY) {
        if (baseY < -500) return; // Hidden section (using threshold because init is -1000)
        
        int y = baseY - 12 - scrollPixels;
        
        // Simple visibility check
        if (y + 8 > minY && y < maxY) {
            // Draw text with shadow
            graphics.drawString(this.font, text, x, y, 0xFFAAAAAA, true);
        }
    }

    private void renderLockedSlots(GuiGraphics graphics) {
        if (com.yitianys.BlockZ.client.ClientSettings.dayzEnabled || this.minecraft.player.hasPermissions(2)) {
            return;
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300); // 确保在物品之上

        // Render lock icon over extra Inventory Slots 44-88 when DayZ UI is disabled
        // Pockets (39-43) are standard vanilla slots and remain unlocked
        for (int i = 44; i <= 88; i++) {
            if (i >= this.menu.slots.size()) break;
            Slot slot = this.menu.slots.get(i);
            
            // 只有当槽位在屏幕范围内且激活时才渲染
            if (slot.y < -2000 || !slot.isActive()) continue;

            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;

            // 绘制半透明黑色遮罩
            graphics.fill(x, y, x + 16, y + 16, 0x80000000);
            
            // 绘制锁图标
            RenderSystem.setShaderTexture(0, LOCK_ICON);
            RenderSystem.enableBlend();
            graphics.blit(LOCK_ICON, x, y, 0, 0, 16, 16, 16, 16);
        }
        
        graphics.pose().popPose();
    }

    private void renderPlayerInInventory(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        if (this.minecraft.player == null) return;
        
        // 使用 graphics.pose() 获取 PoseStack
        graphics.pose().pushPose();
        
        // 渲染玩家实体 (Scale reduced from 42 to 32 as requested: 0.75x)
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x, y, 32, (float)x - mouseX, (float)y - 50 - mouseY, this.minecraft.player);
        
        graphics.pose().popPose();
    }

    private void renderSlotIcons(GuiGraphics graphics) {
        for (int slotId = 0; slotId < this.menu.slots.size(); slotId++) {
            Slot slot = this.menu.slots.get(slotId);
            if (!slot.isActive() || slot.hasItem()) continue;

            int slotX = this.leftPos + slot.x;
            int slotY = this.topPos + slot.y;

            ResourceLocation icon = getSlotIcon(slotId);
            if (icon == null) continue;

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.4F);
            graphics.blit(icon, slotX, slotY, 0, 0, 16, 16, 16, 16);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private ResourceLocation getSlotIcon(int slotId) {
        // 索引重算 (DayZ 布局):
        // Vicinity: 0-29
        // 30: Headgear
        // 31: Shirt
        // 32: Pants
        // 33: Shoes
        // 34: Offhand
        // 35: Backpack
        // 36: Vest
        // 37: Gloves
        // 38: Mask

        if (slotId >= 0 && slotId <= 29) return null;

        if (slotId == 30) return UITextures.SLOT_HEADGEAR;
        if (slotId == 31) return UITextures.SLOT_SHIRT;
        if (slotId == 32) return UITextures.SLOT_PANTS;
        if (slotId == 33) return UITextures.SLOT_SHOES;
        if (slotId == 34) return UITextures.SLOT_OFFHAND;
        if (slotId == 35) return UITextures.SLOT_BACKPACK;
        if (slotId == 36) return UITextures.SLOT_VEST;
        if (slotId == 37) return UITextures.SLOT_GLOVES;
        if (slotId == 38) return UITextures.SLOT_MASK;

        return null;
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int w, int h, String title) {
        // 仿 DayM 风格面板
        graphics.fill(x, y, x + w, y + h, 0xB0000000); // 加深背景不透明度，提高对比度
        graphics.fill(x, y - 1, x + w, y, 0xFF555555); // 上边框 (细线)
        graphics.fill(x, y + h, x + w, y + h + 1, 0xFF555555); // 下边框
        graphics.fill(x - 1, y, x, y + h, 0xFF555555); // 左边框
        graphics.fill(x + w, y, x + w + 1, y + h, 0xFF555555); // 右边框

        // 标题栏
        graphics.fill(x, y - 12, x + w, y, 0xCC000000);
        // 标题文字 - 使用小字体效果或调整颜色
        graphics.drawString(this.font, title, x + 4, y - 10, 0xFFDDDDDD, false);
    }

    private void renderTetrisFootprints(GuiGraphics graphics) {
        // 1. Render for Inventory Sections (Backpack/Vest 44-88)
        // Vicinity (0-29) and Pockets (39-43) are regular slots and should not show footprints
        // because they don't use the Tetris grid system for occupancy.
        int scissorX = this.leftPos + UIConstants.INVENTORY_X;
        int scissorY = this.topPos + UIConstants.INVENTORY_SLOTS_Y - 1;
        int scissorW = UIConstants.PANEL_W;
        int scissorH = VIEW_HEIGHT + 2;

        graphics.enableScissor(scissorX, scissorY, scissorX + scissorW, scissorY + scissorH);
        renderFootprintsForRange(graphics, 44, 88, UIConstants.INVENTORY_COLS, true);
        graphics.disableScissor();
    }

    private void renderFootprintsForRange(GuiGraphics graphics, int startIdx, int endIdx, int cols, boolean isInventory) {
        for (int i = startIdx; i <= endIdx; i++) {
            if (i >= this.menu.slots.size()) break;
            Slot slot = this.menu.slots.get(i);
            if (!slot.hasItem()) continue;

            ItemStack stack = slot.getItem();
            ItemSizeManager.ItemSize size = ItemSizeManager.getSize(stack);
            if (size.width() <= 1 && size.height() <= 1) continue;

            // Only render if this is the anchor slot for a multi-slot item
            // or if it's a TetrisSlot and we want to show its extent
            int slotX = this.leftPos + slot.x - 1;
            int slotY = this.topPos + slot.y - 1;
            int pixelW = size.width() * UIConstants.SLOT_PITCH;
            int pixelH = size.height() * UIConstants.SLOT_PITCH;

            // Draw footprint background
            graphics.fill(slotX + 1, slotY + 1, slotX + pixelW - 1, slotY + pixelH - 1, 0x60FFFFAA);
            graphics.renderOutline(slotX, slotY, pixelW, pixelH, 0xA0FFFFAA);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        // 渲染拿起物品的占格预览
        renderCarriedItemPreview(graphics, mouseX, mouseY);
        
        // 渲染悬停物品的名称 (DayZ 风格)
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem() && this.menu.getCarried().isEmpty()) {
            ItemStack stack = this.hoveredSlot.getItem();
            String name = stack.getHoverName().getString();
            graphics.drawString(this.font, name, mouseX + 12, mouseY - 12, 0xFFFFFFFF, true);
        }

        if (this.menu.isEnchantingTable) {
            renderEnchantingInfo(graphics, mouseX, mouseY);
        }
        
        // 渲染锁图标 (在物品之上)
        renderLockedSlots(graphics);
        
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderCarriedItemPreview(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack carried = this.menu.getCarried();
        if (carried.isEmpty() || this.hoveredSlot == null) return;

        ItemSizeManager.ItemSize size = ItemSizeManager.getSize(carried);
        int w = size.width();
        int h = size.height();

        // Determine if it fits
        boolean fits = true;
        int id = this.hoveredSlot.index;
        // Grid areas: ONLY Backpack/Vest (44-88)
        // Vicinity (0-29) and Pockets (39-43) are NOT grid areas for multi-slot occupancy
        boolean isGridArea = (id >= 44 && id <= 88);
        
        int previewW = w;
        int previewH = h;

        if (isGridArea) {
            // Detailed fit check using the slot's own logic (Tetris grid check)
            fits = this.hoveredSlot.mayPlace(carried);
        } else {
            // Non-grid area: Items only take ONE slot regardless of size
            previewW = 1;
            previewH = 1;
            fits = this.hoveredSlot.mayPlace(carried);
        }

        int slotX = this.leftPos + this.hoveredSlot.x - 1;
        int slotY = this.topPos + this.hoveredSlot.y - 1;
        int pixelW = previewW * UIConstants.SLOT_PITCH;
        int pixelH = previewH * UIConstants.SLOT_PITCH;

        int color = fits ? 0x8055FF55 : 0x80FF5555; // Green if fits, Red if not
        
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 250); // Draw above slots and items
        graphics.renderOutline(slotX, slotY, pixelW, pixelH, color | 0xFF000000); // Opaque border
        graphics.fill(slotX + 1, slotY + 1, slotX + pixelW - 1, slotY + pixelH - 1, color); // Semi-transparent fill
        graphics.pose().popPose();
    }

    private void renderEnchantingInfo(GuiGraphics graphics, int mouseX, int mouseY) {
        int centerX = this.leftPos + UIConstants.VICINITY_X + UIConstants.PANEL_W / 2;
        int startY = this.topPos + UIConstants.VICINITY_SLOTS_Y + 80;
        
        for (int i = 0; i < 3; i++) {
            int y = startY + i * 20;
            int cost = this.menu.costs[i];
            int clue = this.menu.enchantClue[i];
            int level = this.menu.levelClue[i];
            
            if (cost == 0) {
                // Disabled button
                graphics.fill(centerX - 40, y, centerX + 40, y + 18, 0x50000000);
                graphics.drawString(this.font, "---", centerX - 6, y + 5, 0xFF555555, false);
            } else {
                // Enabled button
                boolean hovered = mouseX >= centerX - 40 && mouseX <= centerX + 40 && mouseY >= y && mouseY <= y + 18;
                int color = hovered ? 0x80FFFFFF : 0x50000000;
                graphics.fill(centerX - 40, y, centerX + 40, y + 18, color);
                graphics.renderOutline(centerX - 40, y, 80, 18, 0xFFAAAAAA);
                
                String text = "LVL " + cost;
                graphics.drawString(this.font, text, centerX - 35, y + 5, 0xFF55FF55, true);
                
                // Show clue if hovered
                if (hovered && clue >= 0) {
                     // Get enchantment name
                     net.minecraft.world.item.enchantment.Enchantment enchant = net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY).registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).byId(clue);
                     if (enchant != null) {
                         net.minecraft.network.chat.MutableComponent clueText = enchant.getFullname(level).copy();
                         graphics.renderTooltip(this.font, clueText, mouseX, mouseY);
                     }
                }
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.menu.updateSlotPositions();
        applyScroll();
    }

    private void applyScroll() {
        int totalContentHeight = this.menu.totalContentHeight;
        int maxScroll = totalContentHeight - VIEW_HEIGHT;
        if (maxScroll <= 0) {
            scrollOffs = 0;
            return;
        }

        int scrollPixels = (int) (this.scrollOffs * maxScroll);
        int startY = this.topPos + UIConstants.INVENTORY_SLOTS_Y;

        // Apply scroll to Pockets (39-43) and Backpack/Vest/etc (44-88)
        for (int i = 39; i <= 88; i++) {
            if (i < this.menu.slots.size()) {
                Slot slot = this.menu.slots.get(i);
                // Check if slot was hidden by Menu (-10000)
                if (slot.y < -2000) continue;

                // Adjust Y
                int newY = slot.y - scrollPixels;
                
                // Hide if out of bounds
                // Note: slot.y is relative to topPos in Menu?
                // Actually DayZInventoryMenu sets x,y relative to UIConstants
                // which are relative to top-left of the GUI.
                // slot.y in Menu is relative to the GUI top-left.
                // So slot.y 40 is 40 pixels from top of GUI.
                
                // Visible range in GUI coordinates:
                int guiMinY = UIConstants.INVENTORY_SLOTS_Y;
                int guiMaxY = guiMinY + VIEW_HEIGHT;

                if (newY + 16 < guiMinY || newY > guiMaxY) {
                     setSlotPos(slot, -10000, -10000);
                } else {
                     setSlotPos(slot, slot.x, newY);
                }
                
                // Adjust section labels similarly (just storing them in menu for now, but we need to render them with offset)
                // We'll handle label rendering offset in renderLabels
            }
        }
        
        // Adjust Label positions in Menu for rendering?
        // No, menu.pocketsY etc are integers. We should apply scroll when rendering labels.
    }

    private static java.lang.reflect.Field SLOT_X_FIELD;
    private static java.lang.reflect.Field SLOT_Y_FIELD;

    static {
        try {
            // 尝试使用 ObfuscationReflectionHelper 获取字段 (适用于生产环境 SRG 名)
            try {
                SLOT_X_FIELD = ObfuscationReflectionHelper.findField(Slot.class, "f_40220_"); // x
                SLOT_Y_FIELD = ObfuscationReflectionHelper.findField(Slot.class, "f_40221_"); // y
            } catch (Exception e) {
                // 如果失败 (例如在某些开发环境中)，尝试直接获取
                SLOT_X_FIELD = net.minecraft.world.inventory.Slot.class.getDeclaredField("x");
                SLOT_Y_FIELD = net.minecraft.world.inventory.Slot.class.getDeclaredField("y");
            }
            SLOT_X_FIELD.setAccessible(true);
            SLOT_Y_FIELD.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int totalContentHeight = this.menu.totalContentHeight;
        if (totalContentHeight > VIEW_HEIGHT) {
            int i = totalContentHeight - VIEW_HEIGHT;
            // Scroll speed factor
            this.scrollOffs = (float)((double)this.scrollOffs - delta / (double)i * 16.0D);
            this.scrollOffs = net.minecraft.util.Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            this.menu.updateSlotPositions(); // Reset positions first
            applyScroll(); // Re-apply scroll
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isScrolling) {
            int j = this.topPos + UIConstants.INVENTORY_SLOTS_Y;
            int k = j + VIEW_HEIGHT;
            this.scrollOffs = ((float)mouseY - (float)j - 7.5F) / ((float)(k - j) - 15.0F);
            this.scrollOffs = net.minecraft.util.Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            this.menu.updateSlotPositions();
            applyScroll();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 1. Scrollbar Logic
        if (button == 0) {
            int x = this.leftPos + UIConstants.INVENTORY_X + UIConstants.PANEL_W - 14; // Scrollbar X
            int y = this.topPos + UIConstants.INVENTORY_SLOTS_Y;
            int w = 8; // Scrollbar Width
            int h = VIEW_HEIGHT;
            
            if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h) {
                this.isScrolling = totalContentSizeGreaterThanView();
                if (this.isScrolling) return true;
            }
        }

        // 2. Enchanting Table Logic
        if (this.menu.isEnchantingTable && button == 0) {
            int centerX = this.leftPos + UIConstants.VICINITY_X + UIConstants.PANEL_W / 2;
            int startY = this.topPos + UIConstants.VICINITY_SLOTS_Y + 80;

            for (int i = 0; i < 3; i++) {
                int y = startY + i * 20;
                if (mouseX >= centerX - 40 && mouseX <= centerX + 40 && mouseY >= y && mouseY <= y + 18) {
                    if (this.menu.costs[i] > 0) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, i);
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isScrolling = false;
        }

        // 处理“拖拽丢弃”逻辑
        // 如果释放鼠标时，鼠标在 VICINITY 面板区域内，且当前正在拖拽物品
        if (button == 0 && !this.menu.getCarried().isEmpty()) {
            int vicX = this.leftPos + UIConstants.VICINITY_X;
            int vicY = this.topPos + UIConstants.PANEL_Y;
            if (mouseX >= vicX && mouseX <= vicX + UIConstants.PANEL_W && 
                mouseY >= vicY && mouseY <= vicY + UIConstants.PANEL_H) {
                
                // 检查是否落在了某个槽位上。
                // 只要鼠标在槽位范围内，就认为是在进行槽位交互，而不是丢弃到地面。
                boolean overSlot = false;
                for (int i = 0; i < this.menu.slots.size(); i++) {
                    Slot slot = this.menu.slots.get(i);
                    if (slot.isActive() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                        overSlot = true;
                        break;
                    }
                }

                if (!overSlot) {
                    // 丢弃物品：模拟点击 UI 外部
                    this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId, -999, 0, ClickType.PICKUP, this.minecraft.player);
                    return true;
                }
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean totalContentSizeGreaterThanView() {
        return this.menu.totalContentHeight > VIEW_HEIGHT;
    }

    @Override
    protected boolean isHovering(int x, int y, int w, int h, double mouseX, double mouseY) {
        return mouseX >= (double)(this.leftPos + x) && mouseX < (double)(this.leftPos + x + w) && mouseY >= (double)(this.topPos + y) && mouseY < (double)(this.topPos + y + h);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyMappings.ROTATE_ITEM != null && ModKeyMappings.ROTATE_ITEM.matches(keyCode, scanCode)) {
            ItemStack carried = this.menu.getCarried();
            if (!carried.isEmpty()) {
                // Toggle locally for immediate visual feedback
                ItemSizeManager.toggleRotation(carried);
                NetworkHandler.CHANNEL.sendToServer(new RotateItemC2S());
                return true;
            }
        }
        // 数字键1-9快速移动物品到快捷栏
        if (keyCode >= 49 && keyCode <= 57) {
            Slot hovered = this.hoveredSlot;
            if (hovered != null && hovered.hasItem()) {
                // 默认行为已经处理了数字键切换，这里可以根据需要自定义
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

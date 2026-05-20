package com.yitianys.BlockZ.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import com.yitianys.BlockZ.client.ClientSettings;
import com.yitianys.BlockZ.compat.CuriosIntegration;
import com.yitianys.BlockZ.client.key.ModKeyMappings;
import com.yitianys.BlockZ.entity.CorpseEntity;
import com.yitianys.BlockZ.entity.ZombieCorpseEntity;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import com.yitianys.BlockZ.menu.slot.TetrisSlot;
import com.yitianys.BlockZ.network.NetworkHandler;
import com.yitianys.BlockZ.network.RotateItemC2S;
import com.yitianys.BlockZ.util.ItemSizeManager;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

public class DayZInventoryScreen extends AbstractContainerScreen<DayZInventoryMenu> {
   private float scrollOffs;
   private boolean isScrolling;
   private float vicinityScrollOffs;
   private boolean isVicinityScrolling;
   private boolean clickStartedInVicinitySlot;
   private static final int VIEW_HEIGHT = 188;
   private static final ResourceLocation LOCK_ICON = new ResourceLocation("blockz", "textures/gui/inventory/lock.png");
   private static final int DEFAULT_GRID_FILL = 0x60808080;
   private static final int DEFAULT_GRID_OUTLINE = 0xFF5A5A5A;
   private static final int CUSTOM_GRID_ALPHA = 0x60;
   private static final int CUSTOM_PREVIEW_ALPHA = 0x20;
   private static final int PREVIEW_FAIL_COLOR = 0x80E03C3C;
   private static final int MULTI_SLOT_HOVER_FILL = 0x66FFFFFF;
   private static final int MULTI_SLOT_HOVER_OUTLINE = 0xFFF0F0F0;
   private static Field SLOT_X_FIELD;
   private static Field SLOT_Y_FIELD;

   public DayZInventoryScreen(DayZInventoryMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
      this.imageWidth = UIConstants.WIDTH;
      this.imageHeight = 200;
   }

   private int getGridFillColor(ItemStack stack, int defaultColor) {
      Integer custom = ItemSizeManager.getGridColor(stack);
      if (custom == null) {
         return defaultColor;
      }
      return applyAlpha(custom, CUSTOM_GRID_ALPHA);
   }

   private int getGridOutlineColor(ItemStack stack, int defaultColor) {
      Integer custom = ItemSizeManager.getGridColor(stack);
      if (custom == null) {
         return defaultColor;
      }
      return custom | 0xFF000000;
   }

   private int getPreviewFillColor(ItemStack stack, boolean fits) {
      if (!fits) {
         return PREVIEW_FAIL_COLOR;
      }
      Integer custom = ItemSizeManager.getGridColor(stack);
      if (custom == null) {
         return DEFAULT_GRID_FILL;
      }
      return applyAlpha(custom, CUSTOM_PREVIEW_ALPHA);
   }

   private int applyAlpha(int color, int alpha) {
      return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
   }

   protected void init() {
      super.init();
      this.updateDynamicLayout();
   }

   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      this.updateDynamicLayout();
      int x = this.leftPos;
      int y = this.topPos;
      Container activeContainer = ((DayZInventoryMenu)this.menu).getActiveContainer();
      String vicinityTitle = "VICINITY";
      if (activeContainer instanceof CorpseEntity) {
         CorpseEntity corpse = (CorpseEntity)activeContainer;
         String ownerName = corpse.getOwnerName();
         vicinityTitle = ownerName != null && !ownerName.isBlank() ? ownerName.toUpperCase() : "CORPSE";
      } else if (activeContainer instanceof ZombieCorpseEntity zombieCorpse) {
         vicinityTitle = zombieCorpse.getDisplayName().getString();
      } else {
         label52: {
            if (activeContainer instanceof Nameable) {
               Nameable nameable = (Nameable)activeContainer;
               if (nameable.hasCustomName()) {
                  vicinityTitle = nameable.getDisplayName().getString();
                  break label52;
               }
            }

            if (activeContainer != null) {
               if (this.title != null && !this.title.getString().equals("DayZ Inventory") && !this.title.getString().equals("screen.blockz.dayz")) {
                  vicinityTitle = this.title.getString();
               } else if (activeContainer instanceof Nameable) {
                  Nameable nameable = (Nameable)activeContainer;
                  vicinityTitle = nameable.getDisplayName().getString().toUpperCase();
               } else {
                  vicinityTitle = "CONTAINER";
               }
            }
         }
      }

      int vicinityWidth = ((DayZInventoryMenu)this.menu).getVicinityPanelWidth();
      int vicOffset = ((DayZInventoryMenu)this.menu).getVicinityOffsetX();
      int vicPanelX = 0 + vicOffset;
      this.drawPanel(graphics, x + vicPanelX, y + 0, vicinityWidth, 200, vicinityTitle);
      int pocketCount;
      int pocketRows;
      int pocketsHeight;
      if (((DayZInventoryMenu)this.menu).supportsContainerPaging()) {
         int pageCount = ((DayZInventoryMenu)this.menu).getContainerPageCount();
         int page = ((DayZInventoryMenu)this.menu).getContainerPage() + 1;
         String pageText = page + "/" + pageCount;
         int pageY = y + 0 - 10;
         int rightArrowX = x + vicPanelX + vicinityWidth - 12;
         int leftArrowX = rightArrowX - 12;
         int pageTextX = leftArrowX - 6 - this.font.width(pageText);
         graphics.drawString(this.font, "<", leftArrowX, pageY, -2236963, false);
         graphics.drawString(this.font, pageText, pageTextX, pageY, -2236963, false);
         graphics.drawString(this.font, ">", rightArrowX, pageY, -2236963, false);
      }

      int playerPanelH = 153;
      this.drawPanel(graphics, x + 172, y + 0, 96, playerPanelH, "PLAYER");
      this.renderPlayerInInventory(graphics, x + 220, y + 75, mouseX, mouseY);
      this.drawPanel(graphics, x + 172, y + 155, 96, 45, "HOTBAR");
      int invPanelW = this.getInventoryPanelWidth();
      this.drawPanel(graphics, x + 270, y + 0, invPanelW, 200, "INVENTORY");
      boolean hasBackpack = ((DayZInventoryMenu)this.menu).hasBackpack();
      this.renderSlotRangeBackground(graphics, 0, ((DayZInventoryMenu)this.menu).slots.size() - 1);
      if (((DayZInventoryMenu)this.menu).isLockedMode) {
         pocketCount = ((DayZInventoryMenu)this.menu).getPocketCount();
         pocketRows = (pocketCount + 5 - 1) / 5;
         pocketsHeight = pocketRows * 18;
         int overlayStartY = y + 10 + pocketsHeight + 2;
         int lockX = x + 270 + invPanelW / 2 - 8;
         int lockY = overlayStartY + 20;
         int overlayEndY = y + 0 + 200 - 2;
         if (overlayStartY < overlayEndY) {
            graphics.fill(x + 270 + 2, overlayStartY, x + 270 + invPanelW - 2, overlayEndY, 1610612736);
            RenderSystem.setShaderTexture(0, LOCK_ICON);
            graphics.blit(LOCK_ICON, lockX, lockY, 0.0F, 0.0F, 16, 16, 16, 16);
            graphics.drawCenteredString(this.font, "LOCKED", lockX + 8, lockY + 20, -5592406);
         }
      }

      graphics.drawString(this.font, "CRAFTING", x + 198, y + 115 - 10, -2236963, false);
      int vStart = 81;
      this.renderEquipmentCapacity(graphics, mouseX, mouseY, vStart + 5, ((DayZInventoryMenu)this.menu).backpackCapacity);
      this.renderEquipmentCapacity(graphics, mouseX, mouseY, vStart + 6, ((DayZInventoryMenu)this.menu).vestCapacity);
      this.renderEquipmentCapacity(graphics, mouseX, mouseY, vStart + 1, ((DayZInventoryMenu)this.menu).shirtCapacity);
      this.renderEquipmentCapacity(graphics, mouseX, mouseY, vStart + 2, ((DayZInventoryMenu)this.menu).pantsCapacity);
      if (!hasBackpack && ((DayZInventoryMenu)this.menu).vestCapacity == 0 && ((DayZInventoryMenu)this.menu).shirtCapacity == 0 && ((DayZInventoryMenu)this.menu).pantsCapacity == 0) {
         pocketCount = ((DayZInventoryMenu)this.menu).getPocketCount();
         pocketRows = (pocketCount + 5 - 1) / 5;
         pocketsHeight = pocketRows * 18;
         int textY = y + 10 + pocketsHeight + 20;
         graphics.drawCenteredString(this.font, "NEED STORAGE", x + 270 + invPanelW / 2, textY, 1090519039);
      }

      this.renderSlotIcons(graphics);
      this.renderTetrisFootprints(graphics);
   }

   private void renderEquipmentCapacity(GuiGraphics graphics, int mouseX, int mouseY, int slotIndex, int capacity) {
      if (capacity > 0) {
         if (slotIndex < ((DayZInventoryMenu)this.menu).slots.size()) {
            Slot slot = (Slot)((DayZInventoryMenu)this.menu).slots.get(slotIndex);
            if (this.isHovering(slot.x, slot.y, 16, 16, (double)mouseX, (double)mouseY)) {
               String capStr = String.format("CAPACITY: %d", capacity);
               int textX = this.leftPos + slot.x - 20;
               int textY = this.topPos + slot.y - 10;
               graphics.drawString(this.font, capStr, textX, textY, -256, true);
               graphics.renderOutline(this.leftPos + slot.x - 1, this.topPos + slot.y - 1, 18, 18, -256);
            }

         }
      }
   }

   private void renderAdditionalEquipmentGroupLabels(GuiGraphics graphics, int x, int scrollPixels, int minY, int maxY) {
      for (DayZInventoryMenu.AdditionalEquipmentGroupLayout group : ((DayZInventoryMenu)this.menu).getAdditionalEquipmentGroupLayouts()) {
         if (group.headerY < -500) {
            continue;
         }
         int y = group.headerY - scrollPixels;
         if (y + 8 <= minY || y >= maxY) {
            continue;
         }
         boolean collapsed = ((DayZInventoryMenu)this.menu).isAdditionalEquipmentGroupCollapsed(group.key);
         graphics.drawString(this.font, collapsed ? "▶" : "▼", x, y, -5592406, true);
         graphics.drawString(this.font, group.label, x + 10, y, -5592406, true);
      }
   }

   private void updateSlotScroll(int slotIndex, int scrollPixels, int guiMinY, int guiMaxY) {
      this.updateSlotScroll(slotIndex, scrollPixels, guiMinY, guiMaxY, false);
   }

   private void renderSlotRangeBackground(GuiGraphics graphics, int start, int end) {
      int vicMinY = this.topPos + 10 - 1;
      int vicH = 188;
      int invMinY = this.topPos + 10 - 1;
      int invH = 188;

      for(int i = start; i <= end && i < ((DayZInventoryMenu)this.menu).slots.size(); ++i) {
         Slot slot = (Slot)((DayZInventoryMenu)this.menu).slots.get(i);
         if (slot.isActive()) {
            int slotX = this.leftPos + slot.x - 1;
            int slotY = this.topPos + slot.y - 1;
            boolean isScissored = false;
            int sY;
            int outlineColor;
            int sH;
            if (i >= 0 && i < 81 || ((DayZInventoryMenu)this.menu).getCorpseStorageSlotStart() >= 0 && i >= ((DayZInventoryMenu)this.menu).getCorpseStorageSlotStart() && i <= ((DayZInventoryMenu)this.menu).getCorpseStorageSlotEnd()) {
               sY = Math.max(slotY, vicMinY);
               outlineColor = Math.min(slotY + 18, vicMinY + vicH);
               sH = outlineColor - sY;
               if (sH <= 0) {
                  continue;
               }

               this.enableScissor(slotX, sY, 18, sH);
               isScissored = true;
            } else if (i >= 90 && i <= ((DayZInventoryMenu)this.menu).getBackpackSlotEnd()) {
               sY = Math.max(slotY, invMinY);
               outlineColor = Math.min(slotY + 18, invMinY + invH);
               sH = outlineColor - sY;
               if (sH <= 0) {
                  continue;
               }

               this.enableScissor(slotX, sY, 18, sH);
               isScissored = true;
            }

            if (slot instanceof TetrisSlot && slot.hasItem()) {
               ItemStack stack = slot.getItem();
               ItemSizeManager.ItemSize size = ItemSizeManager.getSize(stack);
               if (size.width() > 1 || size.height() > 1) {
                  if (isScissored) {
                     RenderSystem.disableScissor();
                  }
                  continue;
               }
            }

            int anchorMenuSlotIndex = ((DayZInventoryMenu)this.menu).getGridAnchorMenuSlotIndex(i);
            if (anchorMenuSlotIndex != -1 && anchorMenuSlotIndex != i) {
               Slot anchorSlot = (Slot)((DayZInventoryMenu)this.menu).slots.get(anchorMenuSlotIndex);
               if (anchorSlot.hasItem()) {
                  ItemStack anchorStack = anchorSlot.getItem();
                  ItemSizeManager.ItemSize anchorSize = ItemSizeManager.getSize(anchorStack);
                  if (anchorSize.width() > 1 || anchorSize.height() > 1) {
                     if (isScissored) {
                        RenderSystem.disableScissor();
                     }
                     continue;
                  }
               }
            }

            sY = slot.hasItem() ? 822083583 : 285212671;
            graphics.fill(slotX, slotY, slotX + 18, slotY + 18, sY);
            outlineColor = slot.hasItem() ? 1627389951 : 553648127;
            graphics.renderOutline(slotX, slotY, 18, 18, outlineColor);
            if (isScissored) {
               RenderSystem.disableScissor();
            }
         }
      }

   }

   public boolean renderCustomSlot(GuiGraphics guiGraphics, Slot slot, Slot clickedSlot, ItemStack draggingItem) {
      if (!(slot instanceof TetrisSlot)) {
         return false;
      } else {
         int vicMinY = this.topPos + 10 - 1;
         int invMinY = this.topPos + 10 - 1;
         int viewportY = -1;
         int viewportH = 188;
         if (slot.index >= 90 && slot.index <= ((DayZInventoryMenu)this.menu).getBackpackSlotEnd()) {
            viewportY = invMinY;
         } else if (slot.index >= 0 && slot.index < 81 || ((DayZInventoryMenu)this.menu).getCorpseStorageSlotStart() >= 0 && slot.index >= ((DayZInventoryMenu)this.menu).getCorpseStorageSlotStart() && slot.index <= ((DayZInventoryMenu)this.menu).getCorpseStorageSlotEnd()) {
            viewportY = vicMinY;
         }

         ItemStack stack = slot.getItem();
         boolean hasItem = !stack.isEmpty();
         ItemSizeManager.ItemSize size = hasItem ? ItemSizeManager.getSize(stack) : new ItemSizeManager.ItemSize(1, 1);
         boolean isMultiSlot = size.width() > 1 || size.height() > 1;
         if (!hasItem && slot.getNoItemIcon() == null) {
            return true;
         } else {
            int areaWidth = size.width() * 18;
            int areaHeight = size.height() * 18;
            int scX = this.leftPos + slot.x - 1;
            int scY = this.topPos + slot.y - 1;
            int scH = areaHeight;
            if (viewportY != -1) {
               int viewportBottom = viewportY + viewportH;
               int newScY = Math.max(scY, viewportY);
               int newBottom = Math.min(scY + areaHeight, viewportBottom);
               int newScH = newBottom - newScY;
               if (newScH <= 0) {
                  return true;
               }

               scY = newScY;
               scH = newScH;
            }

            this.enableScissor(scX, scY, areaWidth, scH);
            if (hasItem) {
               boolean isDragging = slot == clickedSlot && !draggingItem.isEmpty();
               if (isDragging) {
                  RenderSystem.disableScissor();
                  return true;
               }

               guiGraphics.pose().pushPose();
               if (isMultiSlot) {
                  float availableW = (float)areaWidth;
                  float availableH = (float)areaHeight;
                  float scaleX = availableW / 16.0F;
                  float scaleY = availableH / 16.0F;
                  float scale = Math.min(scaleX, scaleY);
                  float renderedWidth = 16.0F * scale;
                  float renderedHeight = 16.0F * scale;
                  float offsetX = ((float)areaWidth - renderedWidth) / 2.0F;
                  float offsetY = ((float)areaHeight - renderedHeight) / 2.0F;
                  guiGraphics.pose().translate((float)(slot.x - 1) + offsetX, (float)(slot.y - 1) + offsetY, 0.0F);
                  guiGraphics.pose().scale(scale, scale, 1.0F);
                  guiGraphics.renderItem(stack, 0, 0);
                  guiGraphics.pose().popPose();
                  int decX = slot.x - 1 + areaWidth - 16;
                  int decY = slot.y - 1 + areaHeight - 16;
                  guiGraphics.renderItemDecorations(this.font, stack, decX, decY);
               } else {
                  guiGraphics.pose().translate((float)slot.x, (float)slot.y, 0.0F);
                  guiGraphics.renderItem(stack, 0, 0);
                  guiGraphics.renderItemDecorations(this.font, stack, 0, 0);
                  guiGraphics.pose().popPose();
               }
            } else {
               Pair<ResourceLocation, ResourceLocation> pair = slot.getNoItemIcon();
               if (pair != null) {
                  TextureAtlasSprite sprite = (TextureAtlasSprite)this.minecraft.getTextureAtlas((ResourceLocation)pair.getFirst()).apply((ResourceLocation)pair.getSecond());
                  guiGraphics.blit(slot.x, slot.y, 0, 16, 16, sprite);
               }
            }

            RenderSystem.disableScissor();
            return true;
         }
      }
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      int x = 274;
      int totalContentHeight = ((DayZInventoryMenu)this.menu).totalContentHeight;
      int maxScroll = totalContentHeight - 188;
      int scrollPixels = (int)(this.scrollOffs * (float)maxScroll);
      int guiMinY = 10;
      int guiMaxY = guiMinY + 188;

      // 方案B：口袋也参与滚动（滚动到底部时口袋自然不可见）
      this.renderAdditionalEquipmentGroupLabels(graphics, x, scrollPixels, guiMinY, guiMaxY);
      this.renderSectionLabel(graphics, Component.translatable("screen.blockz.pockets"), x, ((DayZInventoryMenu)this.menu).pocketsY, scrollPixels, guiMinY, guiMaxY);
      this.renderSectionLabel(graphics, Component.translatable("screen.blockz.shirt_pocket"), x, ((DayZInventoryMenu)this.menu).shirtY, scrollPixels, guiMinY, guiMaxY);
      this.renderSectionLabel(graphics, Component.translatable("screen.blockz.pants_pocket"), x, ((DayZInventoryMenu)this.menu).pantsY, scrollPixels, guiMinY, guiMaxY);
      this.renderSectionLabel(graphics, Component.translatable("screen.blockz.vest"), x, ((DayZInventoryMenu)this.menu).vestY, scrollPixels, guiMinY, guiMaxY);
      this.renderSectionLabel(graphics, Component.translatable("screen.blockz.backpack"), x, ((DayZInventoryMenu)this.menu).backpackY, scrollPixels, guiMinY, guiMaxY);
      if (maxScroll > 0) {
         int scrollX = 270 + this.getInventoryPanelWidth() - 10;
         int scrollY = 10;
         this.renderScrollbar(graphics, scrollX, scrollY, maxScroll, totalContentHeight, this.scrollOffs, 188);
      }

      if (((DayZInventoryMenu)this.menu).isCorpseMode()) {
         int vicX = 4 + ((DayZInventoryMenu)this.menu).getVicinityOffsetX();
         int vicTotalHeight = ((DayZInventoryMenu)this.menu).totalVicinityHeight;
         int vicMaxScroll = Math.max(0, vicTotalHeight - 188);
         int vicScrollPixels = (int)(this.vicinityScrollOffs * (float)vicMaxScroll);
         int vicGuiMinY = 10;
         int vicGuiMaxY = vicGuiMinY + 188;
         this.renderSectionLabel(graphics, Component.translatable("screen.blockz.backpack"), vicX, ((DayZInventoryMenu)this.menu).corpseBackpackY, vicScrollPixels, vicGuiMinY, vicGuiMaxY);
         this.renderSectionLabel(graphics, Component.translatable("screen.blockz.vest"), vicX, ((DayZInventoryMenu)this.menu).corpseVestY, vicScrollPixels, vicGuiMinY, vicGuiMaxY);
         this.renderSectionLabel(graphics, Component.translatable("screen.blockz.shirt_pocket"), vicX, ((DayZInventoryMenu)this.menu).corpseShirtY, vicScrollPixels, vicGuiMinY, vicGuiMaxY);
         this.renderSectionLabel(graphics, Component.translatable("screen.blockz.pants_pocket"), vicX, ((DayZInventoryMenu)this.menu).corpsePantsY, vicScrollPixels, vicGuiMinY, vicGuiMaxY);
         if (vicMaxScroll > 0) {
            int vicOffset = ((DayZInventoryMenu)this.menu).getVicinityOffsetX();
            int vicPanelW = ((DayZInventoryMenu)this.menu).getVicinityPanelWidth();
            int scrollX = 0 + vicOffset + vicPanelW - 14;
            int scrollY = 10;
            this.renderScrollbar(graphics, scrollX, scrollY, vicMaxScroll, vicTotalHeight, this.vicinityScrollOffs);
         }
      }

   }

   private void renderScrollbar(GuiGraphics graphics, int scrollX, int scrollY, int maxScroll, int totalHeight, float currentScrollOffs, int viewH) {
      int scrollH = viewH;
      graphics.fill(scrollX, scrollY, scrollX + 8, scrollY + scrollH, 268435456);
      int thumbH = Math.max(10, (int)((float)scrollH * ((float)scrollH / (float)totalHeight)));
      int thumbY = (int)((float)(scrollH - thumbH) * currentScrollOffs);
      thumbY = Mth.clamp(thumbY, 0, scrollH - thumbH);
      graphics.fill(scrollX + 1, scrollY + thumbY, scrollX + 7, scrollY + thumbY + thumbH, 1090519039);
      graphics.fill(scrollX + 2, scrollY + thumbY + 1, scrollX + 6, scrollY + thumbY + thumbH - 1, 1342177283);
      graphics.fill(scrollX + 3, scrollY + thumbY + 2, scrollX + 5, scrollY + thumbY + thumbH - 2, 1090519039);
   }

   private void renderScrollbar(GuiGraphics graphics, int scrollX, int scrollY, int maxScroll, int totalHeight, float currentScrollOffs) {
      this.renderScrollbar(graphics, scrollX, scrollY, maxScroll, totalHeight, currentScrollOffs, 188);
   }

   private void renderSectionLabel(GuiGraphics graphics, Component text, int x, int baseY, int scrollPixels, int minY, int maxY) {
      if (baseY >= -500) {
         int y = baseY - 12 - scrollPixels;
         if (y + 8 > minY && y < maxY) {
            graphics.drawString(this.font, text, x, y, -5592406, true);
         }

      }
   }

   private void renderLockedSlots(GuiGraphics graphics) {
      if (!ClientSettings.dayzEnabled && !this.minecraft.player.hasPermissions(2) && !(((DayZInventoryMenu)this.menu).getActiveContainer() instanceof CorpseEntity)) {
         graphics.pose().pushPose();
         graphics.pose().translate(0.0F, 0.0F, 300.0F);
         int bpStart = ((DayZInventoryMenu)this.menu).getBackpackSlotStart();
         int bpEnd = ((DayZInventoryMenu)this.menu).getBackpackSlotEnd();

         for(int i = bpStart; i <= bpEnd && i < ((DayZInventoryMenu)this.menu).slots.size(); ++i) {
            Slot slot = (Slot)((DayZInventoryMenu)this.menu).slots.get(i);
            if (slot.y >= -2000 && slot.isActive()) {
               int x = this.leftPos + slot.x;
               int y = this.topPos + slot.y;
               graphics.fill(x, y, x + 16, y + 16, Integer.MIN_VALUE);
               RenderSystem.setShaderTexture(0, LOCK_ICON);
               RenderSystem.enableBlend();
               graphics.blit(LOCK_ICON, x, y, 0.0F, 0.0F, 16, 16, 16, 16);
            }
         }

         graphics.pose().popPose();
      }
   }

   private void renderPlayerInInventory(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
      if (this.minecraft.player != null) {
         graphics.pose().pushPose();
         InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x, y, 32, (float)x - (float)mouseX, (float)y - 50.0F - (float)mouseY, this.minecraft.player);
         graphics.pose().popPose();
      }
   }

   private void renderSlotIcons(GuiGraphics graphics) {
      for(int slotId = 0; slotId < ((DayZInventoryMenu)this.menu).slots.size(); ++slotId) {
         Slot slot = (Slot)((DayZInventoryMenu)this.menu).slots.get(slotId);
         if (slot.isActive() && !slot.hasItem()) {
            int slotX = this.leftPos + slot.x;
            int slotY = this.topPos + slot.y;
            if (this.renderCuriosSlotIcon(graphics, slotId, slotX, slotY)) {
               continue;
            }
            ResourceLocation icon = this.getSlotIcon(slotId);
            if (icon != null) {
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.4F);
               graphics.blit(icon, slotX, slotY, 0.0F, 0.0F, 16, 16, 16, 16);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
         }
      }

   }

   private boolean renderCuriosSlotIcon(GuiGraphics graphics, int slotId, int slotX, int slotY) {
      if (this.minecraft.player == null) {
         return false;
      }
      String dynamicSlotId = ((DayZInventoryMenu)this.menu).getAdditionalEquipmentSlotId(slotId);
      if (dynamicSlotId == null || dynamicSlotId.isBlank()) {
         return false;
      }
      ResourceLocation icon = CuriosIntegration.getSlotIcon(this.minecraft.player, dynamicSlotId);
      if (icon == null || CuriosIntegration.isGenericSlotIcon(icon)) {
         return false;
      }
      TextureAtlasSprite sprite = (TextureAtlasSprite)this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(icon);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.4F);
      graphics.blit(slotX, slotY, 0, 16, 16, sprite);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

   private ResourceLocation getSlotIcon(int slotId) {
      int vSlots = 81;
      if (slotId >= 0 && slotId < vSlots) {
         if (((DayZInventoryMenu)this.menu).getActiveContainer() instanceof ZombieCorpseEntity) {
            if (slotId == 4) {
               return UITextures.SLOT_HEADWEAR;
            }

            if (slotId == 7) {
               return UITextures.SLOT_MASK;
            }

            if (slotId == 2) {
               return UITextures.SLOT_SHIRT;
            }

            if (slotId == 1) {
               return UITextures.SLOT_VEST;
            }

            if (slotId == 8) {
               return UITextures.SLOT_GLOVES;
            }

            if (slotId == 3) {
               return UITextures.SLOT_PANTS;
            }

            if (slotId == 5) {
               return UITextures.SLOT_SHOES;
            }

            if (slotId == 0) {
               return UITextures.SLOT_BACKPACK;
            }
         } else if (((DayZInventoryMenu)this.menu).getActiveContainer() instanceof CorpseEntity) {
            if (slotId == 4) {
               return UITextures.SLOT_HEADWEAR;
            }

            if (slotId == 7) {
               return UITextures.SLOT_MASK;
            }

            if (slotId == 2) {
               return UITextures.SLOT_SHIRT;
            }

            if (slotId == 1) {
               return UITextures.SLOT_VEST;
            }

            if (slotId == 6) {
               return UITextures.SLOT_OFFHAND;
            }

            if (slotId == 8) {
               return UITextures.SLOT_GLOVES;
            }

            if (slotId == 3) {
               return UITextures.SLOT_PANTS;
            }

            if (slotId == 5) {
               return UITextures.SLOT_SHOES;
            }

            if (slotId == 0) {
               return UITextures.SLOT_BACKPACK;
            }
         }

         return null;
      } else if (slotId == vSlots + 0) {
         return UITextures.SLOT_HEADWEAR;
      } else if (slotId == vSlots + 1) {
         return UITextures.SLOT_SHIRT;
      } else if (slotId == vSlots + 2) {
         return UITextures.SLOT_PANTS;
      } else if (slotId == vSlots + 3) {
         return UITextures.SLOT_SHOES;
      } else if (slotId == vSlots + 4) {
         return UITextures.SLOT_OFFHAND;
      } else if (slotId == vSlots + 5) {
         return UITextures.SLOT_BACKPACK;
      } else if (slotId == vSlots + 6) {
         return UITextures.SLOT_VEST;
      } else if (slotId == vSlots + 7) {
         return UITextures.SLOT_GLOVES;
      } else if (slotId == vSlots + 8) {
         return UITextures.SLOT_MASK;
      } else {
         String dynamicSlotId = ((DayZInventoryMenu)this.menu).getAdditionalEquipmentSlotId(slotId);
         if (dynamicSlotId == null) {
            return null;
         }
         String normalized = dynamicSlotId.toLowerCase();
         if (normalized.contains("mask") || normalized.contains("face") || normalized.contains("mouth") || normalized.contains("respir")) {
            return UITextures.SLOT_MASK;
         }
         if (normalized.contains("glove") || normalized.contains("hand") || normalized.contains("wrist")) {
            return UITextures.SLOT_GLOVES;
         }
         if (normalized.contains("boot") || normalized.contains("shoe") || normalized.contains("feet") || normalized.contains("ankle")) {
            return UITextures.SLOT_SHOES;
         }
         if (normalized.contains("pant") || normalized.contains("leg") || normalized.contains("waist")) {
            return UITextures.SLOT_PANTS;
         }
         if (normalized.contains("vest") || normalized.contains("body") || normalized.contains("chest") || normalized.contains("belt")) {
            return UITextures.SLOT_VEST;
         }
         if (normalized.contains("back") || normalized.contains("pack")) {
            return UITextures.SLOT_BACKPACK;
         }
         if (normalized.contains("offhand") || normalized.contains("shield")) {
            return UITextures.SLOT_OFFHAND;
         }
         return UITextures.SLOT_HEADWEAR;
      }
   }

   private void drawPanel(GuiGraphics graphics, int x, int y, int w, int h, String title) {
      graphics.fill(x, y, x + w, y + h, -1342177280);
      graphics.fill(x, y - 1, x + w, y, -11184811);
      graphics.fill(x, y + h, x + w, y + h + 1, -11184811);
      graphics.fill(x - 1, y, x, y + h, -11184811);
      graphics.fill(x + w, y, x + w + 1, y + h, -11184811);
      graphics.fill(x, y - 12, x + w, y, -872415232);
      graphics.drawString(this.font, title, x + 4, y - 10, -2236963, false);
   }

   private void renderTetrisFootprints(GuiGraphics graphics) {
      int invScissorX = this.leftPos + 270;
      int invScissorY = this.topPos + 10 - 1;
      int panelW = this.getInventoryPanelWidth();
      int viewH = 190;
      graphics.enableScissor(invScissorX, invScissorY, invScissorX + panelW, invScissorY + viewH);
      this.renderFootprintsForRange(graphics, ((DayZInventoryMenu)this.menu).getBackpackSlotStart(), ((DayZInventoryMenu)this.menu).getBackpackSlotEnd(), ((DayZInventoryMenu)this.menu).getInventoryMaxCols(), true);
      graphics.disableScissor();
      int corpseStart = ((DayZInventoryMenu)this.menu).getCorpseStorageSlotStart();
      int corpseEnd = ((DayZInventoryMenu)this.menu).getCorpseStorageSlotEnd();
      if (corpseStart >= 0 && corpseEnd >= corpseStart) {
         int vicScissorX = this.leftPos + 0 + ((DayZInventoryMenu)this.menu).getVicinityOffsetX();
         int vicScissorY = this.topPos + 10 - 1;
         int vicW = ((DayZInventoryMenu)this.menu).getVicinityPanelWidth();
         graphics.enableScissor(vicScissorX, vicScissorY, vicScissorX + vicW, vicScissorY + viewH);
         this.renderFootprintsForRange(graphics, corpseStart, corpseEnd, ((DayZInventoryMenu)this.menu).getVicinityCols(), true);
         graphics.disableScissor();
      }
      if (((DayZInventoryMenu)this.menu).isZombieCorpseLootMenuSlot(((DayZInventoryMenu)this.menu).getZombieCorpseLootMenuStart())) {
         int vicScissorX = this.leftPos + 0 + ((DayZInventoryMenu)this.menu).getVicinityOffsetX();
         int vicScissorY = this.topPos + 10 - 1;
         int vicW = ((DayZInventoryMenu)this.menu).getVicinityPanelWidth();
         graphics.enableScissor(vicScissorX, vicScissorY, vicScissorX + vicW, vicScissorY + viewH);
         this.renderFootprintsForRange(graphics, ((DayZInventoryMenu)this.menu).getZombieCorpseLootMenuStart(), ((DayZInventoryMenu)this.menu).getZombieCorpseLootMenuEnd(), ((DayZInventoryMenu)this.menu).getVicinityCols(), false);
         graphics.disableScissor();
      }

   }

   private void renderFootprintsForRange(GuiGraphics graphics, int startIdx, int endIdx, int cols, boolean isInventory) {
      for(int i = startIdx; i <= endIdx && i < ((DayZInventoryMenu)this.menu).slots.size(); ++i) {
         Slot slot = (Slot)((DayZInventoryMenu)this.menu).slots.get(i);
         if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            ItemSizeManager.ItemSize size = ItemSizeManager.getSize(stack);
            if (size.width() > 1 || size.height() > 1) {
               int slotX = this.leftPos + slot.x - 1;
               int slotY = this.topPos + slot.y - 1;
               int pixelW = size.width() * 18;
               int pixelH = size.height() * 18;
               int fillColor = getGridFillColor(stack, 0x60808080);
               int outlineColor = getGridOutlineColor(stack, 0xFF5A5A5A);
               graphics.fill(slotX, slotY, slotX + pixelW, slotY + pixelH, fillColor);
               graphics.renderOutline(slotX, slotY, pixelW, pixelH, outlineColor);
            }
         }
      }

   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.renderHoveredMultiSlotHighlight(graphics);
      this.renderCarriedItemPreview(graphics, mouseX, mouseY);
      Slot hoveredSlot = this.hoveredSlot;
      if (hoveredSlot != null && hoveredSlot.hasItem() && ((DayZInventoryMenu)this.menu).getCarried().isEmpty()) {
         ItemStack stack = hoveredSlot.getItem();
         String name = stack.getHoverName().getString();
         graphics.drawString(this.font, name, mouseX + 12, mouseY - 12, -1, true);
      } else if (hoveredSlot != null && !hoveredSlot.hasItem() && ((DayZInventoryMenu)this.menu).getCarried().isEmpty()) {
         String slotLabel = ((DayZInventoryMenu)this.menu).getAdditionalEquipmentSlotLabel(hoveredSlot.index);
         if (slotLabel != null && !slotLabel.isBlank()) {
            graphics.drawString(this.font, slotLabel, mouseX + 12, mouseY - 12, -1, true);
         }
      }

      if (((DayZInventoryMenu)this.menu).isEnchantingTable) {
         this.renderEnchantingInfo(graphics, mouseX, mouseY);
      }

      this.renderLockedSlots(graphics);
      this.renderTooltip(graphics, mouseX, mouseY);
   }

   private void renderHoveredMultiSlotHighlight(GuiGraphics graphics) {
      Slot hoveredSlot = this.hoveredSlot;
      if (hoveredSlot == null) {
         return;
      }
      DayZInventoryMenu menu = (DayZInventoryMenu)this.menu;
      if (!menu.getCarried().isEmpty()) {
         return;
      }
      int anchorMenuSlotIndex = menu.getGridAnchorMenuSlotIndex(hoveredSlot.index);
      if (anchorMenuSlotIndex == -1 || anchorMenuSlotIndex >= menu.slots.size()) {
         return;
      }
      Slot anchorSlot = (Slot)menu.slots.get(anchorMenuSlotIndex);
      if (!anchorSlot.hasItem()) {
         return;
      }
      ItemStack stack = anchorSlot.getItem();
      ItemSizeManager.ItemSize size = ItemSizeManager.getSize(stack);
      int width = Math.max(1, size.width());
      int height = Math.max(1, size.height());
      if (width <= 1 && height <= 1) {
         return;
      }

      int slotX = this.leftPos + anchorSlot.x - 1;
      int slotY = this.topPos + anchorSlot.y - 1;
      int pixelW = width * 18;
      int pixelH = height * 18;
      graphics.pose().pushPose();
      graphics.pose().translate(0.0F, 0.0F, 260.0F);
      graphics.fill(slotX, slotY, slotX + pixelW, slotY + pixelH, MULTI_SLOT_HOVER_FILL);
      graphics.renderOutline(slotX, slotY, pixelW, pixelH, MULTI_SLOT_HOVER_OUTLINE);
      graphics.pose().popPose();
   }

   private void renderCarriedItemPreview(GuiGraphics graphics, int mouseX, int mouseY) {
      ItemStack carried = ((DayZInventoryMenu)this.menu).getCarried();
      Slot hoveredSlot = this.hoveredSlot;
      if (!carried.isEmpty() && hoveredSlot != null) {
         DayZInventoryMenu menu = (DayZInventoryMenu)this.menu;
         ItemSizeManager.ItemSize size = ItemSizeManager.getSize(carried);
         int w = size.width();
         int h = size.height();
         boolean fits = true;
         int id = hoveredSlot.index;
         boolean isGridArea = id >= menu.getBackpackSlotStart() && id <= menu.getBackpackSlotEnd()
                 || menu.getCorpseStorageSlotStart() >= 0 && id >= menu.getCorpseStorageSlotStart() && id <= menu.getCorpseStorageSlotEnd()
                 || menu.isZombieCorpseLootMenuSlot(id);
         int previewW = w;
         int previewH = h;
         Slot previewSlot = hoveredSlot;
         if (isGridArea) {
            int previewMenuSlotIndex = menu.getCenteredPreviewAnchorMenuSlotIndex(id, carried);
            if (previewMenuSlotIndex >= 0 && previewMenuSlotIndex < menu.slots.size()) {
               previewSlot = (Slot)menu.slots.get(previewMenuSlotIndex);
            }
            fits = previewSlot.mayPlace(carried);
         } else {
            previewW = 1;
            previewH = 1;
            fits = hoveredSlot.mayPlace(carried);
         }

         int slotX = this.leftPos + previewSlot.x - 1;
         int slotY = this.topPos + previewSlot.y - 1;
         int pixelW = previewW * 18;
         int pixelH = previewH * 18;
         int color = getPreviewFillColor(carried, fits);
         graphics.pose().pushPose();
         graphics.pose().translate(0.0F, 0.0F, 250.0F);
         graphics.renderOutline(slotX, slotY, pixelW, pixelH, color | -16777216);
         graphics.fill(slotX, slotY, slotX + pixelW, slotY + pixelH, color);
         graphics.pose().popPose();
      }
   }

   private void renderEnchantingInfo(GuiGraphics graphics, int mouseX, int mouseY) {
      int centerX = this.leftPos + 0 + 48;
      int startY = this.topPos + 10 + 80;

      for(int i = 0; i < 3; ++i) {
         int y = startY + i * 20;
         int cost = ((DayZInventoryMenu)this.menu).costs[i];
         int clue = ((DayZInventoryMenu)this.menu).enchantClue[i];
         int level = ((DayZInventoryMenu)this.menu).levelClue[i];
         if (cost == 0) {
            graphics.fill(centerX - 40, y, centerX + 40, y + 18, 1342177280);
            graphics.drawString(this.font, "---", centerX - 6, y + 5, -11184811, false);
         } else {
            boolean hovered = mouseX >= centerX - 40 && mouseX <= centerX + 40 && mouseY >= y && mouseY <= y + 18;
            int color = hovered ? -2130706433 : 1342177280;
            graphics.fill(centerX - 40, y, centerX + 40, y + 18, color);
            graphics.renderOutline(centerX - 40, y, 80, 18, -5592406);
            String text = "LVL " + cost;
            graphics.drawString(this.font, text, centerX - 35, y + 5, -11141291, true);
            if (hovered && clue >= 0) {
               Enchantment enchant = (Enchantment)RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).registry(Registries.ENCHANTMENT).get().byId(clue);
               if (enchant != null) {
                  MutableComponent clueText = enchant.getFullname(level).copy();
                  graphics.renderTooltip(this.font, clueText, mouseX, mouseY);
               }
            }
         }
      }

   }

   protected void containerTick() {
      super.containerTick();
      ((DayZInventoryMenu)this.menu).updateSlotPositions();
      this.applyInventoryScroll();
      this.applyVicinityScroll();
   }

   private void applyInventoryScroll() {
      int totalContentHeight = ((DayZInventoryMenu)this.menu).totalContentHeight;
      int maxScroll = totalContentHeight - 188;
      if (maxScroll <= 0) {
         this.scrollOffs = 0.0F;
      }

      int scrollPixels = (int)(this.scrollOffs * (float)maxScroll);
      int guiMinY = 10;
      int guiMaxY = guiMinY + 188;
      // 方案B：口袋 + 扩展区一起滚动
      int startIdx = ((DayZInventoryMenu)this.menu).getScrollableInventoryStart();
      int endIdx = ((DayZInventoryMenu)this.menu).getBackpackSlotEnd();

      for(int i = startIdx; i <= endIdx; ++i) {
         this.updateSlotScroll(i, scrollPixels, guiMinY, guiMaxY, false);
      }

   }

   private void applyVicinityScroll() {
      int totalVicinityHeight = ((DayZInventoryMenu)this.menu).totalVicinityHeight;
      int maxScroll = totalVicinityHeight - 188;
      if (maxScroll <= 0) {
         this.vicinityScrollOffs = 0.0F;
      }

      int scrollPixels = (int)(this.vicinityScrollOffs * (float)maxScroll);
      int guiMinY = 10;
      int guiMaxY = guiMinY + 188;

      int corpseStart;
      for(corpseStart = 0; corpseStart < 81; ++corpseStart) {
         this.updateSlotScroll(corpseStart, scrollPixels, guiMinY, guiMaxY, false);
      }

      corpseStart = ((DayZInventoryMenu)this.menu).getCorpseStorageSlotStart();
      int corpseEnd = ((DayZInventoryMenu)this.menu).getCorpseStorageSlotEnd();
      if (corpseStart >= 0 && corpseEnd >= corpseStart) {
         for(int i = corpseStart; i <= corpseEnd; ++i) {
            this.updateSlotScroll(i, scrollPixels, guiMinY, guiMaxY, false);
         }
      }

   }

   private void updateSlotScroll(int slotIndex, int scrollPixels, int guiMinY, int guiMaxY, boolean strictTop) {
      if (slotIndex < ((DayZInventoryMenu)this.menu).slots.size()) {
         Slot slot = (Slot)((DayZInventoryMenu)this.menu).slots.get(slotIndex);
         if (slot.y >= -2000) {
            int newY = slot.y - scrollPixels;
            if (strictTop && newY < guiMinY) {
               this.setSlotPos(slot, -10000, -10000);
               return;
            }

            if (newY + 16 >= guiMinY && newY <= guiMaxY) {
               this.setSlotPos(slot, slot.x, newY);
            } else {
               this.setSlotPos(slot, -10000, -10000);
            }

         }
      }
   }

   private void setSlotPos(Slot slot, int x, int y) {
      try {
         if (SLOT_X_FIELD != null) {
            SLOT_X_FIELD.setInt(slot, x);
         }

         if (SLOT_Y_FIELD != null) {
            SLOT_Y_FIELD.setInt(slot, y);
         }
      } catch (Exception var5) {
      }

   }

   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      int vicOffset = ((DayZInventoryMenu)this.menu).getVicinityOffsetX();
      int vicPanelW = ((DayZInventoryMenu)this.menu).getVicinityPanelWidth();
      int vicStartX = this.leftPos + 0 + vicOffset;
      boolean overVicinity = mouseX >= (double)vicStartX && mouseX < (double)(vicStartX + vicPanelW);
      boolean overInventory = mouseX >= (double)(this.leftPos + 270) && mouseX < (double)(this.leftPos + 270 + this.getInventoryPanelWidth());
      boolean overPanelY = mouseY >= (double)(this.topPos + 0) && mouseY < (double)(this.topPos + 0 + 200);
      if (overPanelY) {
         int totalContentHeight;
         int i;
         if (overVicinity) {
            totalContentHeight = ((DayZInventoryMenu)this.menu).totalVicinityHeight;
            if (totalContentHeight > 188) {
               i = totalContentHeight - 188;
               this.vicinityScrollOffs = (float)((double)this.vicinityScrollOffs - delta / (double)i * 16.0D);
               this.vicinityScrollOffs = Mth.clamp(this.vicinityScrollOffs, 0.0F, 1.0F);
               ((DayZInventoryMenu)this.menu).updateSlotPositions();
               this.applyVicinityScroll();
               this.applyInventoryScroll();
               return true;
            }
         } else if (overInventory) {
            totalContentHeight = ((DayZInventoryMenu)this.menu).totalContentHeight;
            if (totalContentHeight > 188) {
               i = totalContentHeight - 188;
               this.scrollOffs = (float)((double)this.scrollOffs - delta / (double)i * 8.0D);
               this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
               ((DayZInventoryMenu)this.menu).updateSlotPositions();
               this.applyInventoryScroll();
               this.applyVicinityScroll();
               return true;
            }
         }
      }

      return super.mouseScrolled(mouseX, mouseY, delta);
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      int j;
      int k;
      if (this.isScrolling) {
         j = this.topPos + 10;
         k = j + 188;
         this.scrollOffs = ((float)mouseY - (float)j - 7.5F) / ((float)(k - j) - 15.0F);
         this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
         ((DayZInventoryMenu)this.menu).updateSlotPositions();
         this.applyInventoryScroll();
         this.applyVicinityScroll();
         return true;
      } else if (this.isVicinityScrolling) {
         j = this.topPos + 10;
         k = j + 188;
         this.vicinityScrollOffs = ((float)mouseY - (float)j - 7.5F) / ((float)(k - j) - 15.0F);
         this.vicinityScrollOffs = Mth.clamp(this.vicinityScrollOffs, 0.0F, 1.0F);
         ((DayZInventoryMenu)this.menu).updateSlotPositions();
         this.applyVicinityScroll();
         this.applyInventoryScroll();
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
      }
   }

   protected boolean hasClickedOutside(double mouseX, double mouseY, int leftPos, int topPos, int button) {
      int minX = leftPos + 0;
      int maxX = leftPos + 270 + this.getInventoryPanelWidth();
      int minY = topPos + 0;
      int maxY = topPos + 0 + 200;
      boolean inside = mouseX >= (double)minX && mouseX < (double)maxX && mouseY >= (double)minY && mouseY < (double)maxY;
      return !inside;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.clickStartedInVicinitySlot = false;
      if (button == 0 && ((DayZInventoryMenu)this.menu).supportsContainerPaging() && this.handleVicinityPageClick(mouseX, mouseY)) {
         return true;
      } else if (button == 0 && this.handleAdditionalEquipmentGroupClick(mouseX, mouseY)) {
         return true;
      } else {
         int startY;
         int i;
         int y;
         if (button == 0 || button == 1) {
            Slot slot = this.findSlotAt(mouseX, mouseY);
            if (slot != null && slot.isActive()) {
               startY = slot.index;
               i = ((DayZInventoryMenu)this.menu).getCorpseStorageSlotStart();
               y = ((DayZInventoryMenu)this.menu).getCorpseStorageSlotEnd();
               if (startY >= 0 && startY < 81) {
                  this.clickStartedInVicinitySlot = true;
               } else if (i >= 0 && startY >= i && startY <= y) {
                  this.clickStartedInVicinitySlot = true;
               }
            }
         }

         int centerX;
         if (button == 0) {
            centerX = this.leftPos + 270 + this.getInventoryPanelWidth() - 14;
            startY = this.topPos + 10;
            int w = 8;
            int h = 188;
            if (mouseX >= (double)centerX && mouseX < (double)(centerX + w) && mouseY >= (double)startY && mouseY < (double)(startY + h)) {
               this.isScrolling = this.totalContentSizeGreaterThanView();
               if (this.isScrolling) {
                  return true;
               }
            }

            int vicOffset = ((DayZInventoryMenu)this.menu).getVicinityOffsetX();
            int vicPanelW = ((DayZInventoryMenu)this.menu).getVicinityPanelWidth();
            int vx = this.leftPos + 0 + vicOffset + vicPanelW - 14;
            int vy = this.topPos + 10;
            if (mouseX >= (double)vx && mouseX < (double)(vx + w) && mouseY >= (double)vy && mouseY < (double)(vy + h)) {
               this.isVicinityScrolling = ((DayZInventoryMenu)this.menu).totalVicinityHeight > 188;
               if (this.isVicinityScrolling) {
                  return true;
               }
            }
         }

         if (((DayZInventoryMenu)this.menu).isEnchantingTable && button == 0) {
            centerX = this.leftPos + 0 + 48;
            startY = this.topPos + 10 + 80;

            for(i = 0; i < 3; ++i) {
               y = startY + i * 20;
               if (mouseX >= (double)(centerX - 40) && mouseX <= (double)(centerX + 40) && mouseY >= (double)y && mouseY <= (double)(y + 18) && ((DayZInventoryMenu)this.menu).costs[i] > 0) {
                  this.minecraft.gameMode.handleInventoryButtonClick(((DayZInventoryMenu)this.menu).containerId, i);
                  return true;
               }
            }
         }

         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   private boolean handleAdditionalEquipmentGroupClick(double mouseX, double mouseY) {
      int totalContentHeight = ((DayZInventoryMenu)this.menu).totalContentHeight;
      int maxScroll = totalContentHeight - 188;
      int scrollPixels = (int)(this.scrollOffs * (float)Math.max(0, maxScroll));
      int textX = this.leftPos + 274;
      for (DayZInventoryMenu.AdditionalEquipmentGroupLayout group : ((DayZInventoryMenu)this.menu).getAdditionalEquipmentGroupLayouts()) {
         if (group.headerY < -500) {
            continue;
         }
         int y = this.topPos + group.headerY - scrollPixels;
         int labelWidth = 10 + this.font.width(group.label);
         if (mouseX >= (double)textX && mouseX <= (double)(textX + labelWidth) && mouseY >= (double)y && mouseY <= (double)(y + 10)) {
            ((DayZInventoryMenu)this.menu).toggleAdditionalEquipmentGroupCollapsed(group.key);
            this.applyInventoryScroll();
            return true;
         }
      }
      return false;
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.isScrolling = false;
         this.isVicinityScrolling = false;
      }

      if (button == 0 || button == 1) {
         ItemStack carried = ((DayZInventoryMenu)this.menu).getCarried();
         if (!carried.isEmpty()) {
            int vicX = 0 + ((DayZInventoryMenu)this.menu).getVicinityOffsetX();
            int vicW = ((DayZInventoryMenu)this.menu).getVicinityPanelWidth();
            int vicY = 0;
            int vicH = 200;
            if (!this.clickStartedInVicinitySlot && this.isHovering(vicX, vicY, vicW, vicH, mouseX, mouseY)) {
               Slot slot = this.findSlotAt(mouseX, mouseY);
               if (slot == null || !slot.isActive()) {
                  this.minecraft.gameMode.handleInventoryMouseClick(((DayZInventoryMenu)this.menu).containerId, -999, button, ClickType.PICKUP, this.minecraft.player);
                  this.clickStartedInVicinitySlot = false;
                  return true;
               }
            }
         }
      }

      this.clickStartedInVicinitySlot = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   private Slot findSlotAt(double mouseX, double mouseY) {
      for(int i = 0; i < ((DayZInventoryMenu)this.menu).slots.size(); ++i) {
         Slot slot = (Slot)((DayZInventoryMenu)this.menu).slots.get(i);
         if (slot.isActive() && this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
            return slot;
         }
      }

      return null;
   }

   private boolean handleVicinityPageClick(double mouseX, double mouseY) {
      int vicOffset = ((DayZInventoryMenu)this.menu).getVicinityOffsetX();
      int vicPanelW = ((DayZInventoryMenu)this.menu).getVicinityPanelWidth();
      int vicPanelX = this.leftPos + 0 + vicOffset;
      int titleY = this.topPos + 0 - 12;
      int rightArrowX = vicPanelX + vicPanelW - 12;
      int leftArrowX = rightArrowX - 12;
      int buttonW = 8;
      int buttonH = 10;
      if (mouseY >= (double)titleY && mouseY <= (double)(titleY + buttonH)) {
         if (mouseX >= (double)leftArrowX && mouseX <= (double)(leftArrowX + buttonW)) {
            ((DayZInventoryMenu)this.menu).setContainerPage(((DayZInventoryMenu)this.menu).getContainerPage() - 1);
            this.minecraft.gameMode.handleInventoryButtonClick(((DayZInventoryMenu)this.menu).containerId, 100);
            this.vicinityScrollOffs = 0.0F;
            ((DayZInventoryMenu)this.menu).updateSlotPositions();
            this.applyVicinityScroll();
            this.applyInventoryScroll();
            return true;
         }

         if (mouseX >= (double)rightArrowX && mouseX <= (double)(rightArrowX + buttonW)) {
            ((DayZInventoryMenu)this.menu).setContainerPage(((DayZInventoryMenu)this.menu).getContainerPage() + 1);
            this.minecraft.gameMode.handleInventoryButtonClick(((DayZInventoryMenu)this.menu).containerId, 101);
            this.vicinityScrollOffs = 0.0F;
            ((DayZInventoryMenu)this.menu).updateSlotPositions();
            this.applyVicinityScroll();
            this.applyInventoryScroll();
            return true;
         }
      }

      return false;
   }

   private boolean totalContentSizeGreaterThanView() {
      return ((DayZInventoryMenu)this.menu).totalContentHeight > 188;
   }

   private int getInventoryPanelWidth() {
      int maxCols = ((DayZInventoryMenu)this.menu).getInventoryMaxCols();
      int extraCols = Math.max(0, maxCols - UIConstants.INVENTORY_COLS);
      return UIConstants.INVENTORY_PANEL_W + extraCols * UIConstants.SLOT_PITCH;
   }

   private void updateDynamicLayout() {
      int maxCols = ((DayZInventoryMenu)this.menu).getInventoryMaxCols();
      int extraCols = Math.max(0, maxCols - UIConstants.INVENTORY_COLS);
      int dynamicWidth = UIConstants.WIDTH + extraCols * UIConstants.SLOT_PITCH;
      if (this.imageWidth != dynamicWidth) {
         this.imageWidth = dynamicWidth;
      }

      int vicOffset = ((DayZInventoryMenu)this.menu).getVicinityOffsetX();
      int vicLeft = 0 + vicOffset;
      int right = dynamicWidth;
      int centerNorm = (vicLeft + right) / 2;
      this.leftPos = this.width / 2 - centerNorm;
      this.topPos = (this.height - 200) / 2;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (ModKeyMappings.ROTATE_ITEM != null && ModKeyMappings.ROTATE_ITEM.matches(keyCode, scanCode)) {
         ItemStack carried = ((DayZInventoryMenu)this.menu).getCarried();
         if (!carried.isEmpty()) {
            ItemSizeManager.toggleRotation(carried);
            NetworkHandler.CHANNEL.sendToServer(new RotateItemC2S());
            return true;
         }
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   private void enableScissor(int x, int y, int width, int height) {
      Minecraft mc = Minecraft.getInstance();
      double scale = mc.getWindow().getGuiScale();
      int sx = (int)((double)x * scale);
      int sy = (int)((double)mc.getWindow().getHeight() - (double)(y + height) * scale);
      int endX = (int)((double)(x + width) * scale);
      int topY = (int)((double)mc.getWindow().getHeight() - (double)y * scale);
      int sw = Math.max(0, endX - sx);
      int sh = Math.max(0, topY - sy);
      RenderSystem.enableScissor(sx, sy, sw, sh);
   }

   static {
      try {
         try {
            SLOT_X_FIELD = ObfuscationReflectionHelper.findField(Slot.class, "f_40220_");
            SLOT_Y_FIELD = ObfuscationReflectionHelper.findField(Slot.class, "f_40221_");
         } catch (Exception var1) {
            SLOT_X_FIELD = Slot.class.getDeclaredField("x");
            SLOT_Y_FIELD = Slot.class.getDeclaredField("y");
         }

         SLOT_X_FIELD.setAccessible(true);
         SLOT_Y_FIELD.setAccessible(true);
      } catch (Exception var2) {
         var2.printStackTrace();
      }

   }
}

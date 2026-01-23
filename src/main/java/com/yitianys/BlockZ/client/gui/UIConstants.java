package com.yitianys.BlockZ.client.gui;

public class UIConstants {
    // 总尺寸 (Reduced from 320x240 to 280x200 to fix "too big" issue)
    public static final int WIDTH = 280;
    public static final int HEIGHT = 200;
    public static final int GUI_WIDTH = WIDTH;
    public static final int GUI_HEIGHT = HEIGHT;

    // 面板尺寸 (Restored to original height for space)
    public static final int PANEL_W = 96;
    public static final int PANEL_H = 200; // Increased to 200 to prevent overlap
    
    // 面板 X 坐标 (居中布局)
    public static final int VICINITY_X = (WIDTH - (PANEL_W * 3 + 4)) / 2;
    public static final int PLAYER_X = VICINITY_X + PANEL_W + 2;
    public static final int INVENTORY_X = PLAYER_X + PANEL_W + 2;

    // 面板 Y 坐标
    public static final int PANEL_Y = (HEIGHT - PANEL_H) / 2;

    // 槽位尺寸
    public static final int SLOT_SIZE = 18;
    public static final int SLOT_PITCH = 18;

    // 快捷栏 (Restored to 5-column grid style)
    public static final int HOTBAR_W = PANEL_W;
    public static final int HOTBAR_H = 45;
    public static final int HOTBAR_X = PLAYER_X;
    public static final int HOTBAR_Y = PANEL_Y + PANEL_H - HOTBAR_H;

    // 护甲槽位 (Restored positions)
    public static final int SLOT_HEADGEAR_X = PLAYER_X + 6;
    public static final int SLOT_HEADGEAR_Y = PANEL_Y + 12;
    
    public static final int SLOT_MASK_X = PLAYER_X + 6;
    public static final int SLOT_MASK_Y = SLOT_HEADGEAR_Y + SLOT_PITCH + 1;
    
    public static final int SLOT_SHIRT_X = PLAYER_X + 6;
    public static final int SLOT_SHIRT_Y = SLOT_MASK_Y + SLOT_PITCH + 1;
    
    public static final int SLOT_VEST_X = PLAYER_X + 6;
    public static final int SLOT_VEST_Y = SLOT_SHIRT_Y + SLOT_PITCH + 1;
    
    public static final int OFFHAND_X = PLAYER_X + 6;
    public static final int OFFHAND_Y = SLOT_VEST_Y + SLOT_PITCH + 1;

    // 右列
    public static final int SLOT_GLOVES_X = PLAYER_X + PANEL_W - 24;
    public static final int SLOT_GLOVES_Y = PANEL_Y + 12;
    
    public static final int SLOT_PANTS_X = PLAYER_X + PANEL_W - 24;
    public static final int SLOT_PANTS_Y = SLOT_GLOVES_Y + SLOT_PITCH + 1;
    
    public static final int SLOT_SHOES_X = PLAYER_X + PANEL_W - 24;
    public static final int SLOT_SHOES_Y = SLOT_PANTS_Y + SLOT_PITCH + 1;
    
    public static final int BACKPACK_EQUIP_X = PLAYER_X + PANEL_W - 24;
    public static final int BACKPACK_EQUIP_Y = SLOT_SHOES_Y + SLOT_PITCH + 1;

    // Crafting Slots (Restored to Player Panel, positioned between Equipment and Hotbar)
    // Equipment ends at Y ~ 100. Hotbar starts at Y ~ 155 (200-45).
    // Space available: 100 to 155.
    // Crafting is 36px high.
    // Center it at Y ~ 115.
    public static final int CRAFTING_X = PLAYER_X + 26; 
    public static final int CRAFTING_Y = PANEL_Y + 115; 
    public static final int CRAFTING_RESULT_X = PLAYER_X + 26 + 36 + 8;
    public static final int CRAFTING_RESULT_Y = CRAFTING_Y + 10;

    // Player Model Position (Centered in Equipment area)
    public static final int PLAYER_MODEL_X = PLAYER_X + PANEL_W / 2;
    public static final int PLAYER_MODEL_Y = PANEL_Y + 75; // Moved down from 65 to 75

    // 物品栏布局 (Restored to 5 columns)
    public static final int INVENTORY_COLS = 5;
    public static final int INVENTORY_SLOTS_X = INVENTORY_X + 4;
    public static final int INVENTORY_SLOTS_Y = PANEL_Y + 10;

    // Vicinity 槽位 (Restored to 5 columns)
    public static final int VICINITY_COLS = 5;
    public static final int VICINITY_SLOTS_X = VICINITY_X + 4;
    public static final int VICINITY_SLOTS_Y = PANEL_Y + 10;
}

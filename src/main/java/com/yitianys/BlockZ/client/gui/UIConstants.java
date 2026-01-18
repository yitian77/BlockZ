package com.yitianys.BlockZ.client.gui;

public class UIConstants {
    // 总尺寸
    public static final int WIDTH = 400;
    public static final int HEIGHT = 240;
    public static final int GUI_WIDTH = WIDTH;
    public static final int GUI_HEIGHT = HEIGHT;

    // 面板尺寸 (缩小宽度)
    public static final int PANEL_W = 100;
    public static final int PANEL_H = 180;
    
    // 面板 X 坐标
    public static final int VICINITY_X = 20;
    public static final int PLAYER_X = VICINITY_X + PANEL_W + 15;
    public static final int INVENTORY_X = PLAYER_X + PANEL_W + 15;

    // 面板 Y 坐标
    public static final int PANEL_Y = 20;

    // 槽位尺寸
    public static final int SLOT_SIZE = 18;
    public static final int SLOT_PITCH = 18;

    // 快捷栏
    public static final int HOTBAR_COLS = 9;
    public static final int HOTBAR_X = (WIDTH - HOTBAR_COLS * SLOT_PITCH) / 2;
    public static final int HOTBAR_Y = HEIGHT - 30;

    // 护甲槽位 (相对于 PLAYER_X, PANEL_Y)
    public static final int ARMOR_HELMET_X = PLAYER_X + 10;
    public static final int ARMOR_HELMET_Y = PANEL_Y + 10;
    public static final int ARMOR_CHEST_X = PLAYER_X + 10;
    public static final int ARMOR_CHEST_Y = PANEL_Y + 34;
    public static final int ARMOR_LEGS_X = PLAYER_X + 10;
    public static final int ARMOR_LEGS_Y = PANEL_Y + 58;
    public static final int ARMOR_BOOTS_X = PLAYER_X + 10;
    public static final int ARMOR_BOOTS_Y = PANEL_Y + 82;
    
    // 副手槽位 (在 PLAYER 面板右侧)
    public static final int OFFHAND_X = PLAYER_X + PANEL_W - 28;
    public static final int OFFHAND_Y = PANEL_Y + 10;

    // 背包装备位 (在 PLAYER 面板右侧，副手下方)
    public static final int BACKPACK_EQUIP_X = PLAYER_X + PANEL_W - 28;
    public static final int BACKPACK_EQUIP_Y = OFFHAND_Y + SLOT_PITCH + 5;

    // 物品栏布局重新规划 (标准的 27 格背包)
    public static final int INVENTORY_COLS = 5; // 保持 5 列以适应面板宽度
    public static final int INVENTORY_SLOTS_X = INVENTORY_X + 5;
    public static final int INVENTORY_SLOTS_Y = PANEL_Y + 10;

    // Vicinity 槽位 (VICINITY 面板 - 5列布局)
    public static final int VICINITY_COLS = 5;
    public static final int VICINITY_SLOTS_X = VICINITY_X + 5;
    public static final int VICINITY_SLOTS_Y = PANEL_Y + 10;
}

package com.yitianys.BlockZ.client;

import com.yitianys.BlockZ.util.LeanManager.LeanState;

public class ClientSettings {
    public static final int FRACTURE_FLASH_DURATION_TICKS = 80;
    public static boolean dayzEnabled = true;
    public static boolean dayzToggleAllowed = true;
    public static boolean dayzHudEnabled = true;
    public static int fractureFlashTicks = 0;
    public static long lastFractureFlashGameTime = -1L;
    public static float healthPointsRatio = 1.0F;
    public static float healthRatio = 1.0F;
    public static float staminaRatio = 1.0F;
    public static float infectionRatio = 0.0F;

    public static LeanState clientLeanPending = LeanState.NONE;
    public static long lastLeanChangeTime = 0L;
}

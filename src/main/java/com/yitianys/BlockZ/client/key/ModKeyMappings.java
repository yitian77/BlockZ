package com.yitianys.BlockZ.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.client.ClientSettings;
import com.yitianys.BlockZ.compat.TaczProneCompat;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.network.DayzToggleRequestC2S;
import com.yitianys.BlockZ.network.LeanUpdateC2S;
import com.yitianys.BlockZ.network.NetworkHandler;
import com.yitianys.BlockZ.network.ProneUpdateC2S;
import com.yitianys.BlockZ.util.LeanManager;
import com.yitianys.BlockZ.util.LeanManager.LeanState;
import com.yitianys.BlockZ.util.ProneManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = BlockZ.MODID, value = Dist.CLIENT)
public class ModKeyMappings {
    public static KeyMapping OPEN_DAYZ;
    public static KeyMapping ROTATE_ITEM;
    public static KeyMapping LEAN_LEFT;
    public static KeyMapping LEAN_RIGHT;
    public static KeyMapping PRONE;
    public static KeyMapping FOCUS;

    private static boolean leftHeld = false;
    private static boolean rightHeld = false;
    private static boolean proneHeld = false;
    private static boolean jumpSuppressedUntilRelease = false;
    private static LeanState lastSentLean = LeanState.NONE;
    private static float focusFovMultiplier = 1.0F;

    public static void register(RegisterKeyMappingsEvent event) {
        OPEN_DAYZ = new KeyMapping("key.blockz.open_dayz", InputConstants.KEY_I, "key.categories.inventory");
        ROTATE_ITEM = new KeyMapping("key.blockz.rotate_item", InputConstants.KEY_SPACE, "key.categories.inventory");
        LEAN_LEFT = new KeyMapping("key.blockz.lean_left",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Q,
                "key.categories.blockz");
        LEAN_RIGHT = new KeyMapping("key.blockz.lean_right",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_E,
                "key.categories.blockz");
        PRONE = new KeyMapping("key.blockz.prone",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "key.categories.blockz");
        FOCUS = new KeyMapping("key.blockz.focus",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
                "key.categories.blockz");
        event.register(OPEN_DAYZ);
        event.register(ROTATE_ITEM);
        event.register(LEAN_LEFT);
        event.register(LEAN_RIGHT);
        event.register(PRONE);
        event.register(FOCUS);
    }

    public static void configureClientDefaults() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) {
            return;
        }
        InputConstants.Key middleMouse = InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
        if (!mc.options.keyPickItem.getKey().equals(middleMouse)) {
            return;
        }
        mc.options.keyPickItem.setKey(InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_4));
        KeyMapping.resetMapping();
        mc.options.save();
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (OPEN_DAYZ != null && OPEN_DAYZ.consumeClick()) {
            if (!ClientSettings.dayzToggleAllowed) {
                mc.player.sendSystemMessage(Component.translatable("msg.blockz.dayz_toggle_denied"));
                return;
            }
            BlockZ.LOGGER.info("Toggling DayZ UI. Current state: {}. Sending: {}", ClientSettings.dayzEnabled, !ClientSettings.dayzEnabled);
            NetworkHandler.CHANNEL.sendToServer(new DayzToggleRequestC2S(!ClientSettings.dayzEnabled));
        }
        if (handleProneJumpInput(mc, event)) {
            return;
        }
        if (PRONE == null) {
            return;
        }

        if (PRONE.matches(event.getKey(), event.getScanCode()) && event.getAction() == GLFW.GLFW_RELEASE) {
            proneHeld = false;
            return;
        }

        if (mc.screen == null && mc.player != null && PRONE.matches(event.getKey(), event.getScanCode()) && event.getAction() == GLFW.GLFW_PRESS && !proneHeld) {
            proneHeld = true;
            LocalPlayer player = mc.player;
            if (player == null) {
                return;
            }
            boolean desiredProne = !ProneManager.isClientProne(player.getUUID());
            if (desiredProne && !ProneManager.canEnterProne(player)) {
                return;
            }
            if (!desiredProne && !ProneManager.canStandUp(player)) {
                return;
            }
            setClientProneState(player, desiredProne);
        }
    }

    @SubscribeEvent
    public static void onKeyPress(InputEvent.Key event) {
        if (LEAN_LEFT == null || LEAN_RIGHT == null) return;
        if (Minecraft.getInstance().screen != null) return;

        int key = event.getKey();
        int action = event.getAction();

        if (LEAN_LEFT.matches(key, event.getScanCode())) {
            if (action == GLFW.GLFW_PRESS) {
                leftHeld = true;
            } else if (action == GLFW.GLFW_RELEASE) {
                leftHeld = false;
            }
        }
        if (LEAN_RIGHT.matches(key, event.getScanCode())) {
            if (action == GLFW.GLFW_PRESS) {
                rightHeld = true;
            } else if (action == GLFW.GLFW_RELEASE) {
                rightHeld = false;
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (jumpSuppressedUntilRelease && mc.options != null && mc.options.keyJump.isDown()) {
            mc.options.keyJump.setDown(false);
        }
        LocalPlayer player = mc.player;
        updateFocusFov(player, mc);
        if (player == null) {
            jumpSuppressedUntilRelease = false;
            return;
        }

        if (!BlockZConfigs.isLeanEnabled() || ProneManager.isProne(player)) {
            resetLean(player);
            return;
        }

        LeanState desired = getDesiredLeanState(player);
        if (desired != lastSentLean) {
            lastSentLean = desired;
            LeanManager.setClientLeanState(player.getUUID(), desired);
            NetworkHandler.CHANNEL.sendToServer(new LeanUpdateC2S(desired));
        }
    }

    private static void resetLean(LocalPlayer player) {
        if (lastSentLean != LeanState.NONE) {
            lastSentLean = LeanState.NONE;
            LeanManager.setClientLeanState(player.getUUID(), LeanState.NONE);
            NetworkHandler.CHANNEL.sendToServer(new LeanUpdateC2S(LeanState.NONE));
        }
    }

    private static boolean handleProneJumpInput(Minecraft mc, InputEvent.Key event) {
        if (mc.player == null || mc.screen != null || mc.options == null) {
            return false;
        }
        if (!mc.options.keyJump.matches(event.getKey(), event.getScanCode())) {
            return false;
        }
        if (event.getAction() == GLFW.GLFW_RELEASE) {
            jumpSuppressedUntilRelease = false;
            return false;
        }
        if (jumpSuppressedUntilRelease) {
            mc.options.keyJump.setDown(false);
            return true;
        }
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return false;
        }
        LocalPlayer player = mc.player;
        if (!ProneManager.isProne(player)) {
            return false;
        }
        jumpSuppressedUntilRelease = true;
        mc.options.keyJump.setDown(false);
        if (!ProneManager.canStandUp(player)) {
            return true;
        }
        setClientProneState(player, false);
        return true;
    }

    private static void setClientProneState(LocalPlayer player, boolean desiredProne) {
        if (desiredProne) {
            resetLean(player);
        }
        ProneManager.setClientProneState(player.getUUID(), desiredProne);
        TaczProneCompat.setClientCrawling(player, desiredProne);
        if (!desiredProne) {
            ProneManager.clearAppliedPronePose(player);
        }
        NetworkHandler.CHANNEL.sendToServer(new ProneUpdateC2S(desiredProne));
    }

    private static LeanState getDesiredLeanState(LocalPlayer player) {
        if (player.isSpectator() || player.isPassenger() || !player.onGround() || ProneManager.isProne(player)) {
            return LeanState.NONE;
        }
        if (player.isSprinting()) {
            return LeanState.NONE;
        }
        if (leftHeld && rightHeld) return LeanState.NONE;
        if (leftHeld) return LeanState.LEFT;
        if (rightHeld) return LeanState.RIGHT;
        return LeanState.NONE;
    }

    private static void updateFocusFov(LocalPlayer player, Minecraft mc) {
        float targetMultiplier = 1.0F;
        if (player != null && isFocusActive(player, mc)) {
            targetMultiplier = player.isSprinting()
                    ? (float) BlockZConfigs.getFocusFovMultiplierSprint()
                    : (float) BlockZConfigs.getFocusFovMultiplierWalk();
        }
        float smoothing = Mth.clamp((float) BlockZConfigs.getFocusFovSmoothing(), 0.01F, 1.0F);
        focusFovMultiplier = Mth.lerp(smoothing, focusFovMultiplier, targetMultiplier);
        if (Math.abs(focusFovMultiplier - targetMultiplier) < 0.001F) {
            focusFovMultiplier = targetMultiplier;
        }
    }

    private static boolean isFocusActive(LocalPlayer player, Minecraft mc) {
        return FOCUS != null
                && FOCUS.isDown()
                && BlockZConfigs.isFocusZoomEnabled()
                && mc.screen == null
                && !mc.isPaused()
                && mc.options.getCameraType().isFirstPerson()
                && !player.isSpectator()
                && !player.isSleeping()
                && !player.isScoping();
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!BlockZConfigs.isLeanEnabled()) return;
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (ProneManager.isProne(player)) return;

        float progress = LeanManager.getSmoothLeanProgress(player.getUUID());
        if (Math.abs(progress) < 0.001F) return;

        event.setRoll(event.getRoll() - LeanManager.getFirstPersonLeanRollDegrees(progress));
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (Math.abs(focusFovMultiplier - 1.0F) < 0.001F) {
            return;
        }
        event.setFOV(event.getFOV() * focusFovMultiplier);
    }
}

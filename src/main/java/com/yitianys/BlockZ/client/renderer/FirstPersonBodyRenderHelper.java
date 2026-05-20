package com.yitianys.BlockZ.client.renderer;

import com.yitianys.BlockZ.compat.TaczRenderCompat;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.util.ProneManager;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class FirstPersonBodyRenderHelper {
    private FirstPersonBodyRenderHelper() {
    }

    public static boolean shouldRenderPlayerBody(Camera camera) {
        if (camera.isDetached() || !(camera.getEntity() instanceof LocalPlayer player)) {
            return false;
        }
        return isBodyRenderActive(Minecraft.getInstance(), player);
    }

    public static boolean shouldSuppressVanillaHands(AbstractClientPlayer player) {
        if (!(player instanceof LocalPlayer localPlayer)) {
            return false;
        }
        return isBodyRenderActive(Minecraft.getInstance(), localPlayer) && !shouldUseExternalHands(localPlayer);
    }

    public static boolean shouldHideArms(LocalPlayer player) {
        return shouldUseExternalHands(player);
    }

    public static boolean shouldHideHeldItems(LocalPlayer player) {
        return shouldUseExternalHands(player);
    }

    public static Vec3 getPlayerRenderOffset(AbstractClientPlayer player, float partialTick, Vec3 baseOffset) {
        if (baseOffset == null) {
            baseOffset = Vec3.ZERO;
        }
        if (!(player instanceof LocalPlayer localPlayer)) {
            return baseOffset;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.cameraEntity != localPlayer || !FirstPersonBodyRenderState.isRendering()) {
            return baseOffset;
        }
        return baseOffset.add(getBodyOffset(localPlayer, partialTick));
    }

    public static Vec3 getBodyOffset(AbstractClientPlayer player, float partialTick) {
        if (!(player instanceof LocalPlayer localPlayer)) {
            return Vec3.ZERO;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.cameraEntity != localPlayer || !isBodyRenderActive(minecraft, localPlayer)) {
            return Vec3.ZERO;
        }
        float bodyYaw = Mth.rotLerp(partialTick, localPlayer.yBodyRotO, localPlayer.yBodyRot);
        float bodyOffset = localPlayer.isShiftKeyDown() ? 0.27F : 0.25F;
        double x = bodyOffset * Mth.sin(bodyYaw * Mth.DEG_TO_RAD);
        double z = -bodyOffset * Mth.cos(bodyYaw * Mth.DEG_TO_RAD);
        return new Vec3(x, 0.0D, z);
    }

    private static boolean isBodyRenderActive(Minecraft minecraft, LocalPlayer player) {
        if (minecraft.player != player || minecraft.level == null) {
            return false;
        }
        if (!BlockZConfigs.isRealFirstPersonEnabled()) {
            return false;
        }
        if (minecraft.options.getCameraType() != CameraType.FIRST_PERSON) {
            return false;
        }
        if (minecraft.screen != null || minecraft.isPaused()) {
            return false;
        }
        if (player.isSpectator() || player.isInvisible() || player.isSleeping()) {
            return false;
        }
        if (player.isPassenger() || player.isFallFlying() || player.isAutoSpinAttack()) {
            return false;
        }
        if (ProneManager.isProne(player)) {
            return false;
        }
        if (player.getSwimAmount(1.0F) != 0.0F && !player.isVisuallySwimming()) {
            return false;
        }
        return !player.isSwimming() && !player.isScoping();
    }

    private static boolean shouldUseExternalHands(LocalPlayer player) {
        return TaczRenderCompat.isTaczGun(player.getMainHandItem())
                || TaczRenderCompat.isTaczGun(player.getOffhandItem());
    }
}

package com.yitianys.BlockZ.client.event;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.client.camera.CameraWalkSwayManager;
import com.yitianys.BlockZ.client.camera.ThirdPersonShoulderCameraManager;
import com.yitianys.BlockZ.config.BlockZConfigs;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZ.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CameraSwayEvents {
    private CameraSwayEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        CameraWalkSwayManager.tick(minecraft);
        ThirdPersonShoulderCameraManager.tick(minecraft);
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        var player = minecraft.player;
        if (!minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (!BlockZConfigs.shouldReplaceVanillaWalkBobbing()) {
            return;
        }

        float partialTick = (float) event.getPartialTick();
        event.setYaw(event.getYaw() + CameraWalkSwayManager.getYawOffset(player, partialTick));
        event.setPitch(event.getPitch() + CameraWalkSwayManager.getPitchOffset(player, partialTick));
        event.setRoll(event.getRoll() + CameraWalkSwayManager.getRollOffset(player, partialTick));
    }

    @SubscribeEvent
    public static void onClientPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CameraWalkSwayManager.resetRuntimeState();
        ThirdPersonShoulderCameraManager.resetRuntimeState();
    }
}

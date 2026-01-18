package com.yitianys.BlockZ.client.key;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.client.ClientSettings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = BlockZ.MODID, value = Dist.CLIENT)
public class ModKeyMappings {
    public static KeyMapping OPEN_DAYZ;

    public static void register(RegisterKeyMappingsEvent event) {
        OPEN_DAYZ = new KeyMapping("key.blockz.open_dayz", GLFW.GLFW_KEY_I, "key.categories.inventory");
        event.register(OPEN_DAYZ);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (OPEN_DAYZ != null && OPEN_DAYZ.consumeClick()) {
            ClientSettings.dayzEnabled = !ClientSettings.dayzEnabled;
            mc.player.sendSystemMessage(Component.translatable(ClientSettings.dayzEnabled ? "msg.blockz.dayz_enabled" : "msg.blockz.dayz_disabled"));
        }
    }
}

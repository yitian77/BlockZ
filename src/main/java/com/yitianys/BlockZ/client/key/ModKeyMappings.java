package com.yitianys.BlockZ.client.key;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.client.ClientSettings;
import com.yitianys.BlockZ.network.DayzToggleRequestC2S;
import com.yitianys.BlockZ.network.NetworkHandler;
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
    public static KeyMapping ROTATE_ITEM;

    public static void register(RegisterKeyMappingsEvent event) {
        OPEN_DAYZ = new KeyMapping("key.blockz.open_dayz", GLFW.GLFW_KEY_I, "key.categories.inventory");
        ROTATE_ITEM = new KeyMapping("key.blockz.rotate_item", GLFW.GLFW_KEY_SPACE, "key.categories.inventory");
        event.register(OPEN_DAYZ);
        event.register(ROTATE_ITEM);
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
    }
}

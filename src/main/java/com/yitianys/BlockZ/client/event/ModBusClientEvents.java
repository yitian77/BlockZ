package com.yitianys.BlockZ.client.event;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.client.gui.DayZHudOverlay;
import com.yitianys.BlockZ.client.gui.DayZInventoryScreen;
import com.yitianys.BlockZ.client.key.ModKeyMappings;
import com.yitianys.BlockZ.client.renderer.CorpseRenderer;
import com.yitianys.BlockZ.client.renderer.DayZZombieRenderer;
import com.yitianys.BlockZ.client.renderer.ZombieCorpseRenderer;
import com.yitianys.BlockZ.client.renderer.layer.ClothingLayer;
import com.yitianys.BlockZ.init.ModEntities;
import com.yitianys.BlockZ.init.ModMenus;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = BlockZ.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
@SuppressWarnings({"deprecation", "removal"})
public class ModBusClientEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.DAYZ_INVENTORY.get(), DayZInventoryScreen::new);
            ModKeyMappings.configureClientDefaults();
        });
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        ModKeyMappings.register(event);
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        // 使用 registerAboveAll 确保在最顶层渲染
        event.registerAboveAll("dayz_hud", DayZHudOverlay.HUD_OVERLAY);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.DAYZ_ZOMBIE.get(), DayZZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.CORPSE.get(), CorpseRenderer::new);
        event.registerEntityRenderer(ModEntities.DAYZ_ZOMBIE_CORPSE.get(), ZombieCorpseRenderer::new);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // Add clothing layer to default player renderer
        PlayerRenderer defaultRenderer = event.getSkin("default");
        if (defaultRenderer != null) {
            defaultRenderer.addLayer(new ClothingLayer(defaultRenderer));
        }

        // Add clothing layer to slim player renderer (Alex)
        PlayerRenderer slimRenderer = event.getSkin("slim");
        if (slimRenderer != null) {
            slimRenderer.addLayer(new ClothingLayer(slimRenderer));
        }
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(new ResourceLocation(BlockZ.MODID, "item/backpack_coyote_3d"));
        event.register(new ResourceLocation(BlockZ.MODID, "item/backpack_alice_3d"));
        event.register(new ResourceLocation(BlockZ.MODID, "item/backpack_czech_3d"));
        event.register(new ResourceLocation(BlockZ.MODID, "item/backpack_czechpouch_3d"));
        event.register(new ResourceLocation(BlockZ.MODID, "item/backpack_patrolpack_3d"));
        event.register(new ResourceLocation(BlockZ.MODID, "item/vest_0_3d"));
    }
}

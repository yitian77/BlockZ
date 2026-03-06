package com.yitianys.BlockZ.client.event;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.client.ClientSettings;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import com.yitianys.BlockZ.network.NetworkHandler;
import com.yitianys.BlockZ.network.OpenDayZMenuC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;
import com.yitianys.BlockZ.network.LootPickupC2S;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;

import com.yitianys.BlockZ.network.RequestSwitchToDayZMenuC2S;
import com.yitianys.BlockZ.util.InventoryUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = BlockZ.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {
    // onRegisterGuiOverlays 移到了 ModBusClientEvents

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (ClientSettings.dayzEnabled) {
            // 隐藏原版快捷栏、经验条、生命、饥饿等，以及手持物品名称
            if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id()) ||
                event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id()) ||
                event.getOverlay().id().equals(VanillaGuiOverlay.FOOD_LEVEL.id()) ||
                event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id()) ||
                event.getOverlay().id().equals(VanillaGuiOverlay.ARMOR_LEVEL.id()) ||
                event.getOverlay().id().equals(VanillaGuiOverlay.ITEM_NAME.id()) ||
                event.getOverlay().id().getNamespace().equals("thirst")) {
                event.setCanceled(true);
            }
        }
    }

    private static boolean isArclightServer() {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) return false;
        String brand = null;
        try {
            Method method = connection.getClass().getMethod("getServerBrand");
            Object value = method.invoke(connection);
            if (value instanceof String text) {
                brand = text;
            }
        } catch (Exception ignored) {
        }

        if (brand == null) {
            try {
                Field field = connection.getClass().getDeclaredField("serverBrand");
                field.setAccessible(true);
                Object value = field.get(connection);
                if (value instanceof String text) {
                    brand = text;
                }
            } catch (Exception ignored) {
            }
        }

        if (brand == null) {
            try {
                for (Field field : connection.getClass().getDeclaredFields()) {
                    if (field.getType() != String.class) continue;
                    String name = field.getName().toLowerCase(Locale.ROOT);
                    if (!name.contains("brand")) continue;
                    field.setAccessible(true);
                    Object value = field.get(connection);
                    if (value instanceof String text) {
                        brand = text;
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (brand == null || brand.isBlank()) return false;
        String lower = brand.toLowerCase(Locale.ROOT);
        return lower.contains("arclight") || lower.contains("bukkit");
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof InventoryScreen) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            
            // 只有当 DayZ UI 启用时才拦截
            if (ClientSettings.dayzEnabled) {
                BlockZ.LOGGER.info("Intercepting InventoryScreen opening, sending OpenDayZMenuC2S to server. dayzEnabled={}", ClientSettings.dayzEnabled);
                NetworkHandler.CHANNEL.sendToServer(new OpenDayZMenuC2S());
                if (!isArclightServer()) {
                    event.setCanceled(true);
                }
            } else {
                BlockZ.LOGGER.info("Allowing InventoryScreen opening. dayzEnabled={}", ClientSettings.dayzEnabled);
            }
        } else if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            if (ClientSettings.dayzEnabled) {
                if (event.getScreen() instanceof com.yitianys.BlockZ.client.gui.DayZInventoryScreen) return;
                if (event.getScreen() instanceof com.yitianys.BlockZ.client.gui.DayZChestScreen) return;

                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return;
                if (containerScreen.getMenu() == mc.player.inventoryMenu) return;

                int minX = Integer.MAX_VALUE;
                int minY = Integer.MAX_VALUE;
                List<Slot> containerSlots = new ArrayList<>();
                for (Slot slot : containerScreen.getMenu().slots) {
                    if (slot.container == mc.player.getInventory()) continue;
                    containerSlots.add(slot);
                    if (slot.x < minX) minX = slot.x;
                    if (slot.y < minY) minY = slot.y;
                }

                if (!containerSlots.isEmpty()) {
                    List<DayZInventoryMenu.VicinitySlotLayout> layout = new ArrayList<>();
                    int baseX = minX == Integer.MAX_VALUE ? 0 : minX;
                    int baseY = minY == Integer.MAX_VALUE ? 0 : minY;
                    for (Slot slot : containerSlots) {
                        layout.add(new DayZInventoryMenu.VicinitySlotLayout(
                                slot.getSlotIndex(),
                                slot.x - baseX,
                                slot.y - baseY
                        ));
                    }
                    DayZInventoryMenu.setPendingClientLayout(layout);
                }

                NetworkHandler.CHANNEL.sendToServer(new RequestSwitchToDayZMenuC2S(containerScreen.getTitle()));

                if (containerScreen.getMenu() instanceof ChestMenu chestMenu) {
                    event.setNewScreen(new com.yitianys.BlockZ.client.gui.DayZChestScreen(
                            chestMenu,
                            mc.player.getInventory(),
                            containerScreen.getTitle()
                    ));
                } else if (!isArclightServer()) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        tryPickup(event);
    }

    @SubscribeEvent
    public static void onClientRightClickItem(PlayerInteractEvent.RightClickItem event) {
        tryPickup(event);
    }
    
    @SubscribeEvent
    public static void onClientRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        tryPickup(event);
    }

    @SubscribeEvent
    public static void onClientEntityInteract(PlayerInteractEvent.EntityInteract event) {
        tryPickup(event);
    }

    private static void tryPickup(PlayerInteractEvent event) {
        if (!event.getLevel().isClientSide) return;
        if (!ClientSettings.dayzEnabled) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Entity entity = InventoryUtils.getTargetedItemEntity(mc.player, 4.0); // 4 blocks reach
        if (entity != null) {
            NetworkHandler.CHANNEL.sendToServer(new LootPickupC2S(entity.getId()));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (event instanceof PlayerInteractEvent.RightClickBlock blockEvent) {
                blockEvent.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
                blockEvent.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
            } else if (event instanceof PlayerInteractEvent.RightClickItem itemEvent) {
                itemEvent.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }

}

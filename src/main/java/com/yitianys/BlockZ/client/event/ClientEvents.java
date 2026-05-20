package com.yitianys.BlockZ.client.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.client.ClientSettings;
import com.yitianys.BlockZ.compat.TaczClientCompat;
import com.yitianys.BlockZ.compat.TaczProneCompat;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.config.DayZZombieConfig;
import com.yitianys.BlockZ.entity.DayZZombieEntity;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import com.yitianys.BlockZ.network.NetworkHandler;
import com.yitianys.BlockZ.network.OpenDayZMenuC2S;
import com.yitianys.BlockZ.util.DayZStatsManager;
import com.yitianys.BlockZ.util.ProneManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import com.yitianys.BlockZ.network.LootPickupC2S;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import com.yitianys.BlockZ.network.RequestSwitchToDayZMenuC2S;
import com.yitianys.BlockZ.util.InventoryUtils;
import com.yitianys.BlockZ.util.ItemSizeManager;
import net.minecraft.client.CameraType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = BlockZ.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {
    private static final float CLIENT_SPRINT_EXHAUSTED_STAMINA = 1.0F;
    private static final float CLIENT_SPRINT_RECOVERY_STAMINA = 8.0F;
    private static final float EXHAUSTED_STAMINA_THRESHOLD = 0.03F;
    private static final float HEAVY_BREATHING_THRESHOLD = 0.10F;
    private static final float EXHAUSTED_RECOVERY_CLEAR_THRESHOLD = 0.25F;
    private static final int STAMINA_SOUND_NONE = 0;
    private static final int STAMINA_SOUND_NORMAL = 1;
    private static final int STAMINA_SOUND_EXHAUSTED = 2;
    private static float lastStaminaRatio = 1.0f;
    private static int breathingCooldown = 0;
    private static boolean exhaustedRecoveryQueued = false;
    private static boolean sprintRecoveryLocked = false;
    private static net.minecraft.client.resources.sounds.SoundInstance activeStaminaSound = null;
    private static int activeStaminaSoundType = STAMINA_SOUND_NONE;
    private static boolean taczAimingCameraOverride = false;
    private static CameraType taczPreviousCameraType = CameraType.THIRD_PERSON_BACK;

    // onRegisterGuiOverlays 移到了 ModBusClientEvents

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        com.yitianys.BlockZ.client.gui.mainmenu.DayZMainMenuScreen.tickMenuMusic();
    }

    @SubscribeEvent
    public static void onClientPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !(event.player instanceof LocalPlayer player)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (BlockZConfigs.isThirdPersonFrontViewDisabled() && minecraft.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        updateTaczAimingCamera(minecraft, player);

        if (ProneManager.validateClientState(player)) {
            ProneManager.clearAppliedPronePose(player);
            TaczProneCompat.setClientCrawling(player, false);
            NetworkHandler.CHANNEL.sendToServer(new com.yitianys.BlockZ.network.ProneUpdateC2S(false));
        }
        
        // 更新本地统计数据
        DayZStatsManager.update(player);

        // 体力呼吸声逻辑
        if (BlockZConfigs.isStaminaEnabled()) {
            float staminaRatio = ClientSettings.staminaRatio;
            Minecraft mc = Minecraft.getInstance();
            if (activeStaminaSound != null && !mc.getSoundManager().isActive(activeStaminaSound)) {
                activeStaminaSound = null;
                activeStaminaSoundType = STAMINA_SOUND_NONE;
            }
            if (staminaRatio <= EXHAUSTED_STAMINA_THRESHOLD && lastStaminaRatio > EXHAUSTED_STAMINA_THRESHOLD) {
                exhaustedRecoveryQueued = true;
            }

            if (breathingCooldown > 0) {
                breathingCooldown--;
            }

            boolean staminaRecovering = staminaRatio > lastStaminaRatio;
            int desiredSoundType = STAMINA_SOUND_NONE;
            if (staminaRatio < HEAVY_BREATHING_THRESHOLD || (exhaustedRecoveryQueued && staminaRecovering && staminaRatio < EXHAUSTED_RECOVERY_CLEAR_THRESHOLD)) {
                desiredSoundType = STAMINA_SOUND_EXHAUSTED;
            } else if (staminaRatio < 0.80f && staminaRecovering) {
                desiredSoundType = STAMINA_SOUND_NORMAL;
            }

            if (desiredSoundType == STAMINA_SOUND_NONE) {
                if (staminaRatio >= 0.80f || staminaRatio >= EXHAUSTED_RECOVERY_CLEAR_THRESHOLD) {
                    exhaustedRecoveryQueued = false;
                }
                stopActiveStaminaSound(mc);
            } else {
                boolean forceSwitch = desiredSoundType > activeStaminaSoundType;
                boolean needsStart = activeStaminaSound == null || forceSwitch || desiredSoundType != activeStaminaSoundType;
                if (needsStart && (forceSwitch || breathingCooldown <= 0)) {
                    playStaminaBreathing(mc, desiredSoundType, staminaRatio);
                    breathingCooldown = desiredSoundType == STAMINA_SOUND_EXHAUSTED ? 6 : 12;
                }
            }
            lastStaminaRatio = staminaRatio;
        } else {
            resetStaminaBreathingState();
        }

        if (player.isCreative() || player.isSpectator()) {
            sprintRecoveryLocked = false;
            return;
        }

        float maxStamina = Math.max(1.0F, (float) BlockZConfigs.getStaminaMaxCapacity());
        float staminaValue = ClientSettings.staminaRatio * maxStamina;
        float exhaustedThreshold = Math.min(CLIENT_SPRINT_EXHAUSTED_STAMINA, maxStamina);
        float recoveryThreshold = Math.min(CLIENT_SPRINT_RECOVERY_STAMINA, maxStamina);
        if (staminaValue <= exhaustedThreshold) {
            sprintRecoveryLocked = true;
        } else if (sprintRecoveryLocked && staminaValue >= recoveryThreshold) {
            sprintRecoveryLocked = false;
        }

        if (sprintRecoveryLocked) {
            if (player.isSprinting()) {
                player.setSprinting(false);
            }
            if (minecraft.options.keySprint.isDown()) {
                minecraft.options.keySprint.setDown(false);
            }
        }
    }

    @SubscribeEvent
    public static void onClientPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        DayZStatsManager.load();
        DayZStatsManager.resetSession();
        
        // 进入游戏时彻底停止主菜单音乐
        com.yitianys.BlockZ.client.gui.mainmenu.DayZMainMenuScreen.stopMenuMusic();
        ProneManager.clearClientStates();
        resetStaminaBreathingState();
    }

    @SubscribeEvent
    public static void onClientPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DayZStatsManager.save();
        BlockZConfigs.clearSyncedValues();
        DayZZombieConfig.clearSyncedValues();
        ItemSizeManager.clearSyncedClientState();
        ProneManager.clearClientStates();
        resetStaminaBreathingState();
        resetTaczAimingCamera(Minecraft.getInstance());
    }

    private static void updateTaczAimingCamera(Minecraft minecraft, LocalPlayer player) {
        boolean aiming = isTaczAiming(player);
        CameraType cameraType = minecraft.options.getCameraType();
        if (aiming) {
            if (!taczAimingCameraOverride && cameraType == CameraType.THIRD_PERSON_BACK) {
                taczPreviousCameraType = cameraType;
                taczAimingCameraOverride = true;
                minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            } else if (taczAimingCameraOverride && cameraType != CameraType.FIRST_PERSON) {
                minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            }
            return;
        }

        if (taczAimingCameraOverride) {
            if (cameraType == CameraType.FIRST_PERSON) {
                minecraft.options.setCameraType(taczPreviousCameraType);
            }
            taczAimingCameraOverride = false;
            taczPreviousCameraType = CameraType.THIRD_PERSON_BACK;
        }
    }

    private static void resetTaczAimingCamera(Minecraft minecraft) {
        if (taczAimingCameraOverride && minecraft != null && minecraft.options.getCameraType() == CameraType.FIRST_PERSON) {
            minecraft.options.setCameraType(taczPreviousCameraType);
        }
        taczAimingCameraOverride = false;
        taczPreviousCameraType = CameraType.THIRD_PERSON_BACK;
    }

    private static boolean isTaczAiming(LocalPlayer player) {
        return TaczClientCompat.isClientAiming(player);
    }

    private static void playStaminaBreathing(Minecraft mc, int soundType, float staminaRatio) {
        stopActiveStaminaSound(mc);
        net.minecraft.sounds.SoundEvent sound = soundType == STAMINA_SOUND_EXHAUSTED
                ? com.yitianys.BlockZ.init.ModSounds.PLAYER_STAMINA_REGEN_EXHAUSTED.get()
                : com.yitianys.BlockZ.init.ModSounds.PLAYER_STAMINA_REGEN_NORMAL.get();
        activeStaminaSound = net.minecraft.client.resources.sounds.SimpleSoundInstance.forLocalAmbience(
                sound,
                1.0f + (0.5f - staminaRatio) * 0.2f,
                0.45f
        );
        mc.getSoundManager().play(activeStaminaSound);
        activeStaminaSoundType = soundType;
        if (soundType == STAMINA_SOUND_EXHAUSTED && staminaRatio >= EXHAUSTED_RECOVERY_CLEAR_THRESHOLD) {
            exhaustedRecoveryQueued = false;
        }
    }

    private static void stopActiveStaminaSound(Minecraft mc) {
        if (mc != null && activeStaminaSound != null) {
            mc.getSoundManager().stop(activeStaminaSound);
        }
        activeStaminaSound = null;
        activeStaminaSoundType = STAMINA_SOUND_NONE;
    }

    private static void resetStaminaBreathingState() {
        breathingCooldown = 0;
        exhaustedRecoveryQueued = false;
        lastStaminaRatio = 1.0f;
        sprintRecoveryLocked = false;
        stopActiveStaminaSound(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onClientLivingDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer == null) {
            return;
        }

        if (event.getSource().getEntity() != localPlayer) {
            return;
        }

        Entity killed = event.getEntity();
        if (killed instanceof DayZZombieEntity || killed instanceof net.minecraft.world.entity.monster.Zombie) {
            DayZStatsManager.addZombieKill();
        } else if (killed instanceof Player && killed != localPlayer) {
            DayZStatsManager.addPlayerKill();
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (BlockZConfigs.getShowDayzHud() && ClientSettings.dayzHudEnabled) {
            // 在 DayZ 模式下，我们在隐藏原版 UI 之前先渲染暗角，这样暗角就在 DayZ UI 的底层
            renderWastelandVignette(event);

            // 隐藏原版快捷栏、经验条、生命、饥饿等，以及手持物品名称
            String path = event.getOverlay().id().getPath().toLowerCase(Locale.ROOT);
            if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id()) ||
                event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id()) ||
                event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id()) ||
                event.getOverlay().id().equals(VanillaGuiOverlay.FOOD_LEVEL.id()) ||
                event.getOverlay().id().equals(VanillaGuiOverlay.ARMOR_LEVEL.id()) ||
                event.getOverlay().id().equals(VanillaGuiOverlay.AIR_LEVEL.id()) ||
                event.getOverlay().id().equals(VanillaGuiOverlay.ITEM_NAME.id()) ||
                path.contains("thirst") || path.contains("hydration")) { // 拦截第三方饮水模组的 overlay
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPost(RenderGuiOverlayEvent.Post event) {
        // 在非 DayZ 模式下，我们在原版 HOTBAR 之后渲染暗角，使其在最顶层
        if (!BlockZConfigs.getShowDayzHud()) {
            if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
                renderWastelandVignette(event);
            }
        }
    }

    private static void renderWastelandVignette(RenderGuiOverlayEvent event) {
        if (!BlockZConfigs.getEnableVignette()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

        float strength = (float) BlockZConfigs.getVignetteStrength();
        if (strength <= 0.0F) return;

        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();
        int band = Math.max(1, (int) (h * 0.22F));

        int alpha = Math.max(0, Math.min(255, (int) (strength * 200.0F)));
        int darkColor = alpha << 24;
        
        RenderSystem.enableBlend();
        g.fillGradient(0, 0, w, band, darkColor, 0);
        g.fillGradient(0, h - band, w, h, 0, darkColor);
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

    private static boolean shouldSkipDayZOverride(AbstractContainerScreen<?> screen) {
        try {
            if (screen.getMenu() != null) {
                var key = ForgeRegistries.MENU_TYPES.getKey(screen.getMenu().getType());
                if (key == null || !"minecraft".equals(key.getNamespace())) {
                    return true;
                }
                String menuName = screen.getMenu().getClass().getName().toLowerCase(Locale.ROOT);
                if (menuName.contains("tacz") || !isSafeVanillaContainerMenu(screen)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            return true;
        }

        String screenName = screen.getClass().getName().toLowerCase(Locale.ROOT);
        return screenName.contains("tacz");
    }

    private static boolean isSafeVanillaContainerMenu(AbstractContainerScreen<?> screen) {
        if (screen.getMenu() instanceof ChestMenu chestMenu) {
            return !(chestMenu.getContainer() instanceof ContainerEntity);
        }

        String menuClassName = screen.getMenu().getClass().getName();
        return "net.minecraft.world.inventory.DispenserMenu".equals(menuClassName)
                || "net.minecraft.world.inventory.HopperMenu".equals(menuClassName)
                || "net.minecraft.world.inventory.ShulkerBoxMenu".equals(menuClassName);
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        boolean inventoryUiEnabled = BlockZConfigs.isDayzInventoryEnabled() && ClientSettings.dayzEnabled;
        if (event.getScreen() instanceof InventoryScreen) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            
            // 只有当 DayZ UI 启用时才拦截
            if (inventoryUiEnabled) {
                BlockZ.LOGGER.info("Intercepting InventoryScreen opening, sending OpenDayZMenuC2S to server. dayzEnabled={}, inventoryUiEnabled={}", ClientSettings.dayzEnabled, inventoryUiEnabled);
                NetworkHandler.CHANNEL.sendToServer(new OpenDayZMenuC2S());
                if (!isArclightServer()) {
                    event.setCanceled(true);
                }
            } else {
                BlockZ.LOGGER.info("Allowing InventoryScreen opening. dayzEnabled={}, inventoryUiEnabled={}", ClientSettings.dayzEnabled, inventoryUiEnabled);
            }
        } else if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            if (inventoryUiEnabled) {
                if (event.getScreen() instanceof com.yitianys.BlockZ.client.gui.DayZInventoryScreen) return;
                if (event.getScreen() instanceof com.yitianys.BlockZ.client.gui.DayZChestScreen) return;

                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return;
                if (containerScreen.getMenu() == mc.player.inventoryMenu) return;

                if (isArclightServer() && containerScreen.getMenu() instanceof ChestMenu chestMenu) {
                    event.setCanceled(true);
                    mc.setScreen(new com.yitianys.BlockZ.client.gui.DayZChestScreen(chestMenu, mc.player.getInventory(), containerScreen.getTitle()));
                    return;
                }

                if (shouldSkipDayZOverride(containerScreen)) {
                    return;
                }

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
                if (!isArclightServer()) {
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

    private static void tryPickup(PlayerInteractEvent event) {
        if (!event.getLevel().isClientSide) return;
        if (!ClientSettings.dayzEnabled) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Entity entity = InventoryUtils.getTargetedItemEntity(mc.player, 4.0); // 4 blocks reach
        if (entity != null) {
            NetworkHandler.CHANNEL.sendToServer(new LootPickupC2S(entity.getId()));
            if (event.isCancelable()) {
                event.setCanceled(true);
            }
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

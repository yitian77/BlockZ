package com.yitianys.BlockZ.compat;

import com.yitianys.BlockZ.capability.PlayerBackpack;
import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.menu.StorageRefreshableMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class TaczAmmoCompat {
    private static final ResourceLocation FALLBACK_EMPTY_AMMO_ID = new ResourceLocation("tacz", "empty");

    private TaczAmmoCompat() {
    }

    public static boolean hasCompatibleAmmo(Player player, ItemStack gunStack) {
        return player != null && !gunStack.isEmpty() && extractCompatibleAmmo(player, gunStack, 1, true) > 0;
    }

    public static int extractCompatibleAmmo(Player player, ItemStack gunStack, int neededAmount) {
        return extractCompatibleAmmo(player, gunStack, neededAmount, false);
    }

    public static int countCompatibleAmmo(Player player, ItemStack gunStack) {
        return Math.min(9999, extractCompatibleAmmo(player, gunStack, 9999, true));
    }

    public static int countAmmoStack(ItemStack ammoStack, ItemStack gunStack) {
        if (ammoStack.isEmpty() || gunStack.isEmpty()) {
            return 0;
        }
        if (isAmmoOfGun(ammoStack, gunStack)) {
            return ammoStack.getCount();
        }
        if (!isAmmoBoxOfGun(ammoStack, gunStack)) {
            return 0;
        }
        if (invokeBoolean(ammoStack.getItem(), "isAllTypeCreative", new Class<?>[]{ItemStack.class}, ammoStack)
                || invokeBoolean(ammoStack.getItem(), "isCreative", new Class<?>[]{ItemStack.class}, ammoStack)) {
            return 9999;
        }
        return Math.max(0, getAmmoBoxCount(ammoStack));
    }

    public static boolean canReloadWithCompatibleAmmo(Object gunItem, LivingEntity shooter, ItemStack gunStack) {
        if (invokeBoolean(gunItem, "canReload", new Class<?>[]{LivingEntity.class, ItemStack.class}, shooter, gunStack)) {
            return true;
        }
        return shooter instanceof Player player && hasCompatibleAmmo(player, gunStack);
    }

    public static int extractCompatibleAmmo(Player player, ItemStack gunStack, int neededAmount, boolean simulate) {
        if (player == null || gunStack.isEmpty() || neededAmount <= 0) {
            return 0;
        }

        int[] extracted = new int[1];
        player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
            ItemStackHandler equipment = cap.getInventory();
            ItemStack vestStack = CuriosIntegration.getEquippedDirect(player, CuriosIntegration.SLOT_BODY);
            if (vestStack.isEmpty()) {
                vestStack = equipment.getStackInSlot(PlayerBackpack.SLOT_VEST);
            }
            extracted[0] += extractFromStorageStack(vestStack, gunStack, neededAmount - extracted[0], simulate);
            if (extracted[0] < neededAmount) {
                extracted[0] += extractFromStorageStack(player.getItemBySlot(EquipmentSlot.CHEST), gunStack, neededAmount - extracted[0], simulate);
            }
        });

        if (extracted[0] < neededAmount) {
            extracted[0] += extractFromStorageStack(player.getItemBySlot(EquipmentSlot.LEGS), gunStack, neededAmount - extracted[0], simulate);
        }
        if (!simulate && extracted[0] > 0) {
            refreshOpenStorageMenu(player);
        }
        return extracted[0];
    }

    private static int extractFromStorageStack(ItemStack storageStack, ItemStack gunStack, int neededAmount, boolean simulate) {
        if (storageStack.isEmpty() || neededAmount <= 0) {
            return 0;
        }

        int extracted = 0;
        IItemHandler handler = storageStack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (handler != null) {
            extracted += extractFromItemHandler(handler, gunStack, neededAmount, simulate);
            if (extracted >= neededAmount) {
                return extracted;
            }
        }

        CompoundTag tag = storageStack.getTag();
        if (tag == null || !tag.contains("Inventory")) {
            return extracted;
        }

        int capacity = Math.max(BlockZConfigs.getBackpackSlots(storageStack), 1);
        ItemStackHandler fallbackHandler = new ItemStackHandler(capacity);
        fallbackHandler.deserializeNBT(tag.getCompound("Inventory"));
        int extractedFromTag = extractFromItemHandler(fallbackHandler, gunStack, neededAmount - extracted, simulate);
        if (!simulate && extractedFromTag > 0) {
            if (hasAnyItems(fallbackHandler)) {
                tag.put("Inventory", fallbackHandler.serializeNBT());
            } else {
                tag.remove("Inventory");
                if (tag.isEmpty()) {
                    storageStack.setTag(null);
                }
            }
        }
        return extracted + extractedFromTag;
    }

    private static int extractFromItemHandler(IItemHandler handler, ItemStack gunStack, int neededAmount, boolean simulate) {
        int extracted = 0;
        for (int slot = 0; slot < handler.getSlots() && extracted < neededAmount; slot++) {
            ItemStack candidate = handler.getStackInSlot(slot);
            if (candidate.isEmpty()) {
                continue;
            }

            if (isAmmoOfGun(candidate, gunStack)) {
                int remaining = neededAmount - extracted;
                if (simulate) {
                    extracted += Math.min(candidate.getCount(), remaining);
                } else {
                    extracted += handler.extractItem(slot, remaining, false).getCount();
                }
                continue;
            }

            if (isAmmoBoxOfGun(candidate, gunStack)) {
                int boxAmmoCount = getAmmoBoxCount(candidate);
                if (boxAmmoCount <= 0) {
                    continue;
                }
                int take = Math.min(boxAmmoCount, neededAmount - extracted);
                if (!simulate && take > 0) {
                    int remain = boxAmmoCount - take;
                    setAmmoBoxCount(candidate, remain);
                    if (remain <= 0) {
                        clearAmmoBoxId(candidate);
                    }
                }
                extracted += take;
            }
        }
        return extracted;
    }

    private static boolean hasAnyItems(ItemStackHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void refreshOpenStorageMenu(Player player) {
        if (player.containerMenu instanceof StorageRefreshableMenu menu) {
            menu.blockz$refreshStorageAfterExternalChange();
        }
    }

    private static boolean isAmmoOfGun(ItemStack ammoStack, ItemStack gunStack) {
        return invokeBoolean(ammoStack.getItem(), "isAmmoOfGun", new Class<?>[]{ItemStack.class, ItemStack.class}, gunStack, ammoStack);
    }

    private static boolean isAmmoBoxOfGun(ItemStack ammoBoxStack, ItemStack gunStack) {
        return invokeBoolean(ammoBoxStack.getItem(), "isAmmoBoxOfGun", new Class<?>[]{ItemStack.class, ItemStack.class}, gunStack, ammoBoxStack);
    }

    private static int getAmmoBoxCount(ItemStack ammoBoxStack) {
        return invokeInt(ammoBoxStack.getItem(), "getAmmoCount", new Class<?>[]{ItemStack.class}, ammoBoxStack);
    }

    private static void setAmmoBoxCount(ItemStack ammoBoxStack, int count) {
        invokeVoid(ammoBoxStack.getItem(), "setAmmoCount", new Class<?>[]{ItemStack.class, int.class}, ammoBoxStack, count);
    }

    private static void clearAmmoBoxId(ItemStack ammoBoxStack) {
        invokeVoid(ammoBoxStack.getItem(), "setAmmoId", new Class<?>[]{ItemStack.class, ResourceLocation.class}, ammoBoxStack, resolveEmptyAmmoId());
    }

    private static ResourceLocation resolveEmptyAmmoId() {
        try {
            Class<?> clazz = Class.forName("com.tacz.guns.api.DefaultAssets");
            Field field = clazz.getField("EMPTY_AMMO_ID");
            Object value = field.get(null);
            if (value instanceof ResourceLocation location) {
                return location;
            }
        } catch (Exception ignored) {
        }
        return FALLBACK_EMPTY_AMMO_ID;
    }

    private static boolean invokeBoolean(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            Object value = method.invoke(target, args);
            return value instanceof Boolean result && result;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int invokeInt(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            Object value = method.invoke(target, args);
            return value instanceof Number number ? number.intValue() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static void invokeVoid(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            method.invoke(target, args);
        } catch (Exception ignored) {
        }
    }
}

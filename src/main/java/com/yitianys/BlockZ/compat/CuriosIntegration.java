package com.yitianys.BlockZ.compat;

import com.yitianys.BlockZ.capability.PlayerBackpack;
import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Map;

public final class CuriosIntegration {

    public static final String SLOT_BACK = "back";
    public static final String SLOT_BODY = "body";
    public static final String SLOT_HEAD = "head";

    private CuriosIntegration() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("curios");
    }

    public static boolean supportsSlot(@Nullable Player player, ItemStack stack, String slotId) {
        if (!isLoaded() || player == null || stack.isEmpty()) {
            return false;
        }
        Map<String, ISlotType> slots = CuriosApi.getItemStackSlots(stack, player.level());
        return slots.containsKey(slotId);
    }

    public static ItemStack getEquipped(Player player, String slotId) {
        if (!isLoaded() || player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack direct = getEquippedDirect(player, slotId);
        return direct.isEmpty() ? ItemStack.EMPTY : direct.copy();
    }

    public static ItemStack getEquippedDirect(Player player, String slotId) {
        if (!isLoaded() || player == null) {
            return ItemStack.EMPTY;
        }
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.getStacksHandler(slotId)
                        .map(stacks -> {
                            IDynamicStackHandler dynamic = stacks.getStacks();
                            if (dynamic.getSlots() <= 0) {
                                return ItemStack.EMPTY;
                            }
                            ItemStack equipped = dynamic.getStackInSlot(0);
                            return equipped.isEmpty() ? ItemStack.EMPTY : equipped;
                        }).orElse(ItemStack.EMPTY))
                .orElse(ItemStack.EMPTY);
    }

    public static void setEquipped(Player player, String slotId, ItemStack stack) {
        if (!isLoaded() || player == null) {
            return;
        }
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.getStacksHandler(slotId).ifPresent(stacks -> {
                    IDynamicStackHandler dynamic = stacks.getStacks();
                    if (dynamic.getSlots() > 0) {
                        dynamic.setStackInSlot(0, safeCopy(stack));
                    }
                }));
    }

    public static void importToCapability(ServerPlayer player) {
        if (!isLoaded() || player == null) {
            return;
        }
        player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
            ItemStackHandler handler = cap.getInventory();
            syncHandlerSlot(handler, PlayerBackpack.SLOT_BACKPACK, getEquipped(player, SLOT_BACK));
            syncHandlerSlot(handler, PlayerBackpack.SLOT_VEST, getEquipped(player, SLOT_BODY));
            syncHandlerSlot(handler, PlayerBackpack.SLOT_MASK, getEquipped(player, SLOT_HEAD));
        });
    }

    private static void syncHandlerSlot(ItemStackHandler handler, int slot, ItemStack stack) {
        ItemStack current = handler.getStackInSlot(slot);
        if (!ItemStack.isSameItemSameTags(current, stack)) {
            handler.setStackInSlot(slot, safeCopy(stack));
        }
    }

    private static ItemStack safeCopy(ItemStack stack) {
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    public static void syncFromCapability(ServerPlayer player, int capSlotId, ItemStack stack) {
        if (!isLoaded() || player == null) {
            return;
        }
        String slotId = switch (capSlotId) {
            case PlayerBackpack.SLOT_BACKPACK -> SLOT_BACK;
            case PlayerBackpack.SLOT_VEST -> SLOT_BODY;
            case PlayerBackpack.SLOT_MASK -> SLOT_HEAD;
            default -> null;
        };
        if (slotId != null) {
            setEquipped(player, slotId, stack);
        }
    }
}

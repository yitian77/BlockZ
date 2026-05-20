package com.yitianys.BlockZ.compat;

import com.yitianys.BlockZ.capability.PlayerBackpack;
import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CuriosIntegration {

    public static final String SLOT_BACK = "back";
    public static final String SLOT_BODY = "body";
    public static final String SLOT_HEAD = "head";

    public record CurioSlotRef(String identifier, int slotIndex) {
    }

    public record CurioMenuSlot(CurioSlotRef ref, IItemHandlerModifiable handler, boolean available) {
    }

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

    public static @Nullable ResourceLocation getSlotIcon(@Nullable Player player, @Nullable String slotId) {
        if (!isLoaded() || player == null || slotId == null || slotId.isBlank()) {
            return null;
        }
        return CuriosApi.getSlot(slotId, player.level()).map(ISlotType::getIcon).orElse(null);
    }

    public static @Nullable String getSlotTranslationKey(@Nullable String slotId) {
        if (slotId == null || slotId.isBlank()) {
            return null;
        }
        return "curios.identifier." + slotId;
    }

    public static int getSlotOrder(@Nullable Player player, @Nullable String slotId) {
        if (!isLoaded() || player == null || slotId == null || slotId.isBlank()) {
            return 1000;
        }
        return CuriosApi.getSlot(slotId, player.level()).map(ISlotType::getOrder).orElse(1000);
    }

    public static String getSlotGroupKey(@Nullable Player player, @Nullable String slotId) {
        if (slotId == null || slotId.isBlank()) {
            return "curios";
        }
        ResourceLocation icon = getSlotIcon(player, slotId);
        if (icon != null && !isGenericSlotIcon(icon)) {
            return icon.getNamespace();
        }
        return "curios";
    }

    public static String getSlotGroupLabel(@Nullable String groupKey) {
        if (groupKey == null || groupKey.isBlank() || "curios".equals(groupKey)) {
            return "GEAR";
        }
        Optional<? extends ModContainer> modContainer = ModList.get().getModContainerById(groupKey);
        if (modContainer.isPresent()) {
            return modContainer.get().getModInfo().getDisplayName();
        }
        return formatDisplayName(groupKey);
    }

    public static boolean isGenericSlotIcon(@Nullable ResourceLocation icon) {
        return icon != null && "curios".equals(icon.getNamespace()) && icon.getPath().contains("empty_curio");
    }

    private static String formatDisplayName(String value) {
        String normalized = value.replace('-', ' ').replace('_', ' ').trim();
        StringBuilder builder = new StringBuilder(normalized.length());
        boolean upper = true;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c == ' ') {
                builder.append(c);
                upper = true;
                continue;
            }
            builder.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
            upper = false;
        }
        return builder.toString();
    }

    public static List<CurioSlotRef> collectAdditionalDayZSlotRefs(@Nullable Player player) {
        if (!isLoaded() || player == null) {
            return List.of();
        }
        return CuriosApi.getCuriosInventory(player).map(handler -> {
            List<CurioSlotRef> refs = new ArrayList<>();
            for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
                String slotId = entry.getKey();
                if (isMirroredDayZSlot(slotId)) {
                    continue;
                }
                ICurioStacksHandler stacksHandler = entry.getValue();
                if (stacksHandler == null || !stacksHandler.isVisible()) {
                    continue;
                }
                IDynamicStackHandler dynamic = stacksHandler.getStacks();
                for (int i = 0; i < dynamic.getSlots(); i++) {
                    refs.add(new CurioSlotRef(slotId, i));
                }
            }
            refs.sort(Comparator.comparing(CurioSlotRef::identifier).thenComparingInt(CurioSlotRef::slotIndex));
            return refs;
        }).orElse(List.of());
    }

    public static List<CurioMenuSlot> resolveAdditionalDayZSlots(@Nullable Player player, @Nullable List<CurioSlotRef> refs) {
        if (!isLoaded() || player == null) {
            return List.of();
        }
        List<CurioSlotRef> targetRefs = new ArrayList<>((refs == null || refs.isEmpty()) ? collectAdditionalDayZSlotRefs(player) : refs);
        targetRefs.sort(Comparator.comparing(CurioSlotRef::identifier).thenComparingInt(CurioSlotRef::slotIndex));

        List<CurioMenuSlot> resolved = new ArrayList<>(targetRefs.size());
        for (CurioSlotRef ref : targetRefs) {
            Optional<IDynamicStackHandler> dynamicHandler = resolveDynamicHandler(player, ref);
            IItemHandlerModifiable handler = dynamicHandler.<IItemHandlerModifiable>map(value -> value)
                    .orElseGet(() -> new ItemStackHandler(Math.max(ref.slotIndex() + 1, 1)));
            resolved.add(new CurioMenuSlot(ref, handler, dynamicHandler.isPresent()));
        }
        return resolved;
    }

    public static void writeAdditionalDayZSlotRefs(@Nullable Player player, FriendlyByteBuf buf) {
        List<CurioSlotRef> refs = collectAdditionalDayZSlotRefs(player);
        buf.writeVarInt(refs.size());
        for (CurioSlotRef ref : refs) {
            String identifier = ref.identifier();
            buf.writeUtf(identifier == null ? "" : identifier);
            buf.writeVarInt(ref.slotIndex());
        }
    }

    public static List<CurioSlotRef> readAdditionalDayZSlotRefs(FriendlyByteBuf buf) {
        if (buf == null || !buf.isReadable()) {
            return List.of();
        }
        int count = buf.readVarInt();
        List<CurioSlotRef> refs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            refs.add(new CurioSlotRef(buf.readUtf(), buf.readVarInt()));
        }
        refs.sort(Comparator.comparing(CurioSlotRef::identifier).thenComparingInt(CurioSlotRef::slotIndex));
        return refs;
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
            if (hasSlotHandler(player, SLOT_BACK)) {
                ItemStack curio = getEquipped(player, SLOT_BACK);
                syncHandlerSlot(handler, PlayerBackpack.SLOT_BACKPACK, safeCopy(curio));
            }
            if (hasSlotHandler(player, SLOT_BODY)) {
                ItemStack curio = getEquipped(player, SLOT_BODY);
                syncHandlerSlot(handler, PlayerBackpack.SLOT_VEST, safeCopy(curio));
            }
            if (hasSlotHandler(player, SLOT_HEAD)) {
                ItemStack curio = getEquipped(player, SLOT_HEAD);
                syncHandlerSlot(handler, PlayerBackpack.SLOT_MASK, safeCopy(curio));
            }
        });
    }

    private static boolean hasSlotHandler(Player player, String slotId) {
        if (!isLoaded() || player == null) {
            return false;
        }
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.getStacksHandler(slotId)
                        .map(stacks -> stacks.getStacks().getSlots() > 0)
                        .orElse(false))
                .orElse(false);
    }

    private static boolean isMirroredDayZSlot(String slotId) {
        return SLOT_BACK.equals(slotId) || SLOT_BODY.equals(slotId) || SLOT_HEAD.equals(slotId);
    }

    private static Optional<IDynamicStackHandler> resolveDynamicHandler(Player player, CurioSlotRef ref) {
        return CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(handler -> handler.getStacksHandler(ref.identifier())
                        .map(ICurioStacksHandler::getStacks)
                        .filter(stacks -> ref.slotIndex() >= 0 && ref.slotIndex() < stacks.getSlots()));
    }

    private static void syncHandlerSlot(ItemStackHandler handler, int slot, ItemStack stack) {
        ItemStack current = handler.getStackInSlot(slot);
        // 仅在 Curios 端的物品不为空时才同步到 Capability
        // 这样如果 Curios 因为某种原因（如初始化顺序或配置）没能提供物品，
        // 我们能保留 Capability 自身从 NBT 加载的数据，防止物品消失。
        if (!stack.isEmpty() && !ItemStack.isSameItemSameTags(current, stack)) {
            handler.setStackInSlot(slot, safeCopy(stack));
        }
    }

    public static ItemStack createMirrorStack(ItemStack stack) {
        ItemStack mirror = safeCopy(stack);
        if (mirror.isEmpty() || !mirror.hasTag()) {
            return mirror;
        }

        CompoundTag tag = mirror.getTag();
        if (tag == null) {
            return mirror;
        }

        tag.remove("Inventory");
        tag.remove("inventory");
        if (tag.isEmpty()) {
            mirror.setTag(null);
        }
        return mirror;
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

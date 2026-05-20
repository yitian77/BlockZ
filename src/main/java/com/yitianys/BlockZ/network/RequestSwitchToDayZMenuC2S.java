package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.compat.CuriosIntegration;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import com.yitianys.BlockZ.util.ItemHandlerContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class RequestSwitchToDayZMenuC2S {
    private final Component title;

    public RequestSwitchToDayZMenuC2S(Component title) {
        this.title = title;
    }

    public static void encode(RequestSwitchToDayZMenuC2S msg, FriendlyByteBuf buf) {
        buf.writeComponent(msg.title);
    }

    public static RequestSwitchToDayZMenuC2S decode(FriendlyByteBuf buf) {
        return new RequestSwitchToDayZMenuC2S(buf.readComponent());
    }

    public static void handle(RequestSwitchToDayZMenuC2S msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (!BlockZConfigs.isDayzInventoryEnabled()) return;
            
            // Check current open container
            AbstractContainerMenu menu = player.containerMenu;
            if (menu == null || menu == player.inventoryMenu) return;

            // 尝试从 Slot 获取 Container / IItemHandler（模组常用 ItemHandler）
            Container targetContainer = null;
            Set<IItemHandler> handlers = new LinkedHashSet<>();
            if (menu instanceof ChestMenu chestMenu) {
                targetContainer = chestMenu.getContainer();
            } else {
                // 遍历 Slot 寻找非玩家背包的 Container
                for (Slot slot : menu.slots) {
                    IItemHandler handlerFromSlot = resolveHandlerFromSlot(slot);
                    if (handlerFromSlot != null) {
                        handlers.add(handlerFromSlot);
                        continue;
                    }
                    if (slot.container != player.getInventory()) {
                        targetContainer = slot.container;
                        break;
                    }
                }
            }

            if (targetContainer == null && !handlers.isEmpty()) {
                IItemHandler handler;
                if (handlers.size() == 1) {
                    handler = handlers.iterator().next();
                } else {
                    List<IItemHandlerModifiable> modifiableHandlers = new ArrayList<>();
                    for (IItemHandler itemHandler : handlers) {
                        if (itemHandler instanceof IItemHandlerModifiable modifiable) {
                            modifiableHandlers.add(modifiable);
                        }
                    }
                    if (modifiableHandlers.size() == handlers.size()) {
                        handler = new CombinedInvWrapper(modifiableHandlers.toArray(new IItemHandlerModifiable[0]));
                    } else {
                        handler = handlers.iterator().next();
                    }
                }
                targetContainer = new ItemHandlerContainer(handler);
            }

            if (targetContainer != null) {
                final Container container = targetContainer; // for lambda
                
                // Open new DayZInventoryMenu with the same container
                NetworkHooks.openScreen(player, new SimpleMenuProvider(
                    (id, inv, p) -> new DayZInventoryMenu(id, inv, container, false),
                    msg.title
                ), buf -> {
                    buf.writeInt(com.yitianys.BlockZ.config.BlockZConfigs.getInitialPocketSlots());
                    buf.writeBoolean(false); // No Pos
                    // New Protocol:
                    // boolean hasPos (false)
                    // byte type (2 = Virtual Container)
                    // int containerSize
                    buf.writeByte(2); // Type: Virtual Container
                    buf.writeInt(container.getContainerSize());
                    CuriosIntegration.writeAdditionalDayZSlotRefs(player, buf);
                });
            } else {
                // 如果找不到 Container，可能是纯逻辑 Menu，无法转换
                // 可以发送一个错误消息给玩家
                // player.sendSystemMessage(Component.literal("无法转换为 DayZ 界面：未找到有效容器"));
            }
        });
        ctx.setPacketHandled(true);
    }

    private static IItemHandler resolveHandlerFromSlot(Slot slot) {
        try {
            Method method = slot.getClass().getMethod("getItemHandler");
            if (IItemHandler.class.isAssignableFrom(method.getReturnType())) {
                Object result = method.invoke(slot);
                if (result instanceof IItemHandler handler) return handler;
            }
        } catch (Exception ignored) {
        }

        try {
            for (Field field : slot.getClass().getDeclaredFields()) {
                if (!IItemHandler.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                Object value = field.get(slot);
                if (value instanceof IItemHandler handler) return handler;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}

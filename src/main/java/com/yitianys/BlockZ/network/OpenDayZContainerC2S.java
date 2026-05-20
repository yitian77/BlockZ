package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.compat.CuriosIntegration;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Nameable;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class OpenDayZContainerC2S {
    private static final byte TYPE_POS = 1;
    private static final byte TYPE_ENTITY = 2;

    private final byte type;
    private final BlockPos pos;
    private final int entityId;

    public static OpenDayZContainerC2S forPos(BlockPos pos) {
        return new OpenDayZContainerC2S(TYPE_POS, pos, -1);
    }

    public static OpenDayZContainerC2S forEntity(int entityId) {
        return new OpenDayZContainerC2S(TYPE_ENTITY, null, entityId);
    }

    private OpenDayZContainerC2S(byte type, BlockPos pos, int entityId) {
        this.type = type;
        this.pos = pos;
        this.entityId = entityId;
    }

    public static void encode(OpenDayZContainerC2S msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.type);
        if (msg.type == TYPE_POS && msg.pos != null) {
            buf.writeBlockPos(msg.pos);
        } else if (msg.type == TYPE_ENTITY) {
            buf.writeInt(msg.entityId);
        }
    }

    public static OpenDayZContainerC2S decode(FriendlyByteBuf buf) {
        byte type = buf.readByte();
        if (type == TYPE_POS) {
            BlockPos pos = buf.readBlockPos();
            return new OpenDayZContainerC2S(TYPE_POS, pos, -1);
        }
        if (type == TYPE_ENTITY) {
            int entityId = buf.readInt();
            return new OpenDayZContainerC2S(TYPE_ENTITY, null, entityId);
        }
        return new OpenDayZContainerC2S((byte) 0, null, -1);
    }

    public static void handle(OpenDayZContainerC2S msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (!BlockZConfigs.isDayzInventoryEnabled()) return;

            ServerLevel level = player.serverLevel();

            if (msg.type == TYPE_POS && msg.pos != null) {
                if (!level.isLoaded(msg.pos)) return;
                BlockEntity be = level.getBlockEntity(msg.pos);
                if (be == null) return;
                Component title = be instanceof Nameable nameable ? nameable.getDisplayName() : Component.translatable("container.inventory");

                NetworkHooks.openScreen(player, new SimpleMenuProvider(
                        (id, inv, p) -> new DayZInventoryMenu(id, inv, msg.pos),
                        title
                ), buf -> {
                    buf.writeInt(BlockZConfigs.getInitialPocketSlots());
                    buf.writeBoolean(true);
                    buf.writeBlockPos(msg.pos);
                    CuriosIntegration.writeAdditionalDayZSlotRefs(player, buf);
                });
                return;
            }

            if (msg.type == TYPE_ENTITY) {
                Entity entity = level.getEntity(msg.entityId);
                if (entity == null) return;
                Component title = entity.getDisplayName();

                NetworkHooks.openScreen(player, new SimpleMenuProvider(
                        (id, inv, p) -> new DayZInventoryMenu(id, inv, entity),
                        title
                ), buf -> {
                    buf.writeInt(BlockZConfigs.getInitialPocketSlots());
                    buf.writeBoolean(false);
                    // New Protocol:
                    // boolean hasPos (false)
                    // byte type (1 = Entity)
                    // int entityId
                    buf.writeByte(1); // Type: Entity
                    buf.writeInt(entity.getId());
                    CuriosIntegration.writeAdditionalDayZSlotRefs(player, buf);
                });
            }
        });
        ctx.setPacketHandled(true);
    }
}

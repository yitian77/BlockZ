package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.compat.TaczProneCompat;
import com.yitianys.BlockZ.util.ProneManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class ProneSyncS2C {
    private final UUID playerUuid;
    private final boolean prone;

    public ProneSyncS2C(UUID playerUuid, boolean prone) {
        this.playerUuid = playerUuid;
        this.prone = prone;
    }

    public static void encode(ProneSyncS2C msg, FriendlyByteBuf buf) {
        buf.writeUUID(Objects.requireNonNull(msg.playerUuid));
        buf.writeBoolean(msg.prone);
    }

    public static ProneSyncS2C decode(FriendlyByteBuf buf) {
        return new ProneSyncS2C(Objects.requireNonNull(buf.readUUID()), buf.readBoolean());
    }

    public static void handle(ProneSyncS2C msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> blockz$handleClient(msg)));
        ctx.setPacketHandled(true);
    }

    private static void blockz$handleClient(ProneSyncS2C msg) {
        ProneManager.setClientProneState(msg.playerUuid, msg.prone);
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            Object player = minecraftClass.getField("player").get(minecraft);
            if (player instanceof net.minecraft.world.entity.player.Player localPlayer && localPlayer.getUUID().equals(msg.playerUuid)) {
                if (!msg.prone) {
                    ProneManager.clearAppliedPronePose(localPlayer);
                }
                TaczProneCompat.setClientCrawling(localPlayer, msg.prone);
            }
        } catch (Exception ignored) {
        }
    }
}

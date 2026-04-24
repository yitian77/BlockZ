package com.yitianys.BlockZ.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncPlayerStatusS2C {
    private final float healthPointsRatio;
    private final float healthRatio;
    private final float staminaRatio;
    private final float infectionRatio;

    public SyncPlayerStatusS2C(float healthPointsRatio, float healthRatio, float staminaRatio, float infectionRatio) {
        this.healthPointsRatio = healthPointsRatio;
        this.healthRatio = healthRatio;
        this.staminaRatio = staminaRatio;
        this.infectionRatio = infectionRatio;
    }

    public float getHealthPointsRatio() {
        return healthPointsRatio;
    }

    public float getHealthRatio() {
        return healthRatio;
    }

    public float getStaminaRatio() {
        return staminaRatio;
    }

    public float getInfectionRatio() {
        return infectionRatio;
    }

    public static void encode(SyncPlayerStatusS2C msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.healthPointsRatio);
        buf.writeFloat(msg.healthRatio);
        buf.writeFloat(msg.staminaRatio);
        buf.writeFloat(msg.infectionRatio);
    }

    public static SyncPlayerStatusS2C decode(FriendlyByteBuf buf) {
        return new SyncPlayerStatusS2C(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(SyncPlayerStatusS2C msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.yitianys.BlockZ.client.network.ClientPacketHandler.handleSyncPlayerStatus(msg, ctxSupplier)
        ));
        ctxSupplier.get().setPacketHandled(true);
    }
}

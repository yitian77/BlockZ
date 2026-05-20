package com.yitianys.BlockZ.mixin;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.network.message.ClientMessagePlayerShoot;
import com.yitianys.BlockZ.compat.TaczShootAimOverrideAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Pseudo
@Mixin(targets = "com.tacz.guns.network.message.ClientMessagePlayerShoot", remap = false)
public abstract class MixinClientMessagePlayerShoot implements TaczShootAimOverrideAccess {
    @Shadow(remap = false)
    private long timestamp;

    private boolean blockz$hasShootAimOverride;
    private float blockz$shootAimPitch;
    private float blockz$shootAimYaw;

    @Inject(method = "encode", at = @At("TAIL"), remap = false)
    private static void blockz$encodeAimOverride(ClientMessagePlayerShoot message, FriendlyByteBuf buf, CallbackInfo ci) {
        TaczShootAimOverrideAccess access = (TaczShootAimOverrideAccess) message;
        buf.writeBoolean(access.blockz$hasShootAimOverride());
        if (access.blockz$hasShootAimOverride()) {
            buf.writeFloat(access.blockz$getShootAimPitch());
            buf.writeFloat(access.blockz$getShootAimYaw());
        }
    }

    @Inject(method = "decode", at = @At("RETURN"), remap = false)
    private static void blockz$decodeAimOverride(FriendlyByteBuf buf, CallbackInfoReturnable<ClientMessagePlayerShoot> cir) {
        ClientMessagePlayerShoot message = cir.getReturnValue();
        if (message == null) {
            return;
        }
        boolean hasOverride = buf.readBoolean();
        if (!hasOverride) {
            return;
        }
        ((TaczShootAimOverrideAccess) message).blockz$setShootAimOverride(buf.readFloat(), buf.readFloat());
    }

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private static void blockz$handleShootAimOverride(ClientMessagePlayerShoot message, Supplier<NetworkEvent.Context> contextSupplier, CallbackInfo ci) {
        NetworkEvent.Context context = contextSupplier.get();
        if (!context.getDirection().getReceptionSide().isServer()) {
            return;
        }
        context.enqueueWork(() -> {
            ServerPlayer entity = context.getSender();
            if (entity == null) {
                return;
            }
            IGunOperator operator = IGunOperator.fromLivingEntity(entity);
            TaczShootAimOverrideAccess access = (TaczShootAimOverrideAccess) message;
            long shootTimestamp = ((MixinClientMessagePlayerShoot) (Object) message).blockz$getTimestampInternal();
            if (access.blockz$hasShootAimOverride()) {
                operator.shoot(access::blockz$getShootAimPitch, access::blockz$getShootAimYaw, shootTimestamp);
            } else {
                operator.shoot(entity::getXRot, entity::getYRot, shootTimestamp);
            }
        });
        context.setPacketHandled(true);
        ci.cancel();
    }

    private long blockz$getTimestampInternal() {
        return this.timestamp;
    }

    @Override
    public void blockz$setShootAimOverride(float pitch, float yaw) {
        this.blockz$hasShootAimOverride = true;
        this.blockz$shootAimPitch = pitch;
        this.blockz$shootAimYaw = yaw;
    }

    @Override
    public boolean blockz$hasShootAimOverride() {
        return this.blockz$hasShootAimOverride;
    }

    @Override
    public float blockz$getShootAimPitch() {
        return this.blockz$shootAimPitch;
    }

    @Override
    public float blockz$getShootAimYaw() {
        return this.blockz$shootAimYaw;
    }
}

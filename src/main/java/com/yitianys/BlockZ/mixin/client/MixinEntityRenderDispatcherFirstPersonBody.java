package com.yitianys.BlockZ.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yitianys.BlockZ.client.renderer.FirstPersonBodyRenderHelper;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class MixinEntityRenderDispatcherFirstPersonBody {
    private static final Minecraft BLOCKZ_MINECRAFT = Minecraft.getInstance();

    @Redirect(method = "renderShadow", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 0))
    private static double blockz$offsetShadowX(double delta, double oldValue, double currentValue, PoseStack poseStack,
                                               MultiBufferSource bufferSource, Entity entity, float opacity, float partialTick,
                                               LevelReader levelReader, float radius) {
        return Mth.lerp(delta, oldValue, currentValue) + blockz$getBodyOffset(entity, partialTick).x;
    }

    @Redirect(method = "renderShadow", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 2))
    private static double blockz$offsetShadowZ(double delta, double oldValue, double currentValue, PoseStack poseStack,
                                               MultiBufferSource bufferSource, Entity entity, float opacity, float partialTick,
                                               LevelReader levelReader, float radius) {
        return Mth.lerp(delta, oldValue, currentValue) + blockz$getBodyOffset(entity, partialTick).z;
    }

    @Inject(method = "renderShadow", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;last()Lcom/mojang/blaze3d/vertex/PoseStack$Pose;", shift = Shift.BEFORE))
    private static void blockz$translateShadow(PoseStack poseStack, MultiBufferSource bufferSource, Entity entity, float opacity,
                                               float partialTick, LevelReader levelReader, float radius, CallbackInfo ci) {
        Vec3 bodyOffset = blockz$getBodyOffset(entity, partialTick);
        if (bodyOffset == Vec3.ZERO) {
            return;
        }
        poseStack.translate(bodyOffset.x, bodyOffset.y, bodyOffset.z);
    }

    @Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
    private static void blockz$hideFirstPersonHitbox(PoseStack poseStack, VertexConsumer vertexConsumer, Entity entity, float partialTick, CallbackInfo ci) {
        if (entity == BLOCKZ_MINECRAFT.cameraEntity && BLOCKZ_MINECRAFT.options.getCameraType() == CameraType.FIRST_PERSON) {
            ci.cancel();
        }
    }

    private static Vec3 blockz$getBodyOffset(Entity entity, float partialTick) {
        if (entity != BLOCKZ_MINECRAFT.cameraEntity || BLOCKZ_MINECRAFT.options.getCameraType() != CameraType.FIRST_PERSON) {
            return Vec3.ZERO;
        }
        if (!(entity instanceof AbstractClientPlayer player)) {
            return Vec3.ZERO;
        }
        return FirstPersonBodyRenderHelper.getBodyOffset(player, partialTick);
    }
}

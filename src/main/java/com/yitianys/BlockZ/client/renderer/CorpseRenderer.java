package com.yitianys.BlockZ.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yitianys.BlockZ.entity.CorpseEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.UUID;

public class CorpseRenderer extends LivingEntityRenderer<CorpseEntity, PlayerModel<CorpseEntity>> {
    private final PlayerModel<CorpseEntity> modelDefault;
    private final PlayerModel<CorpseEntity> modelSlim;

    public CorpseRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<CorpseEntity>(context.bakeLayer(ModelLayers.PLAYER), false) {
            @Override
            public void setupAnim(CorpseEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
                // 使尸体保持静止，不执行任何动作
                super.setupAnim(entity, 0, 0, 0, 0, 0);
            }
        }, 0.5F);
        this.modelDefault = this.model;
        this.modelSlim = new PlayerModel<CorpseEntity>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true) {
            @Override
            public void setupAnim(CorpseEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
                super.setupAnim(entity, 0, 0, 0, 0, 0);
            }
        };
        
        // 添加护甲层，并确保护甲也不动
        this.addLayer(new HumanoidArmorLayer<>(this, 
            new HumanoidModel<CorpseEntity>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)) {
                @Override
                public void setupAnim(CorpseEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
                    super.setupAnim(entity, 0, 0, 0, 0, 0);
                }
            }, 
            new HumanoidModel<CorpseEntity>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)) {
                @Override
                public void setupAnim(CorpseEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
                    super.setupAnim(entity, 0, 0, 0, 0, 0);
                }
            }, 
            context.getModelManager()));
    }

    @Override
    public void render(CorpseEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        String modelType = getModelType(entity);
        this.model = "slim".equals(modelType) ? this.modelSlim : this.modelDefault;
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    protected void renderNameTag(CorpseEntity entity, Component displayName, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Do not render name tag for corpse
    }

    @Override
    protected boolean shouldShowName(CorpseEntity entity) {
        return false;
    }

    @Override
    protected void setupRotations(CorpseEntity entity, PoseStack stack, float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, stack, ageInTicks, rotationYaw, partialTicks);
        
        // Rotate to lay flat
        stack.translate(0.0D, 0.13D, 0.0D); // Lift slightly above ground
        stack.mulPose(Axis.XP.rotationDegrees(90.0F)); // Rotate to lie down (Face Up)
        stack.translate(0.0D, -0.85D, 0.0D); // Center the body on the shadow
        
        // No manual Z rotation needed as yBodyRot is now synced
    }

    @Override
    public ResourceLocation getTextureLocation(CorpseEntity entity) {
        Optional<UUID> uuid = entity.getOwnerUUID();
        if (uuid.isPresent()) {
             if (Minecraft.getInstance().getConnection() != null) {
                 PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(uuid.get());
                 if (info != null) {
                     return info.getSkinLocation();
                 }
             }
             return DefaultPlayerSkin.getDefaultSkin(uuid.get());
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }
    
    private String getModelType(CorpseEntity entity) {
        Optional<UUID> uuid = entity.getOwnerUUID();
        if (uuid.isPresent()) {
             if (Minecraft.getInstance().getConnection() != null) {
                 PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(uuid.get());
                 if (info != null) {
                     return info.getModelName();
                 }
             }
             return DefaultPlayerSkin.getSkinModelName(uuid.get());
        }
        return "default";
    }
}

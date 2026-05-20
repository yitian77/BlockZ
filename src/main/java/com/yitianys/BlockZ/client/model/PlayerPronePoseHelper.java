package com.yitianys.BlockZ.client.model;

import com.yitianys.BlockZ.util.ProneManager;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

public final class PlayerPronePoseHelper {
    private static final float PRONE_BODY_X_ROT = 0.08F;
    private static final float PRONE_HEAD_X_ROT = -1.30F;
    private static final float PRONE_HEAD_X_ROT_AIMING = -1.12F;
    private static final float PRONE_RIGHT_ARM_X_ROT = -2.72F;
    private static final float PRONE_LEFT_ARM_X_ROT = -2.95F;
    private static final float PRONE_RIGHT_ARM_X_ROT_AIMING = -2.52F;
    private static final float PRONE_LEFT_ARM_X_ROT_AIMING = -2.68F;
    private static final float PRONE_RIGHT_LEG_X_ROT = 0.22F;
    private static final float PRONE_LEFT_LEG_X_ROT = 0.04F;

    private PlayerPronePoseHelper() {
    }

    public static void applyPronePose(HumanoidModel<?> model, Player player, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!ProneManager.isProne(player)) {
            return;
        }

        float moveAmount = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float crawlWave = Mth.sin(limbSwing * 0.65F) * moveAmount;
        float bodySway = Mth.cos(ageInTicks * 0.10F) * moveAmount * 0.035F;
        boolean aiming = player.isScoping() || player.isUsingItem();
        ItemStack mainHandItem = player.getMainHandItem();
        boolean holdingMainHandItem = !mainHandItem.isEmpty();

        model.crouching = false;
        model.head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.22F;
        model.head.xRot = (aiming ? PRONE_HEAD_X_ROT_AIMING : PRONE_HEAD_X_ROT) + headPitch * Mth.DEG_TO_RAD * 0.12F;
        model.head.zRot = bodySway * 0.35F;

        model.body.xRot = PRONE_BODY_X_ROT;
        model.body.yRot = 0.0F;
        model.body.zRot = bodySway;

        model.rightArm.xRot = (aiming ? PRONE_RIGHT_ARM_X_ROT_AIMING : PRONE_RIGHT_ARM_X_ROT) + crawlWave * 0.18F + (holdingMainHandItem ? 0.24F : 0.0F);
        model.rightArm.yRot = aiming ? 0.18F : 0.08F;
        model.rightArm.zRot = aiming ? -0.04F : 0.14F;

        model.leftArm.xRot = (aiming ? PRONE_LEFT_ARM_X_ROT_AIMING : PRONE_LEFT_ARM_X_ROT) - crawlWave * 0.20F + (holdingMainHandItem ? 0.16F : 0.0F);
        model.leftArm.yRot = aiming ? -0.30F : -0.10F;
        model.leftArm.zRot = aiming ? -0.28F : -0.20F;

        model.rightLeg.xRot = PRONE_RIGHT_LEG_X_ROT - crawlWave * 0.18F;
        model.rightLeg.yRot = 0.08F;
        model.rightLeg.zRot = 0.24F;

        model.leftLeg.xRot = PRONE_LEFT_LEG_X_ROT + crawlWave * 0.18F;
        model.leftLeg.yRot = -0.14F;
        model.leftLeg.zRot = -0.06F;

        if (model instanceof PlayerModel<?> playerModel) {
            syncOuterParts(playerModel);
        } else {
            model.hat.copyFrom(model.head);
        }
    }

    private static void syncOuterParts(PlayerModel<?> playerModel) {
        playerModel.hat.copyFrom(playerModel.head);
        playerModel.jacket.copyFrom(playerModel.body);
        playerModel.leftSleeve.copyFrom(playerModel.leftArm);
        playerModel.rightSleeve.copyFrom(playerModel.rightArm);
        playerModel.leftPants.copyFrom(playerModel.leftLeg);
        playerModel.rightPants.copyFrom(playerModel.rightLeg);
    }
}

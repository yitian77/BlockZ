package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.config.DayZZombieConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncConfigS2C {
    // BlockZConfigs - GUI
    private final int gridCols;
    private final int gridRows;
    private final boolean enableGridSystem;
    private final double uiScale;
    private final boolean enableDayzInventory;
    private final boolean allowPlayerToggleDayz;
    private final boolean showDayzHud;
    private final boolean enableHealthSystem;
    private final boolean showDayzToggleChatHint;
    private final boolean enableNursingSystem;
    private final boolean enableBleeding;
    private final boolean enableBrokenLegs;
    private final double baseBleedingChance;
    private final double brokenLegChanceMultiplier;
    private final double brokenLegMaxChance;
    private final boolean enableVanillaBackpackLock;
    private final int initialPocketSlots;
    private final boolean replaceVanillaWalkBobbing;
    private final double walkSwayStrength;
    private final double walkSwaySpeed;
    private final double idleSwayStrength;
    private final boolean walkSwayRequiresSprint;
    private final boolean enableFocusZoom;
    private final double focusFovMultiplierWalk;
    private final double focusFovMultiplierSprint;
    private final double focusFovSmoothing;
    private final boolean enableRealFirstPerson;
    private final boolean enableThirdPersonShoulderCamera;
    private final boolean thirdPersonShoulderDefaultRight;
    private final double thirdPersonShoulderHorizontalOffset;
    private final double thirdPersonShoulderVerticalOffset;
    private final double thirdPersonShoulderForwardOffset;
    private final double thirdPersonShoulderDistanceOffset;
    private final double thirdPersonShoulderSwitchSmoothing;
    private final boolean disableThirdPersonFrontView;

    // BlockZConfigs - Stamina
    private final boolean enableStaminaSystem;
    private final double staminaMaxCapacity;
    private final double staminaSprintCost;
    private final double staminaJumpCost;
    private final double staminaRecoveryRate;
    private final double staminaWaterPenalty;

    // BlockZConfigs - Backpacks
    private final int backpackCoyoteSlots;
    private final int backpackAliceSlots;
    private final int backpackCzechSlots;
    private final int backpackCzechPouchSlots;
    private final int backpackPatrolPackSlots;
    private final int vest0Slots;
    private final int shirtSlots;
    private final int pantsSlots;

    // BlockZConfigs - Corpse & Atmosphere
    private final int corpseDespawnTime;
    private final boolean enableCorpse;
    private final boolean enableBlueFog;
    private final double blueFogTintStrength;
    private final double blueFogDensity;
    private final double worldDesaturation;
    private final boolean enableVignette;
    private final double vignetteStrength;
    private final boolean enableLeanSystem;
    private final double leanOffset;
    private final double leanAngleDegrees;
    private final double leanAnimationDuration;

    // DayZZombieConfig
    private final boolean zombieEnableCustomSense;
    private final boolean zombieEnableNaturalSpawn;
    private final double zombieCrouchDetectionRange;
    private final double zombieWalkDetectionRange;
    private final double zombieMovingDetectionRange;
    private final double zombieSprintDetectionRange;
    private final double zombieCloseDetectionRange;
    private final double zombieTargetSearchVerticalRange;
    private final int zombieTargetScanInterval;
    private final int zombieTargetScanRandomDelay;
    private final int zombieTargetMemoryTicks;
    private final int zombieAlertInterval;
    private final double zombieAlertRangeHorizontal;
    private final double zombieAlertRangeVertical;
    private final double zombieTrackingExtraRange;
    private final double zombieTrackingMemoryExtraRange;
    private final double zombieMaxHealth;
    private final double zombieMovementSpeed;
    private final double zombieAttackDamage;
    private final double zombieFollowRange;
    private final double zombieArmor;
    private final double zombieKnockbackResistance;
    private final double zombieAttackBleedingChance;
    private final int zombieCorpseStayDuration;
    private final boolean zombieEnableNightBoost;
    private final double zombieNightMovementSpeedMultiplier;
    private final double zombieNightAttackDamageMultiplier;
    private final double zombieNightFollowRangeMultiplier;

    public SyncConfigS2C() {
        this.gridCols = BlockZConfigs.getGridCols();
        this.gridRows = BlockZConfigs.getGridRows();
        this.enableGridSystem = BlockZConfigs.isGridEnabled();
        this.uiScale = BlockZConfigs.getUiScale();
        this.enableDayzInventory = BlockZConfigs.isDayzInventoryEnabled();
        this.allowPlayerToggleDayz = BlockZConfigs.getAllowPlayerToggleDayz();
        this.showDayzHud = BlockZConfigs.getShowDayzHud();
        this.enableHealthSystem = BlockZConfigs.isHealthSystemEnabled();
        this.showDayzToggleChatHint = BlockZConfigs.getShowDayzToggleChatHint();
        this.enableNursingSystem = BlockZConfigs.isNursingEnabled();
        this.enableBleeding = BlockZConfigs.isBleedingEnabled();
        this.enableBrokenLegs = BlockZConfigs.isBrokenLegsEnabled();
        this.baseBleedingChance = BlockZConfigs.getBaseBleedingChance();
        this.brokenLegChanceMultiplier = BlockZConfigs.getBrokenLegChanceMultiplier();
        this.brokenLegMaxChance = BlockZConfigs.getBrokenLegMaxChance();
        this.enableVanillaBackpackLock = BlockZConfigs.getEnableVanillaBackpackLock();
        this.initialPocketSlots = BlockZConfigs.getInitialPocketSlots();
        this.replaceVanillaWalkBobbing = BlockZConfigs.shouldReplaceVanillaWalkBobbing();
        this.walkSwayStrength = BlockZConfigs.getWalkSwayStrength();
        this.walkSwaySpeed = BlockZConfigs.getWalkSwaySpeed();
        this.idleSwayStrength = BlockZConfigs.getIdleSwayStrength();
        this.walkSwayRequiresSprint = BlockZConfigs.isWalkSwayRequiresSprint();
        this.enableFocusZoom = BlockZConfigs.isFocusZoomEnabled();
        this.focusFovMultiplierWalk = BlockZConfigs.getFocusFovMultiplierWalk();
        this.focusFovMultiplierSprint = BlockZConfigs.getFocusFovMultiplierSprint();
        this.focusFovSmoothing = BlockZConfigs.getFocusFovSmoothing();
        this.enableRealFirstPerson = BlockZConfigs.isRealFirstPersonEnabled();
        this.enableThirdPersonShoulderCamera = BlockZConfigs.isThirdPersonShoulderCameraEnabled();
        this.thirdPersonShoulderDefaultRight = BlockZConfigs.isThirdPersonShoulderDefaultRight();
        this.thirdPersonShoulderHorizontalOffset = BlockZConfigs.getThirdPersonShoulderHorizontalOffset();
        this.thirdPersonShoulderVerticalOffset = BlockZConfigs.getThirdPersonShoulderVerticalOffset();
        this.thirdPersonShoulderForwardOffset = BlockZConfigs.getThirdPersonShoulderForwardOffset();
        this.thirdPersonShoulderDistanceOffset = BlockZConfigs.getThirdPersonShoulderDistanceOffset();
        this.thirdPersonShoulderSwitchSmoothing = BlockZConfigs.getThirdPersonShoulderSwitchSmoothing();
        this.disableThirdPersonFrontView = BlockZConfigs.isThirdPersonFrontViewDisabled();

        this.enableStaminaSystem = BlockZConfigs.isStaminaEnabled();
        this.staminaMaxCapacity = BlockZConfigs.getStaminaMaxCapacity();
        this.staminaSprintCost = BlockZConfigs.getStaminaSprintCost();
        this.staminaJumpCost = BlockZConfigs.getStaminaJumpCost();
        this.staminaRecoveryRate = BlockZConfigs.getStaminaRecoveryRate();
        this.staminaWaterPenalty = BlockZConfigs.getStaminaWaterPenalty();

        this.backpackCoyoteSlots = BlockZConfigs.getBackpackCoyoteSlots();
        this.backpackAliceSlots = BlockZConfigs.getBackpackAliceSlots();
        this.backpackCzechSlots = BlockZConfigs.getBackpackCzechSlots();
        this.backpackCzechPouchSlots = BlockZConfigs.getBackpackCzechPouchSlots();
        this.backpackPatrolPackSlots = BlockZConfigs.getBackpackPatrolPackSlots();
        this.vest0Slots = BlockZConfigs.getVest0Slots();
        this.shirtSlots = BlockZConfigs.getShirtSlots();
        this.pantsSlots = BlockZConfigs.getPantsSlots();

        this.corpseDespawnTime = BlockZConfigs.getCorpseDespawnTime();
        this.enableCorpse = BlockZConfigs.getEnableCorpse();
        this.enableBlueFog = BlockZConfigs.getEnableBlueFog();
        this.blueFogTintStrength = BlockZConfigs.getBlueFogTintStrength();
        this.blueFogDensity = BlockZConfigs.getBlueFogDensity();
        this.worldDesaturation = BlockZConfigs.getWorldDesaturation();
        this.enableVignette = BlockZConfigs.getEnableVignette();
        this.vignetteStrength = BlockZConfigs.getVignetteStrength();
        this.enableLeanSystem = BlockZConfigs.isLeanEnabled();
        this.leanOffset = BlockZConfigs.getLeanOffset();
        this.leanAngleDegrees = BlockZConfigs.getLeanAngleDegrees();
        this.leanAnimationDuration = BlockZConfigs.getLeanAnimationDuration();

        this.zombieEnableCustomSense = DayZZombieConfig.isCustomSenseEnabled();
        this.zombieEnableNaturalSpawn = DayZZombieConfig.isNaturalSpawnEnabled();
        this.zombieCrouchDetectionRange = DayZZombieConfig.getCrouchDetectionRange();
        this.zombieWalkDetectionRange = DayZZombieConfig.getWalkDetectionRange();
        this.zombieMovingDetectionRange = DayZZombieConfig.getMovingDetectionRange();
        this.zombieSprintDetectionRange = DayZZombieConfig.getSprintDetectionRange();
        this.zombieCloseDetectionRange = DayZZombieConfig.getCloseDetectionRange();
        this.zombieTargetSearchVerticalRange = DayZZombieConfig.getTargetSearchVerticalRange();
        this.zombieTargetScanInterval = DayZZombieConfig.getTargetScanInterval();
        this.zombieTargetScanRandomDelay = DayZZombieConfig.getTargetScanRandomDelay();
        this.zombieTargetMemoryTicks = DayZZombieConfig.getTargetMemoryTicks();
        this.zombieAlertInterval = DayZZombieConfig.getAlertInterval();
        this.zombieAlertRangeHorizontal = DayZZombieConfig.getAlertRangeHorizontal();
        this.zombieAlertRangeVertical = DayZZombieConfig.getAlertRangeVertical();
        this.zombieTrackingExtraRange = DayZZombieConfig.getTrackingExtraRange();
        this.zombieTrackingMemoryExtraRange = DayZZombieConfig.getTrackingMemoryExtraRange();
        this.zombieMaxHealth = DayZZombieConfig.getMaxHealth();
        this.zombieMovementSpeed = DayZZombieConfig.getMovementSpeed();
        this.zombieAttackDamage = DayZZombieConfig.getAttackDamage();
        this.zombieFollowRange = DayZZombieConfig.getFollowRange();
        this.zombieArmor = DayZZombieConfig.getArmor();
        this.zombieKnockbackResistance = DayZZombieConfig.getKnockbackResistance();
        this.zombieAttackBleedingChance = DayZZombieConfig.getAttackBleedingChance();
        this.zombieCorpseStayDuration = DayZZombieConfig.getCorpseStayDuration();
        this.zombieEnableNightBoost = DayZZombieConfig.isNightBoostEnabled();
        this.zombieNightMovementSpeedMultiplier = DayZZombieConfig.getNightMovementSpeedMultiplier();
        this.zombieNightAttackDamageMultiplier = DayZZombieConfig.getNightAttackDamageMultiplier();
        this.zombieNightFollowRangeMultiplier = DayZZombieConfig.getNightFollowRangeMultiplier();
    }

    public SyncConfigS2C(FriendlyByteBuf buf) {
        this.gridCols = buf.readInt();
        this.gridRows = buf.readInt();
        this.enableGridSystem = buf.readBoolean();
        this.uiScale = buf.readDouble();
        this.enableDayzInventory = buf.readBoolean();
        this.allowPlayerToggleDayz = buf.readBoolean();
        this.showDayzHud = buf.readBoolean();
        this.enableHealthSystem = buf.readBoolean();
        this.showDayzToggleChatHint = buf.readBoolean();
        this.enableNursingSystem = buf.readBoolean();
        this.enableBleeding = buf.readBoolean();
        this.enableBrokenLegs = buf.readBoolean();
        this.baseBleedingChance = buf.readDouble();
        this.brokenLegChanceMultiplier = buf.readDouble();
        this.brokenLegMaxChance = buf.readDouble();
        this.enableVanillaBackpackLock = buf.readBoolean();
        this.initialPocketSlots = buf.readInt();
        this.replaceVanillaWalkBobbing = buf.readBoolean();
        this.walkSwayStrength = buf.readDouble();
        this.walkSwaySpeed = buf.readDouble();
        this.idleSwayStrength = buf.readDouble();
        this.walkSwayRequiresSprint = buf.readBoolean();
        this.enableFocusZoom = buf.readBoolean();
        this.focusFovMultiplierWalk = buf.readDouble();
        this.focusFovMultiplierSprint = buf.readDouble();
        this.focusFovSmoothing = buf.readDouble();
        this.enableRealFirstPerson = buf.readBoolean();
        this.enableThirdPersonShoulderCamera = buf.readBoolean();
        this.thirdPersonShoulderDefaultRight = buf.readBoolean();
        this.thirdPersonShoulderHorizontalOffset = buf.readDouble();
        this.thirdPersonShoulderVerticalOffset = buf.readDouble();
        this.thirdPersonShoulderForwardOffset = buf.readDouble();
        this.thirdPersonShoulderDistanceOffset = buf.readDouble();
        this.thirdPersonShoulderSwitchSmoothing = buf.readDouble();
        this.disableThirdPersonFrontView = buf.readBoolean();

        this.enableStaminaSystem = buf.readBoolean();
        this.staminaMaxCapacity = buf.readDouble();
        this.staminaSprintCost = buf.readDouble();
        this.staminaJumpCost = buf.readDouble();
        this.staminaRecoveryRate = buf.readDouble();
        this.staminaWaterPenalty = buf.readDouble();

        this.backpackCoyoteSlots = buf.readInt();
        this.backpackAliceSlots = buf.readInt();
        this.backpackCzechSlots = buf.readInt();
        this.backpackCzechPouchSlots = buf.readInt();
        this.backpackPatrolPackSlots = buf.readInt();
        this.vest0Slots = buf.readInt();
        this.shirtSlots = buf.readInt();
        this.pantsSlots = buf.readInt();

        this.corpseDespawnTime = buf.readInt();
        this.enableCorpse = buf.readBoolean();
        this.enableBlueFog = buf.readBoolean();
        this.blueFogTintStrength = buf.readDouble();
        this.blueFogDensity = buf.readDouble();
        this.worldDesaturation = buf.readDouble();
        this.enableVignette = buf.readBoolean();
        this.vignetteStrength = buf.readDouble();
        this.enableLeanSystem = buf.readBoolean();
        this.leanOffset = buf.readDouble();
        this.leanAngleDegrees = buf.readDouble();
        this.leanAnimationDuration = buf.readDouble();

        this.zombieEnableCustomSense = buf.readBoolean();
        this.zombieEnableNaturalSpawn = buf.readBoolean();
        this.zombieCrouchDetectionRange = buf.readDouble();
        this.zombieWalkDetectionRange = buf.readDouble();
        this.zombieMovingDetectionRange = buf.readDouble();
        this.zombieSprintDetectionRange = buf.readDouble();
        this.zombieCloseDetectionRange = buf.readDouble();
        this.zombieTargetSearchVerticalRange = buf.readDouble();
        this.zombieTargetScanInterval = buf.readInt();
        this.zombieTargetScanRandomDelay = buf.readInt();
        this.zombieTargetMemoryTicks = buf.readInt();
        this.zombieAlertInterval = buf.readInt();
        this.zombieAlertRangeHorizontal = buf.readDouble();
        this.zombieAlertRangeVertical = buf.readDouble();
        this.zombieTrackingExtraRange = buf.readDouble();
        this.zombieTrackingMemoryExtraRange = buf.readDouble();
        this.zombieMaxHealth = buf.readDouble();
        this.zombieMovementSpeed = buf.readDouble();
        this.zombieAttackDamage = buf.readDouble();
        this.zombieFollowRange = buf.readDouble();
        this.zombieArmor = buf.readDouble();
        this.zombieKnockbackResistance = buf.readDouble();
        this.zombieAttackBleedingChance = buf.readDouble();
        this.zombieCorpseStayDuration = buf.readInt();
        this.zombieEnableNightBoost = buf.readBoolean();
        this.zombieNightMovementSpeedMultiplier = buf.readDouble();
        this.zombieNightAttackDamageMultiplier = buf.readDouble();
        this.zombieNightFollowRangeMultiplier = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(gridCols);
        buf.writeInt(gridRows);
        buf.writeBoolean(enableGridSystem);
        buf.writeDouble(uiScale);
        buf.writeBoolean(enableDayzInventory);
        buf.writeBoolean(allowPlayerToggleDayz);
        buf.writeBoolean(showDayzHud);
        buf.writeBoolean(enableHealthSystem);
        buf.writeBoolean(showDayzToggleChatHint);
        buf.writeBoolean(enableNursingSystem);
        buf.writeBoolean(enableBleeding);
        buf.writeBoolean(enableBrokenLegs);
        buf.writeDouble(baseBleedingChance);
        buf.writeDouble(brokenLegChanceMultiplier);
        buf.writeDouble(brokenLegMaxChance);
        buf.writeBoolean(enableVanillaBackpackLock);
        buf.writeInt(initialPocketSlots);
        buf.writeBoolean(replaceVanillaWalkBobbing);
        buf.writeDouble(walkSwayStrength);
        buf.writeDouble(walkSwaySpeed);
        buf.writeDouble(idleSwayStrength);
        buf.writeBoolean(walkSwayRequiresSprint);
        buf.writeBoolean(enableFocusZoom);
        buf.writeDouble(focusFovMultiplierWalk);
        buf.writeDouble(focusFovMultiplierSprint);
        buf.writeDouble(focusFovSmoothing);
        buf.writeBoolean(enableRealFirstPerson);
        buf.writeBoolean(enableThirdPersonShoulderCamera);
        buf.writeBoolean(thirdPersonShoulderDefaultRight);
        buf.writeDouble(thirdPersonShoulderHorizontalOffset);
        buf.writeDouble(thirdPersonShoulderVerticalOffset);
        buf.writeDouble(thirdPersonShoulderForwardOffset);
        buf.writeDouble(thirdPersonShoulderDistanceOffset);
        buf.writeDouble(thirdPersonShoulderSwitchSmoothing);
        buf.writeBoolean(disableThirdPersonFrontView);

        buf.writeBoolean(enableStaminaSystem);
        buf.writeDouble(staminaMaxCapacity);
        buf.writeDouble(staminaSprintCost);
        buf.writeDouble(staminaJumpCost);
        buf.writeDouble(staminaRecoveryRate);
        buf.writeDouble(staminaWaterPenalty);

        buf.writeInt(backpackCoyoteSlots);
        buf.writeInt(backpackAliceSlots);
        buf.writeInt(backpackCzechSlots);
        buf.writeInt(backpackCzechPouchSlots);
        buf.writeInt(backpackPatrolPackSlots);
        buf.writeInt(vest0Slots);
        buf.writeInt(shirtSlots);
        buf.writeInt(pantsSlots);

        buf.writeInt(corpseDespawnTime);
        buf.writeBoolean(enableCorpse);
        buf.writeBoolean(enableBlueFog);
        buf.writeDouble(blueFogTintStrength);
        buf.writeDouble(blueFogDensity);
        buf.writeDouble(worldDesaturation);
        buf.writeBoolean(enableVignette);
        buf.writeDouble(vignetteStrength);
        buf.writeBoolean(enableLeanSystem);
        buf.writeDouble(leanOffset);
        buf.writeDouble(leanAngleDegrees);
        buf.writeDouble(leanAnimationDuration);

        buf.writeBoolean(zombieEnableCustomSense);
        buf.writeBoolean(zombieEnableNaturalSpawn);
        buf.writeDouble(zombieCrouchDetectionRange);
        buf.writeDouble(zombieWalkDetectionRange);
        buf.writeDouble(zombieMovingDetectionRange);
        buf.writeDouble(zombieSprintDetectionRange);
        buf.writeDouble(zombieCloseDetectionRange);
        buf.writeDouble(zombieTargetSearchVerticalRange);
        buf.writeInt(zombieTargetScanInterval);
        buf.writeInt(zombieTargetScanRandomDelay);
        buf.writeInt(zombieTargetMemoryTicks);
        buf.writeInt(zombieAlertInterval);
        buf.writeDouble(zombieAlertRangeHorizontal);
        buf.writeDouble(zombieAlertRangeVertical);
        buf.writeDouble(zombieTrackingExtraRange);
        buf.writeDouble(zombieTrackingMemoryExtraRange);
        buf.writeDouble(zombieMaxHealth);
        buf.writeDouble(zombieMovementSpeed);
        buf.writeDouble(zombieAttackDamage);
        buf.writeDouble(zombieFollowRange);
        buf.writeDouble(zombieArmor);
        buf.writeDouble(zombieKnockbackResistance);
        buf.writeDouble(zombieAttackBleedingChance);
        buf.writeInt(zombieCorpseStayDuration);
        buf.writeBoolean(zombieEnableNightBoost);
        buf.writeDouble(zombieNightMovementSpeedMultiplier);
        buf.writeDouble(zombieNightAttackDamageMultiplier);
        buf.writeDouble(zombieNightFollowRangeMultiplier);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            BlockZConfigs.setSyncedValues(
                gridCols, gridRows, enableGridSystem, uiScale, enableDayzInventory, allowPlayerToggleDayz,
                showDayzHud, enableHealthSystem, showDayzToggleChatHint, enableNursingSystem, enableBleeding, enableBrokenLegs,
                baseBleedingChance, brokenLegChanceMultiplier, brokenLegMaxChance, enableVanillaBackpackLock, initialPocketSlots,
                replaceVanillaWalkBobbing, walkSwayStrength, walkSwaySpeed, idleSwayStrength,
                walkSwayRequiresSprint, enableFocusZoom, focusFovMultiplierWalk, focusFovMultiplierSprint, focusFovSmoothing,
                enableRealFirstPerson,
                enableThirdPersonShoulderCamera, thirdPersonShoulderDefaultRight,
                thirdPersonShoulderHorizontalOffset, thirdPersonShoulderVerticalOffset,
                thirdPersonShoulderForwardOffset, thirdPersonShoulderDistanceOffset, thirdPersonShoulderSwitchSmoothing,
                disableThirdPersonFrontView,
                enableStaminaSystem, staminaMaxCapacity, staminaSprintCost, staminaJumpCost,
                staminaRecoveryRate, staminaWaterPenalty,
                backpackCoyoteSlots, backpackAliceSlots, backpackCzechSlots, backpackCzechPouchSlots,
                backpackPatrolPackSlots, vest0Slots, shirtSlots, pantsSlots,
                corpseDespawnTime, enableCorpse, enableBlueFog, blueFogTintStrength, blueFogDensity,
                worldDesaturation, enableVignette, vignetteStrength,
                enableLeanSystem, leanOffset, leanAngleDegrees, leanAnimationDuration
            );
            DayZZombieConfig.setSyncedValues(
                zombieEnableCustomSense, zombieEnableNaturalSpawn,
                zombieCrouchDetectionRange, zombieWalkDetectionRange, zombieMovingDetectionRange, zombieSprintDetectionRange,
                zombieCloseDetectionRange, zombieTargetSearchVerticalRange,
                zombieTargetScanInterval, zombieTargetScanRandomDelay, zombieTargetMemoryTicks, zombieAlertInterval,
                zombieAlertRangeHorizontal, zombieAlertRangeVertical, zombieTrackingExtraRange, zombieTrackingMemoryExtraRange,
                zombieMaxHealth, zombieMovementSpeed, zombieAttackDamage, zombieFollowRange,
                zombieArmor, zombieKnockbackResistance, zombieAttackBleedingChance,
                zombieCorpseStayDuration, zombieEnableNightBoost,
                zombieNightMovementSpeedMultiplier, zombieNightAttackDamageMultiplier, zombieNightFollowRangeMultiplier
            );
        });
        context.setPacketHandled(true);
    }
}

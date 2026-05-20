package com.yitianys.BlockZ.compat;

public interface TaczShootAimOverrideAccess {
    void blockz$setShootAimOverride(float pitch, float yaw);

    boolean blockz$hasShootAimOverride();

    float blockz$getShootAimPitch();

    float blockz$getShootAimYaw();
}

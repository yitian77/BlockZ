package com.yitianys.BlockZ.init;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.effect.AnalgesicEffect;
import com.yitianys.BlockZ.effect.BleedingEffect;
import com.yitianys.BlockZ.effect.FractureEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final ResourceLocation FRACTURE_ID = new ResourceLocation(BlockZ.MODID, "fracture");
    public static final ResourceLocation BLEEDING_ID = new ResourceLocation(BlockZ.MODID, "bleeding");
    public static final ResourceLocation ANALGESIC_ID = new ResourceLocation(BlockZ.MODID, "pain_relief");

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, BlockZ.MODID);

    public static final RegistryObject<MobEffect> FRACTURE = EFFECTS.register(FRACTURE_ID.getPath(),
            () -> new FractureEffect(MobEffectCategory.HARMFUL, 0xB4B4B4));

    public static final RegistryObject<MobEffect> BLEEDING = EFFECTS.register(BLEEDING_ID.getPath(),
            () -> new BleedingEffect(MobEffectCategory.HARMFUL, 0x8B0000));

    public static final RegistryObject<MobEffect> ANALGESIC = EFFECTS.register(ANALGESIC_ID.getPath(),
            () -> new AnalgesicEffect(MobEffectCategory.BENEFICIAL, 0x6EA8FF));

    public static MobEffect fracture() {
        return FRACTURE.get();
    }

    public static MobEffect bleeding() {
        return BLEEDING.get();
    }

    public static MobEffect analgesic() {
        return ANALGESIC.get();
    }
}

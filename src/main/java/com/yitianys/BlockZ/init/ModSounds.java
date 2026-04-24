package com.yitianys.BlockZ.init;

import com.yitianys.BlockZ.BlockZ;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BlockZ.MODID);

    public static final RegistryObject<SoundEvent> DAYZ_ZOMBIE_IDLE_1 = register("entity.dayz_zombie.idle1");
    public static final RegistryObject<SoundEvent> DAYZ_ZOMBIE_IDLE_2 = register("entity.dayz_zombie.idle2");
    public static final RegistryObject<SoundEvent> DAYZ_ZOMBIE_IDLE_3 = register("entity.dayz_zombie.idle3");
    public static final RegistryObject<SoundEvent> DAYZ_ZOMBIE_HURT_1 = register("entity.dayz_zombie.hurt1");
    public static final RegistryObject<SoundEvent> DAYZ_ZOMBIE_HURT_2 = register("entity.dayz_zombie.hurt2");
    public static final RegistryObject<SoundEvent> DAYZ_ZOMBIE_HURT_3 = register("entity.dayz_zombie.hurt3");
    public static final RegistryObject<SoundEvent> DAYZ_ZOMBIE_DEATH_1 = register("entity.dayz_zombie.death1");
    public static final RegistryObject<SoundEvent> DAYZ_ZOMBIE_DEATH_2 = register("entity.dayz_zombie.death2");
    public static final RegistryObject<SoundEvent> DAYZ_ZOMBIE_FALL = register("entity.dayz_zombie.fall");

    public static final RegistryObject<SoundEvent> PLAYER_STAMINA_REGEN_EXHAUSTED = register("player.stamina.regen_exhausted");
    public static final RegistryObject<SoundEvent> PLAYER_STAMINA_REGEN_NORMAL = register("player.stamina.regen_normal");
    public static final RegistryObject<SoundEvent> MUSIC_MENU_THEME_0 = register("music.menu_theme_0");
    public static final RegistryObject<SoundEvent> MUSIC_MENU_THEME_1 = register("music.menu_theme_1");
    public static final RegistryObject<SoundEvent> MUSIC_MENU_THEME_2 = register("music.menu_theme_2");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(fromNamespaceAndPath(BlockZ.MODID, name)));
    }
}

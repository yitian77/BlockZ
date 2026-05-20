package com.yitianys.BlockZ.init;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.entity.CorpseEntity;
import com.yitianys.BlockZ.entity.DayZZombieEntity;
import com.yitianys.BlockZ.entity.ZombieCorpseEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BlockZ.MODID);

    public static final RegistryObject<EntityType<DayZZombieEntity>> DAYZ_ZOMBIE =
            ENTITIES.register("dayz_zombie", () -> EntityType.Builder.of(DayZZombieEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 1.95F)
                    .clientTrackingRange(8)
                    .build("dayz_zombie"));

    public static final RegistryObject<EntityType<CorpseEntity>> CORPSE =
            ENTITIES.register("corpse", () -> EntityType.Builder.of((EntityType<CorpseEntity> t, net.minecraft.world.level.Level l) -> new CorpseEntity(t, l), MobCategory.MISC)
                    .sized(1.0F, 0.5F) // Wider hitbox for easier interaction
                    .clientTrackingRange(10)
                    .build("corpse"));

    public static final RegistryObject<EntityType<ZombieCorpseEntity>> DAYZ_ZOMBIE_CORPSE =
            ENTITIES.register("dayz_zombie_corpse", () -> EntityType.Builder.of((EntityType<ZombieCorpseEntity> t, net.minecraft.world.level.Level l) -> new ZombieCorpseEntity(t, l), MobCategory.MISC)
                    .sized(0.9F, 0.6F)
                    .clientTrackingRange(10)
                    .build("dayz_zombie_corpse"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(DAYZ_ZOMBIE.get(), DayZZombieEntity.createAttributes().build());
        event.put(CORPSE.get(), CorpseEntity.createAttributes().build());
        event.put(DAYZ_ZOMBIE_CORPSE.get(), ZombieCorpseEntity.createAttributes().build());
    }

    public static void registerSpawnPlacements() {
        SpawnPlacements.register(DAYZ_ZOMBIE.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
    }
}

package com.yitianys.BlockZ.init;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.entity.CorpseEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BlockZ.MODID);

    public static final RegistryObject<EntityType<CorpseEntity>> CORPSE =
            ENTITIES.register("corpse", () -> EntityType.Builder.of((EntityType<CorpseEntity> t, net.minecraft.world.level.Level l) -> new CorpseEntity(t, l), MobCategory.MISC)
                    .sized(1.0F, 0.5F) // Wider hitbox for easier interaction
                    .clientTrackingRange(10)
                    .build("corpse"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(CORPSE.get(), CorpseEntity.createAttributes().build());
    }
}

package com.yitianys.BlockZ.client.model;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.entity.ZombieCorpseEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class ZombieCorpseModel extends GeoModel<ZombieCorpseEntity> {
    private static final ResourceLocation MODEL = fromNamespaceAndPath(BlockZ.MODID, "geo/dayz_zombie.geo.json");
    private static final ResourceLocation TEXTURE = fromNamespaceAndPath(BlockZ.MODID, "textures/entity/dayz_zombie/dayz_zombie.png");
    private static final ResourceLocation ANIMATION = fromNamespaceAndPath(BlockZ.MODID, "animations/dayz_zombie.animation.json");
    private static final String VARIANT_TEXTURE_ROOT = "textures/entity/dayz_zombie/variants";
    private static final CopyOnWriteArrayList<ResourceLocation> VARIANT_TEXTURES = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean VARIANTS_LOADED = new AtomicBoolean(false);

    @Override
    public ResourceLocation getModelResource(ZombieCorpseEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ZombieCorpseEntity animatable) {
        List<ResourceLocation> variantTextures = getVariantTextures();
        if (variantTextures.isEmpty()) {
            return TEXTURE;
        }
        int index = Math.floorMod(animatable.getVariantSeed(), variantTextures.size());
        return variantTextures.get(index);
    }

    @Override
    public ResourceLocation getAnimationResource(ZombieCorpseEntity animatable) {
        return ANIMATION;
    }

    private static List<ResourceLocation> getVariantTextures() {
        if (!VARIANTS_LOADED.get()) {
            synchronized (VARIANT_TEXTURES) {
                if (!VARIANTS_LOADED.get()) {
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft != null) {
                        minecraft.getResourceManager().listResources(VARIANT_TEXTURE_ROOT, location -> location.getPath().endsWith(".png"))
                                .keySet()
                                .stream()
                                .sorted(Comparator.comparing(ResourceLocation::toString))
                                .forEach(VARIANT_TEXTURES::addIfAbsent);
                    }
                    VARIANTS_LOADED.set(true);
                }
            }
        }
        return VARIANT_TEXTURES;
    }
}

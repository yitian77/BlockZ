package com.yitianys.BlockZ;

import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.config.DayZZombieConfig;
import com.yitianys.BlockZ.client.key.ModKeyMappings;
import com.yitianys.BlockZ.init.ModCreativeTabs;
import com.yitianys.BlockZ.init.ModEntities;
import com.yitianys.BlockZ.init.ModEffects;
import com.yitianys.BlockZ.init.ModItems;
import com.yitianys.BlockZ.init.ModMenus;
import com.yitianys.BlockZ.init.ModSounds;
import com.yitianys.BlockZ.event.ModEvents;
import com.yitianys.BlockZ.network.NetworkHandler;
import com.yitianys.BlockZ.util.ItemSizeManager;
import com.yitianys.BlockZ.util.LeanManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import software.bernie.geckolib.GeckoLib;

@Mod(BlockZ.MODID)
public class BlockZ {
    public static final String MODID = "blockz";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BlockZ(final FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        GeckoLib.initialize();
        BlockZConfigs.register();
        DayZZombieConfig.register();
        context.registerConfig(ModConfig.Type.COMMON, BlockZConfigs.COMMON_SPEC);
        context.registerConfig(ModConfig.Type.COMMON, DayZZombieConfig.COMMON_SPEC, "blockz/dayz_zombie.toml");
        NetworkHandler.init();
        ModEffects.EFFECTS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModEntities.register(modBus);
        modBus.addListener(ModEntities::registerAttributes);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBus);
        ModMenus.MENUS.register(modBus);
        modBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ItemSizeManager.loadCustomSizes();
            ModEntities.registerSpawnPlacements();
        });
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("blockz")
                .requires(stack -> stack.hasPermission(2))
                .then(Commands.literal("reload")
                    .executes(context -> {
                        ItemSizeManager.loadCustomSizes();
                        MinecraftServer server = context.getSource().getServer();
                        ModEvents.broadcastServerConfigs(server);
                        context.getSource().sendSuccess(() -> Component.literal("BlockZ 配置已重载！"), true);
                        return 1;
                    })
                )
        );
    }

    @SubscribeEvent
    public void onConfigReload(ModConfigEvent.Reloading event) {
        ModConfig config = event.getConfig();
        if (config == null || config.getSpec() == null) {
            return;
        }
        if (config.getSpec() != BlockZConfigs.COMMON_SPEC && config.getSpec() != DayZZombieConfig.COMMON_SPEC) {
            return;
        }
        ItemSizeManager.loadCustomSizes();
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ModEvents.broadcastServerConfigs(server);
        }
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!BlockZConfigs.isLeanEnabled()) return;
        if (event.getLevel().isClientSide()) return;

        Entity entity = event.getEntity();
        ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (typeKey == null || !"tacz:bullet".equals(typeKey.toString())) return;

        if (!(entity instanceof Projectile projectile)) return;
        Entity owner = projectile.getOwner();
        if (!(owner instanceof Player player)) return;

        Vec3 leanOffset = LeanManager.getRawLeanCameraOffset(player);
        if (leanOffset.lengthSqr() < 1.0E-6D) return;

        entity.setPos(entity.getX() + leanOffset.x, entity.getY() + leanOffset.y, entity.getZ() + leanOffset.z);
    }
}

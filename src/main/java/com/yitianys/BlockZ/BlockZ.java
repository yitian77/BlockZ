package com.yitianys.BlockZ;

import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.client.key.ModKeyMappings;
import com.yitianys.BlockZ.init.ModCreativeTabs;
import com.yitianys.BlockZ.init.ModItems;
import com.yitianys.BlockZ.init.ModMenus;
import com.yitianys.BlockZ.network.NetworkHandler;
import com.yitianys.BlockZ.util.ItemSizeManager;
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

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@Mod(BlockZ.MODID)
public class BlockZ {
    public static final String MODID = "blockz";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BlockZ(final FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        BlockZConfigs.register();
        context.registerConfig(ModConfig.Type.COMMON, BlockZConfigs.COMMON_SPEC);
        NetworkHandler.init();
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBus);
        ModMenus.MENUS.register(modBus);
        modBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ItemSizeManager::loadCustomSizes);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("blockz")
                .requires(stack -> stack.hasPermission(2))
                .then(Commands.literal("reload")
                    .executes(context -> {
                        ItemSizeManager.loadCustomSizes();
                        context.getSource().sendSuccess(() -> Component.literal("BlockZ 配置已重载！"), true);
                        return 1;
                    })
                )
        );
    }
}

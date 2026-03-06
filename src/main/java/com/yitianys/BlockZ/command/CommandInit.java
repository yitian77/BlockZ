package com.yitianys.BlockZ.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import com.yitianys.BlockZ.network.DayzToggleStateS2C;
import com.yitianys.BlockZ.network.NetworkHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import com.yitianys.BlockZ.network.PacketReloadConfigS2C;
import com.yitianys.BlockZ.util.ItemSizeManager;

import com.yitianys.BlockZ.init.ModEntities;
import com.yitianys.BlockZ.entity.CorpseEntity;
import net.minecraft.world.entity.Entity;
import java.util.Collection;

@Mod.EventBusSubscriber(modid = BlockZ.MODID)
public class CommandInit {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("blockz_clear_corpse")
            .requires(source -> source.hasPermission(2)) // 需要管理员权限
            .executes(context -> clearCorpse(context.getSource(), null))
            .then(Commands.argument("targets", EntityArgument.entities())
                .executes(context -> clearCorpse(context.getSource(), EntityArgument.getEntities(context, "targets")))
            )
        );

        dispatcher.register(Commands.literal("blockz_toggle_ui")
            .requires(source -> source.hasPermission(2)) // 需要管理员权限
            .then(Commands.argument("enabled", BoolArgumentType.bool())
                .executes(context -> toggleUI(context.getSource(), context.getSource().getPlayerOrException(), BoolArgumentType.getBool(context, "enabled")))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(context -> toggleUI(context.getSource(), EntityArgument.getPlayer(context, "target"), BoolArgumentType.getBool(context, "enabled")))
                )
            )
        );

        dispatcher.register(Commands.literal("blockz_reload")
            .requires(source -> source.hasPermission(2)) // 需要管理员权限
            .executes(context -> reloadConfig(context.getSource()))
        );
    }

    private static int clearCorpse(CommandSourceStack source, Collection<? extends Entity> targets) {
        int count = 0;
        if (targets == null) {
            // 清理所有尸体实体
            for (Entity entity : source.getLevel().getEntities().getAll()) {
                if (entity instanceof CorpseEntity) {
                    entity.discard();
                    count++;
                }
            }
        } else {
            // 清理指定的实体（如果是尸体）
            for (Entity entity : targets) {
                if (entity instanceof CorpseEntity) {
                    entity.discard();
                    count++;
                }
            }
        }
        
        final int finalCount = count;
        source.sendSuccess(() -> Component.literal("Successfully cleared " + finalCount + " corpses."), true);
        return count;
    }

    private static int reloadConfig(CommandSourceStack source) {
        try {
            // 1. 重载服务端数据
            ItemSizeManager.loadCustomSizes();
            
            // 2. 通知所有客户端重载
            NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new PacketReloadConfigS2C());
            
            source.sendSuccess(() -> Component.translatable("msg.blockz.command.reload_success"), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("msg.blockz.command.reload_failed"));
            e.printStackTrace();
            return 0;
        }
    }

    private static int toggleUI(CommandSourceStack source, ServerPlayer player, boolean enabled) {
        player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
            cap.setDayzEnabled(enabled);
            // 同步到客户端
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DayzToggleStateS2C(enabled));
            
            source.sendSuccess(() -> Component.translatable("msg.blockz.command.toggle_success", player.getDisplayName(), enabled), true);
        });
        return 1;
    }
}

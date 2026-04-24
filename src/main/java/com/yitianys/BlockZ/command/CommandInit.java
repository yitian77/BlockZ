package com.yitianys.BlockZ.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import com.yitianys.BlockZ.network.DayzToggleStateS2C;
import com.yitianys.BlockZ.network.NetworkHandler;
import com.yitianys.BlockZ.network.SyncGridRulesS2C;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import com.yitianys.BlockZ.util.ItemSizeManager;

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

        dispatcher.register(Commands.literal("blockz_grid_item")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("width", IntegerArgumentType.integer(1, 16))
                .then(Commands.argument("height", IntegerArgumentType.integer(1, 16))
                    .executes(context -> updateHeldItemGridRule(
                        context.getSource(),
                        IntegerArgumentType.getInteger(context, "width"),
                        IntegerArgumentType.getInteger(context, "height"),
                        null
                    ))
                    .then(Commands.argument("color", StringArgumentType.greedyString())
                        .executes(context -> updateHeldItemGridRule(
                            context.getSource(),
                            IntegerArgumentType.getInteger(context, "width"),
                            IntegerArgumentType.getInteger(context, "height"),
                            StringArgumentType.getString(context, "color")
                        ))
                    )
                )
            )
        );

        dispatcher.register(Commands.literal("blockz_clothing_capacity")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("width", IntegerArgumentType.integer(1, 16))
                .then(Commands.argument("height", IntegerArgumentType.integer(1, 16))
                    .executes(context -> updateHeldClothingCapacityRule(
                        context.getSource(),
                        IntegerArgumentType.getInteger(context, "width"),
                        IntegerArgumentType.getInteger(context, "height")
                    ))
                )
            )
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
            ItemSizeManager.loadCustomSizes();
            NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), SyncGridRulesS2C.createServerSnapshot());
            
            source.sendSuccess(() -> Component.translatable("msg.blockz.command.reload_success"), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("msg.blockz.command.reload_failed"));
            e.printStackTrace();
            return 0;
        }
    }

    private static int updateHeldItemGridRule(CommandSourceStack source, int width, int height, String rawColor) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ItemStack heldStack = player.getMainHandItem();
            if (heldStack.isEmpty()) {
                source.sendFailure(Component.translatable("msg.blockz.command.grid_item_empty_hand"));
                return 0;
            }

            String normalizedColor = rawColor == null ? null : rawColor.trim();
            Integer parsedColor = null;
            if (normalizedColor != null && !normalizedColor.isEmpty()) {
                parsedColor = ItemSizeManager.parseColorString(normalizedColor);
                if (parsedColor == null) {
                    source.sendFailure(Component.translatable("msg.blockz.command.grid_item_invalid_color", normalizedColor));
                    return 0;
                }
            }

            final Integer finalParsedColor = parsedColor;
            final Object colorDisplay = finalParsedColor == null
                    ? Component.translatable("msg.blockz.command.grid_item_default_color")
                    : normalizedColor;

            Item item = heldStack.getItem();
            if (!ItemSizeManager.saveItemRule(item, width, height, finalParsedColor)) {
                source.sendFailure(Component.translatable("msg.blockz.command.grid_item_save_failed"));
                return 0;
            }

            NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), SyncGridRulesS2C.createServerSnapshot());

            source.sendSuccess(() -> Component.translatable(
                "msg.blockz.command.grid_item_updated",
                heldStack.getHoverName(),
                width,
                height,
                colorDisplay
            ), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("msg.blockz.command.grid_item_save_failed"));
            BlockZ.LOGGER.error("Failed to update held item grid rule", e);
            return 0;
        }
    }

    private static int updateHeldClothingCapacityRule(CommandSourceStack source, int width, int height) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ItemStack heldStack = player.getMainHandItem();
            if (heldStack.isEmpty()) {
                source.sendFailure(Component.translatable("msg.blockz.command.grid_item_empty_hand"));
                return 0;
            }

            Item item = heldStack.getItem();
            // 移除衣物类型限制，允许任何物品设置容量
            // if (!(item instanceof ClothingItem)) {
            //     source.sendFailure(Component.translatable("msg.blockz.command.grid_item_not_clothing"));
            //     return 0;
            // }

            if (!ItemSizeManager.saveCapacityRule(item, width, height)) {
                source.sendFailure(Component.translatable("msg.blockz.command.grid_item_save_failed"));
                return 0;
            }

            NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), SyncGridRulesS2C.createServerSnapshot());

            source.sendSuccess(() -> Component.translatable(
                "msg.blockz.command.grid_item_updated_capacity",
                heldStack.getHoverName(),
                width,
                height,
                width * height
            ), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("msg.blockz.command.grid_item_save_failed"));
            BlockZ.LOGGER.error("Failed to update held clothing capacity rule", e);
            return 0;
        }
    }

    private static int toggleUI(CommandSourceStack source, ServerPlayer player, boolean enabled) {
        player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
            cap.setDayzEnabled(enabled);
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DayzToggleStateS2C(enabled));
            
            source.sendSuccess(() -> Component.translatable("msg.blockz.command.toggle_success", player.getDisplayName(), enabled), true);
        });
        return 1;
    }
}

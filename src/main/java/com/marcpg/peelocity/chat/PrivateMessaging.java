package com.marcpg.peelocity.chat;

import com.marcpg.lang.Translation;
import com.marcpg.peelocity.Peelocity;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PrivateMessaging {
    public static final Map<UUID, UUID> LAST_SEND_RECEIVERS = new HashMap<>();

    public static @NotNull BrigadierCommand createMsgBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("msg")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Peelocity.SERVER.getAllPlayers().forEach(player -> {
                                if (player != context.getSource()) builder.suggest(player.getUsername());
                            });
                            return builder.buildFuture();
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                                .executes(context -> {
                                    Player player = (Player) context.getSource();
                                    Optional<Player> receiverOptional = Peelocity.SERVER.getPlayer(context.getArgument("player", String.class));
                                    if (receiverOptional.isPresent()) {
                                        String message = context.getArgument("message", String.class);

                                        player.sendMessage(Translation.component(player.getEffectiveLocale(), "private_message.send", receiverOptional.get().getUsername(), message).color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC));
                                        receiverOptional.get().sendMessage(Translation.component(receiverOptional.get().getEffectiveLocale(), "private_message.receive", player.getUsername(), message).color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC));

                                        MessageLogging.saveMessage(player, new MessageLogging.MessageData(new Date(), message, MessageLogging.MessageData.Type.PRIVATE, receiverOptional.get().getUsername()));
                                        LAST_SEND_RECEIVERS.put(player.getUniqueId(), receiverOptional.get().getUniqueId());
                                    } else {
                                        player.sendMessage(Translation.component(player.getEffectiveLocale(), "cmd.player_not_found", context.getArgument("player", String.class)).color(NamedTextColor.RED));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .executes(context -> {
                            context.getSource().sendMessage(Component.text("""
                                    §l§nHelp:§r §l/announcement§r
                                    The command /msg will "whisper" a message to another player.
                                    
                                    §l§nArguments:§r
                                    - §lplayer§r: The player to send the message to.
                                    - §lmessage§r: The content of the message to send.
                                    """));
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }

    public static @NotNull BrigadierCommand createWBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("w")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            if (context.getSource() instanceof Player player) {
                                Optional<Player> receiverOptional = Peelocity.SERVER.getPlayer(LAST_SEND_RECEIVERS.get(player.getUniqueId()));
                                if (receiverOptional.isPresent()) {
                                    String message = context.getArgument("message", String.class);

                                    player.sendMessage(Translation.component(player.getEffectiveLocale(), "private_message.send", receiverOptional.get().getUsername(), message).color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC));
                                    receiverOptional.get().sendMessage(Translation.component(receiverOptional.get().getEffectiveLocale(), "private_message.receive", player.getUsername(), message).color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC));

                                    MessageLogging.saveMessage(player, new MessageLogging.MessageData(new Date(), message, MessageLogging.MessageData.Type.PRIVATE, receiverOptional.get().getUsername()));
                                    LAST_SEND_RECEIVERS.put(player.getUniqueId(), receiverOptional.get().getUniqueId());
                                } else {
                                    player.sendMessage(Translation.component(player.getEffectiveLocale(), "private_message.no_last").color(NamedTextColor.RED));
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .executes(context -> {
                            context.getSource().sendMessage(Component.text("""
                                    §l§nHelp:§r §l/w§r
                                    The command /w will send a message to the last player you private messaged. Acts as a /msg without the player argument.
                                    
                                    §l§nArguments:§r
                                    - §lmessage§r: The content of the message to send.
                                    """));
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }
}

package com.marcpg.peelocity.social;

import com.marcpg.peelocity.Peelocity;
import com.marcpg.peelocity.chat.MessageLogging;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public class FriendSystem {
    public static @NotNull BrigadierCommand createFriendBrigadierCommand(final ProxyServer proxy) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("friend")
                .then(LiteralArgumentBuilder.<CommandSource>literal("add")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    proxy.getAllPlayers().forEach(player -> {
                                        if (player != context.getSource()) builder.suggest(player.getUsername());
                                    });
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    // Add friend, if not friended yet
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("remove")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    proxy.getAllPlayers().forEach(player -> {
                                        if (player != context.getSource()) builder.suggest(player.getUsername());
                                    });
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    // Remove friend, if already friended
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("list")
                        .executes(context -> {
                            // Loop through all friends and send them to the player
                            if (context.getSource() instanceof Player player) {
                                for (String name : List.of("Name1", "Name2")) {
                                    player.sendMessage(Component.text(" - " + name, TextColor.color(180, 255, 180)));
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("invite")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    // Get all friends and suggest them here
                                    List.of("Name1", "Name2").forEach(name -> Peelocity.SERVER.getPlayer(name).ifPresent(player -> builder.suggest(player.getUsername())));
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Optional<Player> targetOptional = Peelocity.SERVER.getPlayer(context.getArgument("player", String.class));
                                    // Also check if the player is in your friend list
                                    if (targetOptional.isPresent() && context.getSource() instanceof Player player) {
                                        targetOptional.get().sendMessage(Component.text("You have been invited to a party by " + player.getUsername(), NamedTextColor.GREEN));
                                        targetOptional.get().sendMessage(Component.text("Type \"/party accept " + player.getUsername() + "\" to accept the invite!", NamedTextColor.DARK_GREEN));
                                        player.sendMessage(Component.text("Successfully invited " + targetOptional.get().getUsername() + " to your party!", NamedTextColor.GREEN));
                                    }

                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("message")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    // Get all friends and suggest them here
                                    List.of("Name1", "Name2").forEach(name -> Peelocity.SERVER.getPlayer(name).ifPresent(player -> builder.suggest(player.getUsername())));
                                    return builder.buildFuture();
                                })
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            Optional<Player> receiverOptional = Peelocity.SERVER.getPlayer(context.getArgument("player", String.class));

                                            if (receiverOptional.isPresent() && context.getSource() instanceof Player player) {
                                                // Check if the receiver is actually friends with the player
                                                String message = context.getArgument("message", String.class);
                                                receiverOptional.get().sendMessage(Component.text(message));
                                                MessageLogging.saveMessage(player, new MessageLogging.MessageData(new Date(), message, MessageLogging.MessageData.Type.FRIEND, receiverOptional.get().getUsername()));
                                            }

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                )
                .build();

        return new BrigadierCommand(node);
    }
}

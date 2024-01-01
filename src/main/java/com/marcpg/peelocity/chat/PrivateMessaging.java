package com.marcpg.peelocity.chat;

import com.marcpg.peelocity.Peelocity;
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
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;

public class PrivateMessaging {
    public static final Map<UUID, UUID> LAST_SEND_RECEIVERS = new HashMap<>();

    public static BrigadierCommand createMsgBrigadier(final ProxyServer proxy) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("msg")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            proxy.getAllPlayers().forEach(player -> {
                                if (player != context.getSource()) builder.suggest(player.getUsername());
                            });
                            return builder.buildFuture();
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                                .executes(context -> {
                                    Optional<Player> receiverOptional = Peelocity.SERVER.getPlayer(context.getArgument("player", String.class));
                                    if (receiverOptional.isPresent() && context.getSource() instanceof Player player) {
                                        String message = context.getArgument("message", String.class);
                                        receiverOptional.get().sendMessage(Component.text("[From: " + player.getUsername() + "] " + message, NamedTextColor.GRAY, TextDecoration.ITALIC));
                                        player.sendMessage(Component.text("[To: " + receiverOptional.get().getUsername() + "] " + message, NamedTextColor.GRAY, TextDecoration.ITALIC));
                                        MessageLogging.saveMessage(player, new MessageLogging.MessageData(new Date(), message, MessageLogging.MessageData.Type.PRIVATE, receiverOptional.get().getUsername()));
                                        LAST_SEND_RECEIVERS.put(player.getUniqueId(), receiverOptional.get().getUniqueId());
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .build();

        return new BrigadierCommand(node);
    }
}

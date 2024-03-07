package com.marcpg.peelocity.features;

import com.marcpg.lang.Translation;
import com.marcpg.peelocity.Peelocity;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PrivateMessaging {
    public static final Map<UUID, String> LAST_RECEIVERS = new HashMap<>();

    @Contract(" -> new")
    public static @NotNull BrigadierCommand msgCommand() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("msg")
                .requires(source -> source instanceof Player)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("receiver", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Peelocity.SERVER.getAllPlayers().forEach(p -> builder.suggest(p.getUsername()));
                            return builder.buildFuture();
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                                .executes(context -> {
                                    Player player = (Player) context.getSource();
                                    String receiverArg = context.getArgument("receiver", String.class);

                                    Peelocity.SERVER.getPlayer(receiverArg).ifPresentOrElse(
                                            r -> {
                                                String message = context.getArgument("message", String.class);

                                                player.sendMessage(Translation.component(player.getEffectiveLocale(), "private_message.send", receiverArg, message)
                                                        .color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC));
                                                r.sendMessage(Translation.component(r.getEffectiveLocale(), "private_message.receive", player.getUsername(), message)
                                                        .color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC));

                                                MessageHistory.saveMessage(player, new MessageHistory.MessageData(new Date(), message, MessageHistory.MessageData.Type.PRIVATE, r.getUsername()));
                                                LAST_RECEIVERS.put(player.getUniqueId(), receiverArg);
                                            },
                                            () -> player.sendMessage(Translation.component(player.getEffectiveLocale(), "cmd.player_not_found", receiverArg).color(NamedTextColor.RED))
                                    );
                                    return 1;
                                })
                        )
                )
                .build()
        );
    }

    @Contract(" -> new")
    public static @NotNull BrigadierCommand wCommand() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("w")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            Player player = (Player) context.getSource();
                            String receiverName = LAST_RECEIVERS.get(player.getUniqueId());

                            if (receiverName == null) {
                                player.sendMessage(Translation.component(player.getEffectiveLocale(), "private_message.no_last").color(NamedTextColor.RED));
                                return 1;
                            }

                            Peelocity.SERVER.getPlayer(receiverName).ifPresentOrElse(
                                    r -> {
                                        String message = context.getArgument("message", String.class);

                                        player.sendMessage(Translation.component(player.getEffectiveLocale(), "private_message.send", receiverName, message)
                                                .color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC));
                                        r.sendMessage(Translation.component(r.getEffectiveLocale(), "private_message.receive", player.getUsername(), message)
                                                .color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC));

                                        MessageHistory.saveMessage(player, new MessageHistory.MessageData(new Date(), message, MessageHistory.MessageData.Type.PRIVATE, receiverName));
                                    },
                                    () -> player.sendMessage(Translation.component(player.getEffectiveLocale(), "cmd.player_not_found", receiverName).color(NamedTextColor.RED))
                            );
                            return 1;
                        })
                )
                .build()
        );
    }
}

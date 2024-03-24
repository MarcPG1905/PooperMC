package com.marcpg.peelocity;

import com.marcpg.libpg.lang.Translation;
import com.marcpg.common.features.MessageLogging;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public class Commands {
    public static @NotNull BrigadierCommand msgHist() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("msg-hist")
                .requires(source -> source.hasPermission("poo.msg-hist") && source instanceof Player)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            PlayerCache.PLAYERS.values().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            Player player = (Player) context.getSource();
                            Locale l = player.getEffectiveLocale();

                            String target = context.getArgument("player", String.class);
                            UUID uuid = PlayerCache.getUuid(target);

                            if (uuid == null) {
                                player.sendMessage(Translation.component(l, "cmd.player_not_found", target).color(NamedTextColor.RED));
                                return 1;
                            }
                            if (MessageLogging.noHistory(uuid)) {
                                player.sendMessage(Translation.component(l, "moderation.chat_history.no_history", target).color(NamedTextColor.RED));
                                return 1;
                            }

                            player.sendMessage(Translation.component(l, "moderation.chat_history.title", target).color(NamedTextColor.DARK_GREEN));
                            MessageLogging.getHistory(uuid).forEach(msg -> {
                                String time = "[" + DateTimeFormatter.ofPattern("MMMM d, HH:mm").format(ZonedDateTime.ofInstant(msg.time().toInstant(), ZoneId.of("UTC"))) + " UTC] ";
                                String additional = switch (msg.type()) {
                                    case NORMAL -> "";
                                    case STAFF -> Translation.string(l, "moderation.chat_history.staff") + " ";
                                    case PRIVATE -> Translation.string(l, "moderation.chat_history.private", msg.receiver()) + " ";
                                    case PARTY -> Translation.string(l, "moderation.chat_history.party") + " ";
                                };
                                player.sendMessage(Component.text("| " + time + additional, NamedTextColor.GRAY).append(Component.text(msg.content().strip(), NamedTextColor.WHITE)));
                            });
                            player.sendMessage(Component.text("=========================").color(NamedTextColor.DARK_GREEN));

                            return 1;
                        })
                )
                .build());
    }
}

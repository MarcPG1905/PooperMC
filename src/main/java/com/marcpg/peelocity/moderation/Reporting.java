package com.marcpg.peelocity.moderation;

import com.marcpg.lang.Translation;
import com.marcpg.peelocity.Configuration;
import com.marcpg.peelocity.Peelocity;
import com.marcpg.peelocity.PlayerCache;
import com.marcpg.text.Formatter;
import com.marcpg.web.discord.Embed;
import com.marcpg.web.discord.Webhook;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.format.NamedTextColor;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class Reporting {
    public static final List<String> REASONS = List.of("cheats", "spam", "swearing", "exploiting", "other");

    public static BrigadierCommand command() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("report")
                .requires(source -> source instanceof Player)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            PlayerCache.PLAYERS.values().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("reason", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    REASONS.forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("info", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            Player player = (Player) context.getSource();
                                            Locale l = player.getEffectiveLocale();
                                            String targetArg = context.getArgument("player", String.class);
                                            UUID targetUuid = PlayerCache.getUuid(targetArg);

                                            if (targetUuid == null) {
                                                player.sendMessage(Translation.component(l, "cmd.player_not_found", targetArg).color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            String reason = context.getArgument("reason", String.class);

                                            if (reason.isBlank() || REASONS.contains(reason)) {
                                                player.sendMessage(Translation.component(l, "report.invalid_reason", reason).color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            if (Configuration.modWebhook != null) {
                                                try {
                                                    Configuration.modWebhook.post(new Embed("New Report!", null, Color.decode("#FF5555"), List.of(
                                                            new Embed.Field("Reported User", targetArg, true),
                                                            new Embed.Field("Who Reported?", player.getUsername(), true),
                                                            new Embed.Field("Reason", Formatter.toPascalCase(reason), true),
                                                            new Embed.Field("Additional Info", Webhook.escapeJson(context.getArgument("info", String.class)).trim(), false)
                                                    )));
                                                    player.sendMessage(Translation.component(l, "report.confirm").color(NamedTextColor.GREEN));
                                                } catch (IOException e) {
                                                    player.sendMessage(Translation.component(l, "report.error").color(NamedTextColor.RED));
                                                    throw new RuntimeException(e);
                                                }
                                            } else {
                                                player.sendMessage(Translation.component(l, "report.no_webhook", NamedTextColor.RED));
                                                Peelocity.LOG.warn("A player tried using `/report`, which isn't available as there's no valid webhook in the config.");
                                            }
                                            return 1;
                                        })
                                )
                        )
                )
                .build()
        );
    }
}

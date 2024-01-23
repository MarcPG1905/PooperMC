package com.marcpg.peelocity.moderation;

import com.marcpg.peelocity.Config;
import com.marcpg.peelocity.Peelocity;
import com.marcpg.text.Formatter;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.hectus.lang.Translation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class Reporting {
    public static final List<String> REASONS = List.of("cheats", "spam", "swearing", "exploiting", "other");
    public static final String EMBED = "{\"content\":null,\"embeds\":[{\"title\":\"New Report!\",\"color\":16733525,\"fields\":[{\"name\":\"Reported User\",\"value\":\"%s\",\"inline\":true},{\"name\":\"Who Reported?\",\"value\":\"%s\",\"inline\":true},{\"name\":\"Reason\",\"value\":\"%s\",\"inline\":true},{\"name\":\"Additional\",\"value\":\"%s\"}]}],\"attachments\":[]}";

    public static @NotNull BrigadierCommand createComplexReportBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("report")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Peelocity.SERVER.getAllPlayers().forEach(player -> builder.suggest(player.getUsername()));
                            return builder.buildFuture();
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("reason", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    REASONS.forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("info", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            Peelocity.SERVER.getPlayer(context.getArgument("player", String.class)).ifPresentOrElse(
                                                    player -> {
                                                        String json = String.format(EMBED, player.getUsername(), ((Player) context.getSource()).getUsername(), Formatter.toPascalCase(context.getArgument("reason", String.class)), context.getArgument("info", String.class));
                                                        try {
                                                            int response = sendPost(Config.REPORT_WEBHOOK, json);
                                                            if (response < 300) {
                                                                Peelocity.LOG.trace("Sent report webhook with response code: " + response);
                                                            } else {
                                                                Peelocity.LOG.warn("Couldn't send report webhook. Got response: " + response);
                                                            }
                                                        } catch (IOException e) {
                                                            throw new RuntimeException(e);
                                                        }
                                                    },
                                                    () -> context.getSource().sendMessage(Translation.component(((Player) context.getSource()).getEffectiveLocale(), "cmd.player_not_found", context.getArgument("player", String.class)).color(NamedTextColor.RED))
                                            );
                                            return 1;
                                        })
                                )
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .executes(context -> {
                            context.getSource().sendMessage(Component.text("""
                                    §l§nHelp:§r §l/report§r
                                    The command /report can be used to report a player, if you notice suspicious activity without any moderators present.
                                    
                                    §l§nArguments:§r
                                    - §lplayer§r: To which audience the announcement should be sent.
                                    - §lreason§r: If the announcement should be displayed more noticeably, to assure that really everyone reads it.
                                      - cheats: Using cheats ("hacks") in order to gain an unfair advantage.
                                      - spam: Spamming in the chat or disturbing conversations.
                                      - swearing: Not being family-friendly in the chat.
                                      - exploiting: Using bugs or glitches in order to gain an unfair advantage.
                                      - other: If none of the above listed reasons apply.
                                    - §linfo§r: Additional info and proof for the report. Not providing info might make your report less effective.
                                    """));
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }

    private static int sendPost(@NotNull URL url, String json) throws IOException { // Badass code (worst code in human-existence)
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
            out.writeBytes(json);
            out.flush();
        }
        return connection.getResponseCode();
    }
}

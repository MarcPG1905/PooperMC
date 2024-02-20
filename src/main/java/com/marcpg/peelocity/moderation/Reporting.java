package com.marcpg.peelocity.moderation;

import com.marcpg.lang.Translation;
import com.marcpg.peelocity.Config;
import com.marcpg.peelocity.Peelocity;
import com.marcpg.text.Formatter;
import com.marcpg.web.discord.Embed;
import com.marcpg.web.discord.Webhook;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.util.List;

public class Reporting {
    public static final List<String> REASONS = List.of("cheats", "spam", "swearing", "exploiting", "other");

    public static @NotNull BrigadierCommand createReportBrigadier() {
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
                                                        try {
                                                            Config.MODERATOR_WEBHOOK.post(new Embed("New Report!", null, Color.decode("#FF5555"), List.of(
                                                                    new Embed.Field("Reported User", player.getUsername(), true),
                                                                    new Embed.Field("Who Reported?", ((Player) context.getSource()).getUsername(), true),
                                                                    new Embed.Field("Reason", Formatter.toPascalCase(context.getArgument("reason", String.class)), true),
                                                                    new Embed.Field("Additional Info", Webhook.escapeJson(context.getArgument("info", String.class)).trim(), false)
                                                            )));
                                                        } catch (IOException e) {
                                                            player.sendMessage(Component.text("There was an issue, your report is very likely invalid!", NamedTextColor.RED));
                                                            throw new RuntimeException(e);
                                                        }
                                                        player.sendMessage(Component.text("Successfully submitted the report!", NamedTextColor.GREEN));
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
                                    -§l player§r: To which audience the announcement should be sent.
                                    -§l reason§r: If the announcement should be displayed more noticeably, to assure that really everyone reads it.
                                      - cheats: Using cheats ("hacks") in order to gain an unfair advantage.
                                      - spam: Spamming in the chat or disturbing conversations.
                                      - swearing: Not being family-friendly in the chat.
                                      - exploiting: Using bugs or glitches in order to gain an unfair advantage.
                                      - other: If none of the above listed reasons apply.
                                    -§l info§r: Additional info and proof for the report. Not providing info might make your report less effective.
                                    """));
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }
}

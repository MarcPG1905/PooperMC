package com.marcpg.peelocity.moderation;

import com.marcpg.libpg.lang.Translation;
import com.marcpg.common.optional.PlayerCache;
import com.marcpg.peelocity.common.VelocityPlayer;
import com.marcpg.common.entity.OfflinePlayer;
import com.marcpg.common.moderation.Reporting;
import com.marcpg.common.util.InvalidCommandArgsException;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

public final class VelocityReporting {
    public static @NotNull BrigadierCommand command() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("report")
                .requires(source -> source instanceof Player)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            String sourceName = ((Player) context.getSource()).getUsername();
                            PlayerCache.PLAYERS.values().stream()
                                    .filter(p -> !p.equals(sourceName))
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("reason", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    Reporting.REASONS.forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("info", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            Player player = (Player) context.getSource();
                                            String targetArg = context.getArgument("player", String.class);

                                            if (PlayerCache.PLAYERS.containsValue(targetArg)) {
                                                context.getSource().sendMessage(Translation.component(player.getEffectiveLocale(), "cmd.player_not_found", targetArg).color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            try {
                                                Reporting.report(
                                                        new VelocityPlayer((Player) context.getSource()),
                                                        new OfflinePlayer(targetArg, PlayerCache.getUuid(targetArg)),
                                                        context.getArgument("reason", String.class),
                                                        context.getArgument("info", String.class)
                                                );
                                                player.sendMessage(Translation.component(player.getEffectiveLocale(), "report.confirm").color(NamedTextColor.YELLOW));
                                            } catch (InvalidCommandArgsException e) {
                                                player.sendMessage(e.translatable(player.getEffectiveLocale()));
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

package com.marcpg.peelocity.moderation;

import com.marcpg.lang.Translation;
import com.marcpg.peelocity.Configuration;
import com.marcpg.peelocity.Peelocity;
import com.marcpg.web.discord.Embed;
import com.marcpg.web.discord.Webhook;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class Kicking {
    @Contract(" -> new")
    public static @NotNull BrigadierCommand command() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("kick")
                .requires(source -> source.hasPermission("pee.kick"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Peelocity.SERVER.getAllPlayers().parallelStream()
                                    .filter(player -> player != context.getSource())
                                    .map(Player::getUsername)
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("reason", StringArgumentType.greedyString())
                                .executes(context -> {
                                    CommandSource source = context.getSource();
                                    Locale l = source instanceof Player player ? player.getEffectiveLocale() : new Locale("en", "US");
                                    String targetArg = context.getArgument("player", String.class);

                                    Peelocity.SERVER.getPlayer(targetArg).ifPresentOrElse(
                                            t -> {
                                                String reason = context.getArgument("reason", String.class);

                                                Locale tl = t.getEffectiveLocale();
                                                t.disconnect(Translation.component(tl, "moderation.kick.msg.title").color(NamedTextColor.GOLD)
                                                        .appendNewline().appendNewline()
                                                        .append(Translation.component(tl, "moderation.reason", "").color(NamedTextColor.GRAY).append(Component.text(reason, NamedTextColor.BLUE))));

                                                source.sendMessage(Translation.component(l, "moderation.kick.confirm", targetArg, reason).color(NamedTextColor.YELLOW));

                                                if (source instanceof Player player)
                                                    Peelocity.LOG.info(player.getUsername() + " kicked " + targetArg + " with the reason: \"" + reason + "\"");

                                                try {
                                                    if (Configuration.modWebhook != null)
                                                        Configuration.modWebhook.post(new Embed("Minecraft Kick", targetArg + " got kicked by " + (source instanceof Player player ? player.getUsername() : "Console"), Color.GREEN, List.of(
                                                                new Embed.Field("Kicked", targetArg, true),
                                                                new Embed.Field("Moderator", source instanceof Player player ? player.getUsername() : "Console", true),
                                                                new Embed.Field("Reason", Webhook.escapeJson(reason).trim(), false)
                                                        )));
                                                } catch (IOException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            },
                                            () -> source.sendMessage(Translation.component(l, "cmd.player_not_found", targetArg).color(NamedTextColor.RED))
                                    );
                                    return 1;
                                })
                        )
                )
                .build()
        );
    }
}

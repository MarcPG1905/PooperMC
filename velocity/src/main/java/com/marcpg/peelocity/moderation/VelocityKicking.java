package com.marcpg.peelocity.moderation;

import com.marcpg.libpg.lang.Translation;
import com.marcpg.peelocity.Peelocity;
import com.marcpg.peelocity.common.VelocityPlayer;
import com.marcpg.common.moderation.Kicking;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class VelocityKicking {
    @Contract(" -> new")
    public static @NotNull BrigadierCommand command() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("kick")
                .requires(source -> source.hasPermission("poo.kick"))
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
                                    Locale l = source instanceof Player player ? player.getEffectiveLocale() : Locale.getDefault();
                                    String targetArg = context.getArgument("player", String.class);

                                    Peelocity.SERVER.getPlayer(targetArg).ifPresentOrElse(
                                            t -> {
                                                String reason = context.getArgument("reason", String.class);
                                                Kicking.kick(source instanceof Player player ? player.getUsername() : "Console", new VelocityPlayer(t), reason);
                                                source.sendMessage(Translation.component(l, "moderation.kick.confirm", targetArg, reason).color(NamedTextColor.YELLOW));
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

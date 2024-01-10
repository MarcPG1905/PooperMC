package com.marcpg.peelocity.moderation;

import com.marcpg.data.time.Time;
import com.marcpg.peelocity.Peelocity;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.hectus.PostgreConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.postgresql.util.PGTimestamp;

import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static com.marcpg.peelocity.Peelocity.CONFIG;

public class Mutes {
    public record Mute(Date expiration, Time duration, String reason) {}

    public static final Map<UUID, Mute> MUTES = new HashMap<>();
    public static final List<String> TIME_TYPES = List.of("min", "h", "d", "wk", "mo");
    public static final PostgreConnection DATABASE;
    static {
        try {
            DATABASE = new PostgreConnection(CONFIG.getProperty("db-url"), CONFIG.getProperty("db-user"), CONFIG.getProperty("db-passwd"), "mutes");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static @NotNull BrigadierCommand createMuteBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("mute")
                .requires(source -> source.hasPermission("pee.mute"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Peelocity.SERVER.getAllPlayers().stream()
                                    .filter(player -> !player.hasPermission("pee.mute") && player != context.getSource())
                                    .map(Player::getUsername)
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("time", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    TIME_TYPES.forEach(string -> builder.suggest(builder.getInput().replaceAll("[^-\\d.]+", "") + string));
                                    return builder.buildFuture();
                                })
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("reason", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            CommandSource source = context.getSource();
                                            Peelocity.SERVER.getPlayer(context.getArgument("player", String.class)).ifPresentOrElse(
                                                    target -> {
                                                        if (target.hasPermission("pee.mute") && !source.hasPermission("pee.op")) {
                                                            source.sendMessage(Component.text("You can't mute that player!", NamedTextColor.RED));
                                                            return;
                                                        }

                                                        Time time = Time.parse(context.getArgument("time", String.class));
                                                        if (time.get() <= 0) {
                                                            source.sendMessage(Component.text("The time " + time.getPreciselyFormatted() + " is not valid!", NamedTextColor.RED));
                                                            return;
                                                        }

                                                        String reason = context.getArgument("reason", String.class);

                                                        target.sendMessage(Component.text("You are now muted on this server!", NamedTextColor.RED)
                                                                .appendNewline()
                                                                .append(Component.text("Time: ", NamedTextColor.GRAY).append(Component.text(time.getOneUnitFormatted(), NamedTextColor.BLUE))
                                                                .appendNewline()
                                                                .append(Component.text("Reason: ", NamedTextColor.GRAY).append(Component.text(reason, NamedTextColor.BLUE)))));


                                                        try {
                                                            if (!DATABASE.contains(target.getUniqueId())) {
                                                                DATABASE.add(target.getUniqueId(), PGTimestamp.from(Instant.ofEpochSecond(Instant.now().getEpochSecond() + time.get())).toString(), String.valueOf(time.get()), reason);
                                                                source.sendMessage(Component.text("Successfully muted " + target.getUsername() + "for " + time.getPreciselyFormatted() + " with the reason: \"" + reason + "\"", NamedTextColor.YELLOW));
                                                           } else {
                                                                source.sendMessage(Component.text("The player " + context.getArgument("player", String.class) + " is already muted!", NamedTextColor.RED));
                                                            }
                                                        } catch (SQLException e) {
                                                            throw new RuntimeException(e);
                                                        }
                                                    },
                                                    () -> source.sendMessage(Component.text("The player " + context.getArgument("player", String.class) + " could not be found!"))
                                            );
                                            return 1;
                                        })
                                )
                        )
                )
                .build();

        return new BrigadierCommand(node);
    }

    public static @NotNull BrigadierCommand createUnmuteBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("mute")
                .requires(source -> source.hasPermission("pee.mute"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Peelocity.SERVER.getAllPlayers().stream()
                                    .filter(player -> !player.hasPermission("pee.mute") && player != context.getSource())
                                    .map(Player::getUsername)
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            Peelocity.SERVER.getPlayer(context.getArgument("player", String.class)).ifPresentOrElse(
                                    target -> {
                                        if (target.hasPermission("pee.mute") && !source.hasPermission("pee.op")) {
                                            source.sendMessage(Component.text("You can't unmute that player!", NamedTextColor.RED));
                                            return;
                                        }

                                        target.sendMessage(Component.text("You are now unmuted on this server!", NamedTextColor.GREEN));

                                        try {
                                            if (DATABASE.contains(target.getUniqueId())) {
                                                DATABASE.remove(target.getUniqueId());
                                                source.sendMessage(Component.text("Successfully unmuted " + target.getUsername(), NamedTextColor.YELLOW));
                                            } else {
                                                source.sendMessage(Component.text("The player " + context.getArgument("player", String.class) + " is not muted!", NamedTextColor.RED));
                                            }
                                        } catch (SQLException e) {
                                            throw new RuntimeException(e);
                                        }
                                    },
                                    () -> source.sendMessage(Component.text("The player " + context.getArgument("player", String.class) + " could not be found!"))
                            );
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChat(@NotNull PlayerChatEvent event) {
        Player player = event.getPlayer();
        if (MUTES.containsKey(player.getUniqueId())) {
            Mute mute = MUTES.get(player.getUniqueId());
            if (new Date().after(mute.expiration())) {
                MUTES.remove(player.getUniqueId());
            } else {
                event.setResult(PlayerChatEvent.ChatResult.denied());
                player.sendMessage(Component.text("You are currently muted, so you can't chat!", NamedTextColor.RED));
                player.sendMessage(Component.text("Your mute will expire in: " + new Time(Instant.now().getEpochSecond() - mute.expiration.toInstant().getEpochSecond()).getOneUnitFormatted(), NamedTextColor.GOLD));
            }
        }
    }
}

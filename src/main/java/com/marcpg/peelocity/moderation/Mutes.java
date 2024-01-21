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
import net.hectus.PostgreConnection;
import net.hectus.Translation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.postgresql.util.PGTimestamp;

import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static com.marcpg.peelocity.Peelocity.CONFIG;

public class Mutes {
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
                            Peelocity.SERVER.getAllPlayers().parallelStream()
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
                                            Player source = (Player) context.getSource();
                                            Peelocity.SERVER.getPlayer(context.getArgument("player", String.class)).ifPresentOrElse(
                                                    target -> {
                                                        if (target.hasPermission("pee.mute") && !source.hasPermission("pee.op")) {
                                                            source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.mute.cant").color(NamedTextColor.RED));
                                                            return;
                                                        }

                                                        Time time = Time.parse(context.getArgument("time", String.class));
                                                        if (time.get() <= 0) {
                                                            source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.time.invalid", time.getPreciselyFormatted()));
                                                            return;
                                                        }

                                                        String reason = context.getArgument("reason", String.class);

                                                        Locale tl = target.getEffectiveLocale();
                                                        target.sendMessage(Translation.component(tl, "moderation.mute.msg.title").color(NamedTextColor.RED)
                                                                .appendNewline()
                                                                .append(Translation.component(tl, "moderation.expiration", "").color(NamedTextColor.GRAY).append(Component.text(time.getOneUnitFormatted(), NamedTextColor.BLUE))
                                                                .appendNewline()
                                                                .append(Translation.component(tl, "moderation.reason", "").color(NamedTextColor.GRAY).append(Component.text(reason, NamedTextColor.BLUE)))));


                                                        try {
                                                            if (!DATABASE.contains(target.getUniqueId())) {
                                                                DATABASE.add(target.getUniqueId(), PGTimestamp.from(Instant.ofEpochSecond(Instant.now().getEpochSecond() + time.get())).toString(), String.valueOf(time.get()), reason);
                                                                source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.mute.confirm", target.getUsername(), time.getPreciselyFormatted(), reason).color(NamedTextColor.YELLOW));
                                                           } else {
                                                                source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.mute.already_muted", target.getUsername()).color(NamedTextColor.RED));
                                                            }
                                                        } catch (SQLException e) {
                                                            throw new RuntimeException(e);
                                                        }
                                                    },
                                                    () -> source.sendMessage(Translation.component(source.getEffectiveLocale(), "cmd.player_not_found", context.getArgument("player", String.class)).color(NamedTextColor.RED))
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
                            Player source = (Player) context.getSource();
                            Peelocity.SERVER.getPlayer(context.getArgument("player", String.class)).ifPresentOrElse(
                                    target -> {
                                        if (target.hasPermission("pee.mute") && !source.hasPermission("pee.op")) {
                                            source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.unmute.cant").color(NamedTextColor.RED));
                                            return;
                                        }

                                        target.sendMessage(Translation.component(target.getEffectiveLocale(), "moderation.unmute.msg").color(NamedTextColor.GREEN));

                                        try {
                                            if (DATABASE.contains(target.getUniqueId())) {
                                                DATABASE.remove(target.getUniqueId());
                                                source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.unmute.confirm", target.getUsername()).color(NamedTextColor.YELLOW));
                                            } else {
                                                source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.unmute.not_muted", target.getUsername()).color(NamedTextColor.RED));
                                            }
                                        } catch (SQLException e) {
                                            throw new RuntimeException(e);
                                        }
                                    },
                                    () -> source.sendMessage(Translation.component(source.getEffectiveLocale(), "cmd.player_not_found", context.getArgument("player", String.class)).color(NamedTextColor.RED))
                            );
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChat(@NotNull PlayerChatEvent event) throws SQLException {
        Player player = event.getPlayer();
        Locale l = player.getEffectiveLocale();
        if (DATABASE.contains(player.getUniqueId())) {
            Object[] row = DATABASE.getRowArray(player.getUniqueId());

            Instant expiration = ((PGTimestamp) row[1]).toInstant().plusSeconds(Long.parseLong((String) row[2]));
            if (expiration.isBefore(Instant.now())) {
                DATABASE.remove(player.getUniqueId());
                player.sendMessage(Translation.component(l, "moderation.mute.expired.msg").color(NamedTextColor.RED));
            } else {
                event.setResult(PlayerChatEvent.ChatResult.denied());
                player.sendMessage(Translation.component(l, "moderation.mute.warning").color(NamedTextColor.RED));
                player.sendMessage(Translation.component(l, "moderation.expiration", new Time(Instant.now().getEpochSecond() - expiration.getEpochSecond()).getPreciselyFormatted()).color(NamedTextColor.GOLD));
                player.sendMessage(Translation.component(l, "moderation.reason", row[3]).color(NamedTextColor.GOLD));
            }
        }
    }
}

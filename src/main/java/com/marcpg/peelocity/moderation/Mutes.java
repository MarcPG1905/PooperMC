package com.marcpg.peelocity.moderation;

import com.marcpg.data.time.Time;
import com.marcpg.discord.Embed;
import com.marcpg.discord.Webhook;
import com.marcpg.peelocity.Config;
import com.marcpg.peelocity.Peelocity;
import com.marcpg.peelocity.PlayerCache;
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
import net.hectus.lang.Translation;
import net.hectus.sql.AutoCatchingPostgreConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.postgresql.util.PGTimestamp;

import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

public class Mutes {
    public static final List<String> TIME_TYPES = List.of("min", "h", "d", "wk", "mo");
    public static final AutoCatchingPostgreConnection DATABASE;
    static {
        try {
            DATABASE = new AutoCatchingPostgreConnection(Config.DATABASE_URL, Config.DATABASE_USER, Config.DATABASE_PASSWD, "mutes", e -> Peelocity.LOG.warn("Error while interacting with the mute database: " + e.getMessage()));
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
                                    String input = context.getArguments().size() == 2 ? List.of(builder.getInput().split(" ")).getLast() : "";
                                    TIME_TYPES.forEach(string -> builder.suggest(input.replaceAll("[^-\\d.]+", "") + string));
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

                                                        if (!DATABASE.contains(target.getUniqueId())) {
                                                            DATABASE.add(target.getUniqueId(), PGTimestamp.from(Instant.ofEpochSecond(Instant.now().getEpochSecond() + time.get())), time.get(), reason);
                                                            source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.mute.confirm", target.getUsername(), time.getPreciselyFormatted(), reason).color(NamedTextColor.YELLOW));
                                                            Peelocity.LOG.info(source.getUsername() + " muted " + target.getUsername() + " for " + time.getPreciselyFormatted() + " with the reason: \"" + reason + "\"");
                                                            try {
                                                                Config.MOD_ONLY_WEBHOOK.post(new Embed("Minecraft Mute", target.getUsername() + " got muted by " + source.getUsername(), Color.YELLOW, List.of(
                                                                        new Embed.Field("Muted", target.getUsername(), true),
                                                                        new Embed.Field("Moderator", source.getUsername(), true),
                                                                        new Embed.Field("Time", time.getPreciselyFormatted(), true),
                                                                        new Embed.Field("Reason", Webhook.escapeJson(reason).trim(), false)
                                                                )));
                                                            } catch (IOException e) {
                                                                throw new RuntimeException(e);
                                                            }
                                                        } else {
                                                            source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.mute.already_muted", target.getUsername()).color(NamedTextColor.RED));
                                                        }
                                                    },
                                                    () -> source.sendMessage(Translation.component(source.getEffectiveLocale(), "cmd.player_not_found", context.getArgument("player", String.class)).color(NamedTextColor.RED))
                                            );
                                            return 1;
                                        })
                                )
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .executes(context -> {
                            context.getSource().sendMessage(Component.text("""
                                    §l§nHelp:§r §l/mute§r
                                    The command /mute acts as a utility to prevent players from using the chat for a specified while.
                                    
                                    §l§nArguments:§r
                                    - §lplayer§r: The player to mute on the server.
                                    - §ltime§r: How long the player should remain muted. One time unit only.
                                    - §lreason§r: A good reason for muting the player.
                                    
                                    §l§nAdditional Info:§r
                                    - You can currently only mute players who are online.
                                    """));
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }

    public static @NotNull BrigadierCommand createUnmuteBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("unmute")
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
                            String target = context.getArgument("player", String.class);
                            if (DATABASE.contains(PlayerCache.getUuid(target))) {
                                DATABASE.remove(PlayerCache.getUuid(target));
                                source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.unmute.confirm", target).color(NamedTextColor.YELLOW));
                                Peelocity.LOG.info(source.getUsername() + " unmuted " + target);
                            } else {
                                source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.unmute.not_muted", target).color(NamedTextColor.RED));
                            }
                            return 1;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .executes(context -> {
                            context.getSource().sendMessage(Component.text("""
                                    §l§nHelp:§r §l/unmute§r
                                    The command /unmute acts as a utility to remove a player's mute.
                                    
                                    §l§nArguments:§r
                                    - §lplayer§r: The player to unmute on the server.
                                    """));
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChat(@NotNull PlayerChatEvent event) {
        Player player = event.getPlayer();
        Locale l = player.getEffectiveLocale();
        if (DATABASE.contains(player.getUniqueId())) {
            Object[] row = DATABASE.getRowArray(player.getUniqueId());

            Instant expiration = ((Timestamp) row[1]).toInstant();

            if (expiration.isBefore(Instant.now())) {
                DATABASE.remove(player.getUniqueId());
                player.sendMessage(Translation.component(l, "moderation.mute.expired.msg").color(NamedTextColor.RED));
            } else {
                event.setResult(PlayerChatEvent.ChatResult.denied());
                player.sendMessage(Translation.component(l, "moderation.mute.warning").color(NamedTextColor.RED));
                player.sendMessage(Translation.component(l, "moderation.expiration", new Time(expiration.getEpochSecond() - Instant.now().getEpochSecond()).getPreciselyFormatted()).color(NamedTextColor.GOLD));
                player.sendMessage(Translation.component(l, "moderation.reason", row[3]).color(NamedTextColor.GOLD));
            }
        }
    }
}

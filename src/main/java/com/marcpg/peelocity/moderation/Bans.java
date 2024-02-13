package com.marcpg.peelocity.moderation;

import com.marcpg.data.database.sql.AutoCatchingSQLConnection;
import com.marcpg.data.time.Time;
import com.marcpg.lang.Translation;
import com.marcpg.peelocity.Config;
import com.marcpg.peelocity.Peelocity;
import com.marcpg.peelocity.PlayerCache;
import com.marcpg.web.discord.Embed;
import com.marcpg.web.discord.Webhook;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
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
import java.util.Map;

public class Bans {
    public static final List<String> TIME_TYPES = List.of("min", "h", "d", "wk", "mo", "yr");
    public static final AutoCatchingSQLConnection DATABASE;
    static {
        try {
            DATABASE = new AutoCatchingSQLConnection(Config.DATABASE_TYPE, Config.DATABASE_ADDRESS, Config.DATABASE_PORT, Config.DATABASE_NAME, Config.DATABASE_USER, Config.DATABASE_PASSWD, "bans", e -> Peelocity.LOG.warn("Error while interacting with the ban database: " + e.getMessage()));
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static @NotNull BrigadierCommand createBanBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("ban")
                .requires(source -> source.hasPermission("pee.ban"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Peelocity.SERVER.getAllPlayers().stream()
                                    .filter(player -> !player.hasPermission("pee.ban") && player != context.getSource())
                                    .map(Player::getUsername)
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("time", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    String input = context.getArguments().size() == 2 ? List.of(builder.getInput().split(" ")).getLast() : "";
                                    TIME_TYPES.forEach(string -> builder.suggest(input.replaceAll("[^-\\d.]+", "") + string));
                                    builder.suggest("permanent");
                                    return builder.buildFuture();
                                })
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("reason", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            Player source = (Player) context.getSource();
                                            Peelocity.SERVER.getPlayer(context.getArgument("player", String.class)).ifPresentOrElse(
                                                    target -> {
                                                        if (target.hasPermission("pee.ban") && !source.hasPermission("pee.op")) {
                                                            source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.ban.cant").color(NamedTextColor.RED));
                                                            return;
                                                        }

                                                        boolean permanent = context.getArgument("time", String.class).contains("permanent");
                                                        Time time = permanent ? new Time(0) : Time.parse(context.getArgument("time", String.class));
                                                        if (time.get() < 0) {
                                                            source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.time.invalid", time.getPreciselyFormatted()));
                                                            return;
                                                        }

                                                        String reason = context.getArgument("reason", String.class);

                                                        Locale tl = target.getEffectiveLocale();
                                                        target.disconnect(Translation.component(tl, "moderation.ban.msg.title").color(NamedTextColor.RED)
                                                                .appendNewline().appendNewline()
                                                                .append(Translation.component(tl, "moderation.expiration", "").color(NamedTextColor.GRAY).append(permanent ? Translation.component(tl, "moderation.time.permanent").color(NamedTextColor.RED) : Component.text(time.getOneUnitFormatted(), NamedTextColor.BLUE)))
                                                                .appendNewline()
                                                                .append(Translation.component(tl, "moderation.reason", "").color(NamedTextColor.GRAY).append(Component.text(reason, NamedTextColor.BLUE))));

                                                        if (!DATABASE.contains(target.getUniqueId())) {
                                                            DATABASE.add(target.getUniqueId(), PGTimestamp.from(Instant.now().plusSeconds(permanent ? 10000000000L : time.get())), time.get(), reason); // 10000000000 = ~317 years
                                                            source.sendMessage(Translation.component(tl, "moderation.ban.confirm", target.getUsername(), permanent ? Translation.string(tl, "moderation.time.permanent") : time.getPreciselyFormatted(), reason).color(NamedTextColor.YELLOW));
                                                            Peelocity.LOG.info(source.getUsername() + " banned " + target.getUsername() + " for " + time.getPreciselyFormatted() + " with the reason: \"" + reason + "\"");
                                                            try {
                                                                Config.MODERATOR_WEBHOOK.post(new Embed("Minecraft Ban", target.getUsername() + " got banned by " + source.getUsername(), Color.ORANGE, List.of(
                                                                        new Embed.Field("Banned", target.getUsername(), true),
                                                                        new Embed.Field("Moderator", source.getUsername(), true),
                                                                        new Embed.Field("Time", permanent ? "Permanent" : time.getPreciselyFormatted(), true),
                                                                        new Embed.Field("Reason", Webhook.escapeJson(reason).trim(), false)
                                                                )));
                                                            } catch (IOException e) {
                                                                throw new RuntimeException(e);
                                                            }
                                                        } else {
                                                            source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.ban.already_banned", context.getArgument("player", String.class)).color(NamedTextColor.RED));
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
                                    §l§nHelp:§r §l/ban§r
                                    The command /ban acts as a utility to ban players from the whole server with additional features, such as expirations.
                                    
                                    §l§nArguments:§r
                                    - §lplayer§r: The player to ban off the server.
                                    - §ltime§r: How long the player should remain banned. One time unit only. A value of 'permanent' will act as the time set to 0, which will never expire.
                                    - §lreason§r: A good reason for banning the player.
                                    
                                    §l§nAdditional Info:§r
                                    - You can currently only ban players who are online.
                                    """));
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }

    public static @NotNull BrigadierCommand createPardonBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("pardon")
                .requires(source -> source.hasPermission("pee.ban"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            PlayerCache.CACHED_USERS.entrySet().stream()
                                    .filter(entry -> DATABASE.contains(entry.getKey()))
                                    .map(Map.Entry::getValue)
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            Player source = (Player) context.getSource();
                            String target = context.getArgument("player", String.class);
                            if (DATABASE.contains(PlayerCache.getUuid(target))) {
                                DATABASE.remove(PlayerCache.getUuid(target));
                                source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.pardon.confirm", target).color(NamedTextColor.YELLOW));
                                Peelocity.LOG.info(source.getUsername() + " pardoned/unbanned " + target);
                            } else {
                                source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.pardon.not_banned", target).color(NamedTextColor.RED));
                            }
                            return 1;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .executes(context -> {
                            context.getSource().sendMessage(Component.text("""
                                    §l§nHelp:§r §l/pardon§r
                                    The command /pardon acts as a utility to pardon/unban players.
                                    
                                    §l§nArguments:§r
                                    - §lplayer§r: The player to pardon from the server.
                                    """));
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onLogin(@NotNull LoginEvent event) {
        Player player = event.getPlayer();
        if (DATABASE.contains(player.getUniqueId())) {
            Object[] row = DATABASE.getRowArray(player.getUniqueId());

            Time duration = new Time((Long) row[2]);
            Instant expiration = ((Timestamp) row[1]).toInstant();

            if (expiration.isBefore(Instant.now())) {
                DATABASE.remove(player.getUniqueId());
                player.sendMessage(Translation.component(player.getEffectiveLocale(), "moderation.ban.expired.msg").color(NamedTextColor.GREEN));
            } else {
                Locale l = event.getPlayer().getEffectiveLocale();
                event.setResult(ResultedEvent.ComponentResult.denied(Translation.component(l, "moderation.ban.join.title").color(NamedTextColor.RED)
                        .appendNewline().appendNewline()
                        .append(Translation.component(l, "moderation.expiration", "").color(NamedTextColor.GRAY).append(duration.get() == 0 ? Translation.component(l, "moderation.time.permanent").color(NamedTextColor.RED) : Component.text(new Time(expiration.getEpochSecond() - Instant.now().getEpochSecond()).getPreciselyFormatted(), NamedTextColor.BLUE)))
                        .appendNewline()
                        .append(Translation.component(l, "moderation.reason", "").color(NamedTextColor.GRAY).append(Component.text(row[3].toString(), NamedTextColor.BLUE)))));
            }
        }
    }
}

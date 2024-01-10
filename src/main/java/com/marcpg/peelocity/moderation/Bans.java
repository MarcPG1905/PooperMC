package com.marcpg.peelocity.moderation;

import com.marcpg.data.time.Time;
import com.marcpg.peelocity.Peelocity;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
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

public class Bans {
    public record Ban(Date expiration, Time duration, String reason) {}

    public static final Map<UUID, Ban> BANS = new HashMap<>();
    public static final List<String> TIME_TYPES = List.of("min", "h", "d", "wk", "mo", "yr", "permanent");
    public static final PostgreConnection DATABASE;
    static {
        try {
            DATABASE = new PostgreConnection(CONFIG.getProperty("db-url"), CONFIG.getProperty("db-user"), CONFIG.getProperty("db-passwd"), "bans");
        } catch (SQLException e) {
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
                                    TIME_TYPES.forEach(string -> builder.suggest(builder.getInput().replaceAll("[^-\\d.]+", "") + string));
                                    return builder.buildFuture();
                                })
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("reason", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            CommandSource source = context.getSource();
                                            Peelocity.SERVER.getPlayer(context.getArgument("player", String.class)).ifPresentOrElse(
                                                    target -> {
                                                        if (target.hasPermission("pee.ban") && !source.hasPermission("pee.op")) {
                                                            source.sendMessage(Component.text("You can't ban that player!", NamedTextColor.RED));
                                                            return;
                                                        }

                                                        boolean permanent = context.getArgument("time", String.class).contains("permanent");
                                                        Time time = permanent ? new Time(1, Time.Unit.CENTURIES) : Time.parse(context.getArgument("time", String.class));
                                                        if (time.get() <= 0) {
                                                            source.sendMessage(Component.text("The time " + time.getPreciselyFormatted() + " is not valid!", NamedTextColor.RED));
                                                            return;
                                                        }

                                                        String reason = context.getArgument("reason", String.class);

                                                        target.disconnect(Component.text("You got banned from from server!", NamedTextColor.RED)
                                                                .appendNewline()
                                                                .append(Component.text("Time: ", NamedTextColor.GRAY).append(permanent ? Component.text("Permanent", NamedTextColor.RED) : Component.text(time.getOneUnitFormatted(), NamedTextColor.BLUE)))
                                                                .appendNewline()
                                                                .append(Component.text("Reason: ", NamedTextColor.GRAY).append(Component.text(reason, NamedTextColor.BLUE))));

                                                        try {
                                                            if (!DATABASE.contains(target.getUniqueId())) {
                                                                DATABASE.add(target.getUniqueId(), PGTimestamp.from(Instant.ofEpochSecond(Instant.now().getEpochSecond() + time.get())).toString(), String.valueOf(time.get()), reason);
                                                                source.sendMessage(Component.text("Successfully banned " + target.getUsername() + " " + (permanent ? "permanently" : "for " + time.getPreciselyFormatted()) + " with the reason: \"" + reason + "\"", NamedTextColor.YELLOW));
                                                            } else {
                                                                source.sendMessage(Component.text("The player " + context.getArgument("player", String.class) + " is already banned!", NamedTextColor.RED));
                                                            }
                                                        } catch (SQLException e) {
                                                            throw new RuntimeException(e);
                                                        }
                                                    },
                                                    () -> source.sendMessage(Component.text("The player " + context.getArgument("player", String.class) + " could not be found!", NamedTextColor.RED))
                                            );
                                            return 1;
                                        })
                                )
                        )
                )
                .build();

        return new BrigadierCommand(node);
    }

    public static @NotNull BrigadierCommand createPardonBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("pardon")
                .requires(source -> source.hasPermission("pee.ban"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Peelocity.SERVER.getAllPlayers().stream()
                                    .filter(player -> !player.hasPermission("pee.ban") && player != context.getSource())
                                    .map(Player::getUsername)
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            Peelocity.SERVER.getPlayer(context.getArgument("player", String.class)).ifPresentOrElse(
                                    target -> {
                                        if (target.hasPermission("pee.ban") && !source.hasPermission("pee.op")) {
                                            source.sendMessage(Component.text("You can't ban that player!", NamedTextColor.RED));
                                            return;
                                        }

                                        try {
                                            if (DATABASE.contains(target.getUniqueId())) {
                                                DATABASE.remove(target.getUniqueId());
                                                source.sendMessage(Component.text("Successfully pardoned/unbanned " + target.getUsername(), NamedTextColor.YELLOW));
                                            } else {
                                                source.sendMessage(Component.text("The player " + context.getArgument("player", String.class) + " is not banned!", NamedTextColor.RED));
                                            }
                                        } catch (SQLException e) {
                                            throw new RuntimeException(e);
                                        }
                                    },
                                    () -> source.sendMessage(Component.text("The player " + context.getArgument("player", String.class) + " could not be found!", NamedTextColor.RED))
                            );
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }
}

package com.marcpg.peelocity.social;

import com.marcpg.peelocity.Peelocity;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.hectus.PostgreConnection;
import net.hectus.Translation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;

import static com.marcpg.peelocity.Peelocity.CONFIG;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class FriendSystem {
    public static final Map<UUID, UUID> FRIEND_REQUESTS = new HashMap<>();
    public static final PostgreConnection DATABASE;
    static {
        try {
            DATABASE = new PostgreConnection(CONFIG.getProperty("db-url"), CONFIG.getProperty("db-user"), CONFIG.getProperty("db-passwd"), "friendships");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static @NotNull BrigadierCommand createFriendBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("friend")
                .requires(source -> source.hasPermission("pee.friends"))
                .then(LiteralArgumentBuilder.<CommandSource>literal("add")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    Peelocity.SERVER.getAllPlayers().stream()
                                            .filter(player -> player != context.getSource())
                                            .map(Player::getUsername)
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Player player = (Player) context.getSource();
                                    Optional<Player> optionalTarget = Peelocity.SERVER.getPlayer(context.getArgument("player", String.class));
                                    if (optionalTarget.isPresent()) {
                                        Player target = optionalTarget.get();
                                        if (getFriendship(player, target).getKey()) {
                                            player.sendMessage(Translation.component(player.getEffectiveLocale(), "friend.already_friends", target.getUsername()).color(YELLOW));
                                        } else {
                                            if ((FRIEND_REQUESTS.containsKey(player.getUniqueId()) && FRIEND_REQUESTS.containsValue(target.getUniqueId())) ||
                                                    (FRIEND_REQUESTS.containsKey(target.getUniqueId()) && FRIEND_REQUESTS.containsValue(player.getUniqueId()))) {
                                                player.sendMessage(Translation.component(player.getEffectiveLocale(), "friend.add.already_requested", target.getUsername()).color(YELLOW));
                                            } else {
                                                FRIEND_REQUESTS.put(player.getUniqueId(), target.getUniqueId());
                                                player.sendMessage(Translation.component(player.getEffectiveLocale(), "friend.add.confirm", target.getUsername()).color(GREEN));
                                                target.sendMessage(Translation.component(target.getEffectiveLocale(), "friend.add.msg.1", player.getUsername()).color(GREEN)
                                                        .append(Translation.component(target.getEffectiveLocale(), "friend.add.msg.2").color(YELLOW)
                                                                .hoverEvent(HoverEvent.showText(Translation.component(target.getEffectiveLocale(), "friend.add.msg.2.tooltip")))
                                                                .clickEvent(ClickEvent.runCommand("/friend accept " + player.getUsername())))
                                                        .append(Translation.component(target.getEffectiveLocale(), "friend.add.msg.3")));
                                            }
                                        }
                                    } else {
                                        player.sendMessage(Translation.component(player.getEffectiveLocale(), "cmd.player_not_found", context.getArgument("player", String.class)).color(NamedTextColor.RED));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("accept")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    Peelocity.SERVER.getAllPlayers().stream()
                                            .filter(player -> player != context.getSource())
                                            .map(Player::getUsername)
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Player player = (Player) context.getSource();
                                    Optional<Player> target = Peelocity.SERVER.getPlayer(context.getArgument("player", String.class));
                                    if (target.isPresent()) {
                                        Map.Entry<Boolean, ResultSet> result = getFriendship(player, target.get());
                                        if (result.getKey()) {
                                            player.sendMessage(Translation.component(player.getEffectiveLocale(), "friend.already_friends", target.get().getUsername()).color(YELLOW));
                                        } else {
                                            if (FRIEND_REQUESTS.containsKey(target.get().getUniqueId())) {
                                                try {
                                                    DATABASE.add(UUID.randomUUID(), target.get().getUniqueId().toString(), player.getUniqueId().toString());
                                                    FRIEND_REQUESTS.remove(target.get().getUniqueId());
                                                } catch (SQLException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            } else {
                                                player.sendMessage(Translation.component(player.getEffectiveLocale(), "friend.accept.not_requested", target.get().getUsername()).color(RED));
                                            }
                                        }
                                    } else {
                                        player.sendMessage(Translation.component(player.getEffectiveLocale(), "cmd.player_not_found", context.getArgument("player", String.class)).color(NamedTextColor.RED));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("remove")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    Peelocity.SERVER.getAllPlayers().stream()
                                            .filter(player -> player != context.getSource())
                                            .map(Player::getUsername)
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Player player = (Player) context.getSource();
                                    Optional<Player> target = Peelocity.SERVER.getPlayer(context.getArgument("player", String.class));
                                    if (target.isPresent()) {
                                        Map.Entry<Boolean, ResultSet> result = getFriendship(player, target.get());
                                        if (result.getKey()) {
                                            try {
                                                DATABASE.remove(result.getValue().getObject("uuid", UUID.class));
                                                player.sendMessage(Translation.component(player.getEffectiveLocale(), "friend.remove.confirm", target.get().getUsername()).color(YELLOW));
                                                target.get().sendMessage(Translation.component(target.get().getEffectiveLocale(), "friend.remove.confirm", player.getUsername()).color(YELLOW));
                                            } catch (SQLException e) {
                                                throw new RuntimeException(e);
                                            }
                                        } else {
                                            player.sendMessage(Translation.component(player.getEffectiveLocale(), "friend.not_friends", target.get().getUsername()).color(RED));
                                        }
                                    } else {
                                        player.sendMessage(Translation.component(player.getEffectiveLocale(), "cmd.player_not_found", context.getArgument("player", String.class)).color(NamedTextColor.RED));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("list")
                        .executes(context -> {
                            if (context.getSource() instanceof Player player) {
                                try {
                                    String sql1 = "SELECT * FROM friendships WHERE player1_uuid = '" + player.getUniqueId() + "';";
                                    ResultSet resultSet1 = DATABASE.statement().executeQuery(sql1);
                                    while (resultSet1.next()) {
                                        player.sendMessage(Component.text("- " + Peelocity.DATABASE.get(resultSet1.getObject("player2_uuid", UUID.class), "player_name"), AQUA));
                                    }

                                    String sql2 = "SELECT * FROM friendships WHERE player2_uuid = '" + player.getUniqueId() + "';";
                                    ResultSet resultSet2 = DATABASE.statement().executeQuery(sql2);
                                    while (resultSet2.next()) {
                                        player.sendMessage(Component.text("- " + Peelocity.DATABASE.get(resultSet1.getObject("player2_uuid", UUID.class), "player_name"), AQUA));
                                    }
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }

    private static @NotNull Map.Entry<Boolean, ResultSet> getFriendship(@NotNull Player player1, @NotNull Player player2) {
        try {
            String sql = MessageFormat.format("SELECT 1 FROM friendships WHERE (player1_uuid = ''{0}'' AND player2_uuid = ''{1}'') OR (player1_uuid = ''{0}'' AND player2_uuid = ''{1}'')", player1.getUniqueId(), player2.getUniqueId());
            ResultSet resultSet = DATABASE.statement().executeQuery(sql);
            return new HashMap<>(Map.of(resultSet.next(), resultSet)).entrySet().stream().findFirst().orElseThrow();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

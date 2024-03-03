package com.marcpg.peelocity.social;

import com.marcpg.lang.Translation;
import com.marcpg.peelocity.Configuration;
import com.marcpg.peelocity.Peelocity;
import com.marcpg.peelocity.PlayerCache;
import com.marcpg.peelocity.storage.DatabaseStorage;
import com.marcpg.peelocity.storage.RamStorage;
import com.marcpg.peelocity.storage.Storage;
import com.marcpg.peelocity.storage.YamlStorage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.kyori.adventure.text.format.NamedTextColor.*;

public class FriendSystem {
    private static final HashMap<UUID, HashSet<UUID>> FRIEND_REQUESTS = new HashMap<>();
    private static Storage<UUID> STORAGE;

    public static @NotNull BrigadierCommand command() {
        if (STORAGE == null)
            STORAGE = Configuration.storageType.createStorage("friendships", "uuid");

        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("friend")
                .requires(source -> source instanceof Player)
                .then(LiteralArgumentBuilder.<CommandSource>literal("add")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    String sourceName = ((Player) context.getSource()).getUsername();
                                    PlayerCache.PLAYERS.values().stream()
                                            .filter(s -> !s.equals(sourceName))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Player player = (Player) context.getSource();
                                    UUID playerUuid = player.getUniqueId();
                                    Locale l = player.getEffectiveLocale();
                                    String targetArg = context.getArgument("player", String.class);
                                    UUID targetUuid = PlayerCache.getUuid(targetArg);

                                    if (targetUuid == null) {
                                        player.sendMessage(Translation.component(l, "cmd.player_not_found", targetArg).color(RED));
                                        return 1;
                                    }

                                    if (getFriendship(playerUuid, targetUuid) != null) {
                                        player.sendMessage(Translation.component(l, "friend.already_friends", targetArg).color(YELLOW));
                                    } else if ((FRIEND_REQUESTS.containsKey(playerUuid) && FRIEND_REQUESTS.get(playerUuid).contains(targetUuid)) ||
                                            (FRIEND_REQUESTS.containsKey(targetUuid) && FRIEND_REQUESTS.get(targetUuid).contains(playerUuid))) {
                                        player.sendMessage(Translation.component(l, "friend.add.already_requested", targetArg).color(YELLOW));
                                    } else {
                                        if (FRIEND_REQUESTS.containsKey(targetUuid)) {
                                            FRIEND_REQUESTS.get(targetUuid).add(playerUuid);
                                        } else {
                                            FRIEND_REQUESTS.put(targetUuid, new HashSet<>(Set.of(playerUuid)));
                                        }
                                        player.sendMessage(Translation.component(l, "friend.add.confirm", targetArg).color(GREEN));
                                        Peelocity.SERVER.getPlayer(targetUuid).ifPresent(t -> {
                                            Locale tl = t.getEffectiveLocale();
                                            t.sendMessage(Translation.component(tl, "friend.add.msg.1", player.getUsername()).color(GREEN)
                                                    .appendSpace()
                                                    .append(Translation.component(tl, "friend.add.msg.2").color(YELLOW)
                                                            .hoverEvent(HoverEvent.showText(Translation.component(tl, "friend.add.msg.2.tooltip")))
                                                            .clickEvent(ClickEvent.runCommand("/friend accept " + player.getUsername())))
                                                    .appendSpace()
                                                    .append(Translation.component(tl, "friend.add.msg.3").color(GREEN)));
                                        });
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("remove")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    UUID playerUuid = ((Player) context.getSource()).getUniqueId();
                                    getFriendships(((Player) context.getSource()).getUniqueId()).forEach(m -> {
                                        UUID friend = (UUID) (m.get("player1").equals(playerUuid) ? m.get("player2") : m.get("player1"));
                                        builder.suggest(PlayerCache.PLAYERS.get(friend));
                                    });
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Player player = (Player) context.getSource();
                                    String targetArg = context.getArgument("player", String.class);
                                    UUID targetUuid = PlayerCache.getUuid(targetArg);

                                    if (targetUuid == null) {
                                        player.sendMessage(Translation.component(player.getEffectiveLocale(), "cmd.player_not_found", targetArg).color(RED));
                                        return 1;
                                    }

                                    UUID friendship = getFriendship(player.getUniqueId(), targetUuid);
                                    System.out.println(friendship);
                                    if (friendship != null) {
                                        STORAGE.remove(friendship);
                                        player.sendMessage(Translation.component(player.getEffectiveLocale(), "friend.remove.confirm", targetArg).color(YELLOW));
                                        Peelocity.SERVER.getPlayer(targetUuid).ifPresent(t ->
                                                t.sendMessage(Translation.component(t.getEffectiveLocale(), "friend.remove.confirm", player.getUsername()).color(YELLOW)));
                                    } else {
                                        player.sendMessage(Translation.component(player.getEffectiveLocale(), "friend.not_friends", targetArg).color(YELLOW));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("accept")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    UUID sourceUuid = ((Player) context.getSource()).getUniqueId();
                                    if (FRIEND_REQUESTS.containsKey(sourceUuid))
                                        FRIEND_REQUESTS.get(sourceUuid).forEach(uuid -> builder.suggest(uuid.toString()));
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Player player = (Player) context.getSource();
                                    UUID playerUuid = player.getUniqueId();
                                    Locale l = player.getEffectiveLocale();
                                    String targetArg = context.getArgument("player", String.class);
                                    UUID targetUuid = PlayerCache.getUuid(targetArg);

                                    if (targetUuid == null) {
                                        player.sendMessage(Translation.component(l, "cmd.player_not_found", targetArg).color(RED));
                                        return 1;
                                    }

                                    HashSet<UUID> requests = FRIEND_REQUESTS.get(playerUuid);

                                    if (requests != null && requests.contains(targetUuid)) {
                                        if (getFriendship(playerUuid, targetUuid) == null) {
                                            STORAGE.add(Map.of("uuid", UUID.randomUUID(), "player1", playerUuid, "player2", targetUuid));
                                            requests.remove(targetUuid);
                                            if (requests.isEmpty())
                                                FRIEND_REQUESTS.remove(playerUuid);

                                            player.sendMessage(Translation.component(l, "friend.accept.confirm", targetArg).color(GREEN));
                                            Peelocity.SERVER.getPlayer(targetUuid).ifPresent(t ->
                                                    t.sendMessage(Translation.component(t.getEffectiveLocale(), "friend.accept.confirm", player.getUsername()).color(GREEN)));
                                        } else {
                                            player.sendMessage(Translation.component(l, "friend.already_friends", targetArg).color(RED));
                                        }
                                    } else {
                                        player.sendMessage(Translation.component(l, "friend.accept_deny.not_requested", targetArg).color(RED));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("deny")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    UUID sourceUuid = ((Player) context.getSource()).getUniqueId();
                                    if (FRIEND_REQUESTS.containsKey(sourceUuid))
                                        FRIEND_REQUESTS.get(sourceUuid).forEach(uuid -> builder.suggest(uuid.toString()));
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Player player = (Player) context.getSource();
                                    UUID playerUuid = player.getUniqueId();
                                    Locale l = player.getEffectiveLocale();
                                    String targetArg = context.getArgument("player", String.class);
                                    UUID targetUuid = PlayerCache.getUuid(targetArg);

                                    if (targetUuid == null) {
                                        player.sendMessage(Translation.component(l, "cmd.player_not_found", targetArg).color(RED));
                                        return 1;
                                    }

                                    HashSet<UUID> requests = FRIEND_REQUESTS.get(playerUuid);

                                    if (requests != null && requests.contains(targetUuid)) {
                                        if (getFriendship(playerUuid, targetUuid) == null) {
                                            requests.remove(targetUuid);
                                            if (requests.isEmpty()) FRIEND_REQUESTS.remove(playerUuid);

                                            player.sendMessage(Translation.component(l, "friend.deny.confirm", targetArg).color(YELLOW));
                                            Peelocity.SERVER.getPlayer(targetUuid).ifPresent(t ->
                                                    t.sendMessage(Translation.component(t.getEffectiveLocale(), "friend.deny.msg", player.getUsername()).color(GOLD)));
                                        } else {
                                            player.sendMessage(Translation.component(l, "friend.already_friends", targetArg).color(RED));
                                        }
                                    } else {
                                        player.sendMessage(Translation.component(l, "friend.accept_deny.not_requested", targetArg).color(RED));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("list")
                        .executes(context -> {
                            Player player = (Player) context.getSource();
                            UUID playerUuid = player.getUniqueId();

                            List<Map<String, Object>> friendships = getFriendships(playerUuid);

                            if (friendships.isEmpty()) {
                                player.sendMessage(Translation.component(player.getEffectiveLocale(), "friend.list.none").color(YELLOW));
                            } else {
                                player.sendMessage(Translation.component(player.getEffectiveLocale(), "friend.list.list", friendships.size()).color(GREEN));
                                friendships.forEach(m -> {
                                    UUID friend = (UUID) (m.get("player1").equals(playerUuid) ? m.get("player2") : m.get("player1"));
                                    player.sendMessage(Component.text("- " + PlayerCache.PLAYERS.get(friend)));
                                });
                            }
                            return 1;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .executes(context -> {
                            context.getSource().sendMessage(Component.text("""
                                    §l§nHelp:§r §l/friend§r
                                    The command /friend provides all kinds of utilities for managing your friendships.
                                    
                                    §l§nArguments:§r
                                    -§l add§r: Add someone as a friend, if you're not friends yet.
                                    -§l remove§r: Removes a specific player from your friend list, if you're friends.
                                    -§l accept§r: Accept someone's friend request, if you got one.
                                    -§l deny§r: Deny someone's friend request, if you got one.
                                    -§l list§r: Lists of all your current friends.
                                    """));
                            return 1;
                        })
                )
                .build()
        );
    }

    private static List<Map<String, Object>> getFriendships(UUID player) {
        if (STORAGE instanceof DatabaseStorage<UUID> databaseStorage) {
            return List.copyOf(databaseStorage.get("player1 = ? OR player2 = ?", player, player));
        } else if (STORAGE instanceof RamStorage<UUID> ramStorage) {
            return List.copyOf(ramStorage.get(m -> m.get("player1").equals(player) || m.get("player2").equals(player)));
        } else if (STORAGE instanceof YamlStorage<UUID> yamlStorage) {
            return List.copyOf(yamlStorage.get(m -> m.get("player1").equals(player) || m.get("player2").equals(player)));
        } else {
            return List.of();
        }
    }

    private static @Nullable UUID getFriendship(UUID player1, UUID player2) {
        List<Map<String, Object>> maps = List.of();

        if (STORAGE instanceof DatabaseStorage<UUID> databaseStorage) {
            maps = List.copyOf(databaseStorage.get("player1 = ? AND player2 = ? OR player2 = ? AND player1 = ?", player1, player2, player1, player2));
        } else if (STORAGE instanceof RamStorage<UUID> ramStorage) {
            maps = List.copyOf(ramStorage.get(m -> (m.get("player1").equals(player1) && m.get("player2").equals(player2)) || (m.get("player2").equals(player1) && m.get("player1").equals(player2))));
        } else if (STORAGE instanceof YamlStorage<UUID> yamlStorage) {
            maps = List.copyOf(yamlStorage.get(m -> (m.get("player1").equals(player1) && m.get("player2").equals(player2)) || (m.get("player2").equals(player1) && m.get("player1").equals(player2))));
        }

        return maps.isEmpty() ? null : (UUID) maps.get(0).get("uuid");
    }
}

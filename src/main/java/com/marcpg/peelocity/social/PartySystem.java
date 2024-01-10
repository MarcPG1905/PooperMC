package com.marcpg.peelocity.social;

import com.marcpg.peelocity.chat.MessageLogging;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static net.kyori.adventure.text.format.NamedTextColor.*;

public class PartySystem {
    /** Party UUID | < Player UUID | Is Party Leader > */
    public static final Map<UUID, Map<UUID, Boolean>> PARTIES = new HashMap<>();

    /** Player UUID | Party UUID */
    public static final Map<UUID, UUID> PLAYER_PARTIES = new HashMap<>();

    /** Inviter UUID | Invited player UUID */
    public static final Map<UUID, UUID> INVITES = new HashMap<>();

    public static @NotNull BrigadierCommand createPartyBrigadierCommand(final ProxyServer proxy) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("party")
                .requires(source -> source.hasPermission("pee.parties") && source instanceof Player)
                .then(LiteralArgumentBuilder.<CommandSource>literal("create")
                        .executes(context -> {
                            Player player = (Player) context.getSource();
                            if (PLAYER_PARTIES.containsKey(player.getUniqueId())) {
                                player.sendMessage(Component.text("You are already in a party! ", RED)
                                        .append(Component.text("Click here", GOLD)
                                                .clickEvent(ClickEvent.runCommand("/party leave"))
                                                .hoverEvent(HoverEvent.showText(Component.text("/party leave"))))
                                        .append(Component.text(" to leave your current party.", RED)));
                            } else {
                                UUID uuid = UUID.randomUUID();
                                PARTIES.put(uuid, new HashMap<>(Map.of(player.getUniqueId(), true)));
                                PLAYER_PARTIES.put(player.getUniqueId(), uuid);

                                player.sendMessage(Component.text("You just created your own party! You can now ", GREEN)
                                        .append(Component.text("invite your friends", YELLOW)
                                                .clickEvent(ClickEvent.suggestCommand("/party invite "))
                                                .hoverEvent(HoverEvent.showText(Component.text("/party invite <player>")))));
                            }
                            return 1;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("leave")
                        .executes(context -> {
                            Player player = (Player) context.getSource();
                            if (PLAYER_PARTIES.containsKey(player.getUniqueId())) {
                                if (PARTIES.get(PLAYER_PARTIES.get(player.getUniqueId())).get(player.getUniqueId())) {
                                    if (PARTIES.get(PLAYER_PARTIES.get(player.getUniqueId())).size() > 1) {
                                        player.sendMessage(Component.text("You need to promote someone else, before leaving the party!", RED));
                                        return 1;
                                    } else {
                                        PARTIES.remove(PLAYER_PARTIES.get(player.getUniqueId()));
                                    }
                                }
                                PARTIES.forEach((uuid, players) -> players.remove(player.getUniqueId()));
                                PLAYER_PARTIES.remove(player.getUniqueId());
                                player.sendMessage(Component.text("You just left the party!", YELLOW));
                            } else {
                                player.sendMessage(Component.text("You aren't in any party!", RED));
                            }
                            return 1;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("invite")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    UUID uuid = ((Player) context.getSource()).getUniqueId();
                                    if (PLAYER_PARTIES.containsKey(uuid) && PARTIES.get(PLAYER_PARTIES.get(uuid)).get(uuid)) {
                                        proxy.getAllPlayers().stream()
                                                .filter(player -> !PARTIES.get(PLAYER_PARTIES.get(uuid)).containsKey(player.getUniqueId()) && player != context.getSource())
                                                .map(Player::getUsername)
                                                .toList()
                                                .forEach(builder::suggest);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Player player = (Player) context.getSource();

                                    if (PARTIES.containsKey(PLAYER_PARTIES.get(player.getUniqueId()))) {
                                        if (!PARTIES.get(PLAYER_PARTIES.get(player.getUniqueId())).get(player.getUniqueId())) {
                                            player.sendMessage(Component.text("You need to be the leader of a party to invite someone!", RED));
                                            return 1;
                                        }

                                        Optional<Player> optionalTarget = proxy.getPlayer(context.getArgument("player", String.class));
                                        if (optionalTarget.isPresent()) {
                                            Player target = optionalTarget.get();
                                            target.sendMessage(Component.text(player.getUsername() + " invited you to his party! ", YELLOW)
                                                    .append(Component.text("Click here", GOLD)
                                                            .clickEvent(ClickEvent.runCommand("/party accept " + player.getUsername()))
                                                            .hoverEvent(HoverEvent.showText(Component.text("Click to accept the party invite!", GOLD))))
                                                    .append(Component.text(" to accept the invite!", YELLOW)));

                                            player.sendMessage(Component.text("You just sent a party invite to " + target.getUsername() + "!", GREEN));

                                            INVITES.put(player.getUniqueId(), target.getUniqueId());
                                        } else {
                                            player.sendMessage(Component.text("The player " + context.getArgument("player", String.class) + " was not found!", RED));
                                        }
                                    } else {
                                        player.sendMessage(Component.text("You aren't in any party!", RED));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("accept")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    if (!PLAYER_PARTIES.containsKey(((Player) context.getSource()).getUniqueId())) {
                                        proxy.getAllPlayers().stream()
                                                .map(Player::getUsername)
                                                .toList()
                                                .forEach(builder::suggest);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Player player = (Player) context.getSource();

                                    Optional<Player> optionalPlayer = proxy.getPlayer(context.getArgument("player", String.class));
                                    if (optionalPlayer.isPresent()) {
                                        Player inviter = optionalPlayer.get();

                                        if (PLAYER_PARTIES.containsKey(player.getUniqueId())) {
                                            player.sendMessage(Component.text("You are already in a party! ", RED)
                                                    .append(Component.text("Click here", GOLD)
                                                            .clickEvent(ClickEvent.runCommand("/party leave"))
                                                            .hoverEvent(HoverEvent.showText(Component.text("/party leave"))))
                                                    .append(Component.text(" to leave your current party.", RED)));
                                            return 1;
                                        }

                                        if (INVITES.containsKey(inviter.getUniqueId()) && INVITES.containsValue(player.getUniqueId())) {
                                            if (PLAYER_PARTIES.containsKey(inviter.getUniqueId())) {
                                                PLAYER_PARTIES.put(player.getUniqueId(), PLAYER_PARTIES.get(inviter.getUniqueId()));
                                                PARTIES.get(PLAYER_PARTIES.get(inviter.getUniqueId())).put(player.getUniqueId(), false);

                                                player.sendMessage(Component.text("Successfully joined the party!", GREEN));
                                                PARTIES.get(PLAYER_PARTIES.get(inviter.getUniqueId())).keySet().forEach(uuid -> proxy.getPlayer(uuid).ifPresent(player1 -> player1.sendMessage(Component.text(player.getUsername() + " joined the party!", YELLOW))));
                                            } else {
                                                player.sendMessage(Component.text(inviter.getUsername() + " already left the party, so you can't accept the party invite!", RED));
                                            }
                                            INVITES.remove(inviter.getUniqueId(), player.getUniqueId());
                                        } else {
                                            player.sendMessage(Component.text(inviter.getUsername() + " didn't send you a party invite!", RED));
                                        }
                                    } else {
                                        player.sendMessage(Component.text("The player " + context.getArgument("player", String.class) + " couldn't be found!", RED));
                                    }

                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("promote")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    UUID uuid = ((Player) context.getSource()).getUniqueId();
                                    if (PLAYER_PARTIES.containsKey(uuid) && PARTIES.get(PLAYER_PARTIES.get(uuid)).get(uuid)) {
                                        proxy.getAllPlayers().stream()
                                                .filter(player -> player != context.getSource() && PARTIES.get(PLAYER_PARTIES.get(((Player) context.getSource()).getUniqueId())).containsKey(player.getUniqueId()))
                                                .map(Player::getUsername)
                                                .toList()
                                                .forEach(builder::suggest);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Player player = (Player) context.getSource();

                                    if (PARTIES.containsKey(PLAYER_PARTIES.get(player.getUniqueId()))) {
                                        if (!PARTIES.get(PLAYER_PARTIES.get(player.getUniqueId())).get(player.getUniqueId())) {
                                            player.sendMessage(Component.text("You need to be the leader of a party to promote someone!", RED));
                                            return 1;
                                        }

                                        Optional<Player> target = proxy.getPlayer(context.getArgument("player", String.class));
                                        if (target.isPresent()) {
                                            if (PARTIES.get(PLAYER_PARTIES.get(player.getUniqueId())).containsKey(target.get().getUniqueId())) {
                                                PARTIES.get(PLAYER_PARTIES.get(player.getUniqueId())).put(player.getUniqueId(), false);
                                                PARTIES.get(PLAYER_PARTIES.get(player.getUniqueId())).put(target.get().getUniqueId(), true);

                                                player.sendMessage(Component.text("Successfully promoted " + target.get().getUsername() + " to be the new party leader!", GREEN));
                                                target.get().sendMessage(Component.text("You are now the party leader!", GREEN));
                                            } else {
                                                player.sendMessage(Component.text("The player " + target.get().getUsername() + " is not in your party!", RED));
                                            }
                                        } else {
                                            player.sendMessage(Component.text("The player " + context.getArgument("player", String.class) + " was not found!", RED));
                                        }
                                    } else {
                                        player.sendMessage(Component.text("You aren't in any party!", RED));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("message")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("content", StringArgumentType.greedyString())
                                .executes(context -> {
                                    Player sender = (Player) context.getSource();

                                    if (PLAYER_PARTIES.containsKey(sender.getUniqueId())) {
                                        String content = context.getArgument("content", String.class);

                                        MessageLogging.saveMessage(sender, new MessageLogging.MessageData(new Date(), content, MessageLogging.MessageData.Type.PARTY, null));
                                        for (UUID uuid : PARTIES.get(PLAYER_PARTIES.get(sender.getUniqueId())).keySet()) {
                                            proxy.getPlayer(uuid).ifPresent(player -> player.sendMessage(Component.text("[PARTY] <" + sender.getUsername() + "> " + content, DARK_AQUA)));
                                        }
                                    } else {
                                        sender.sendMessage(Component.text("You aren't in any party!", RED));
                                    }

                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("list")
                        .executes(context -> {
                            Player sender = (Player) context.getSource();
                            if (PLAYER_PARTIES.containsKey(sender.getUniqueId())) {
                                for (Map.Entry<UUID, Boolean> playerData : PARTIES.get(PLAYER_PARTIES.get(sender.getUniqueId())).entrySet()) {
                                    Optional<Player> optionalPlayer = proxy.getPlayer(playerData.getKey());
                                    optionalPlayer.ifPresent(player -> sender.sendMessage(Component.text("- ").append(Component.text(player.getUsername() + (playerData.getValue() ? " (Leader)" : ""), playerData.getValue() ? GOLD : WHITE))));
                                }
                            } else {
                                sender.sendMessage(Component.text("You aren't in any party!", RED));
                            }
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }
}

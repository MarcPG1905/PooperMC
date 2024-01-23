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
import net.hectus.lang.Translation;
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

    public static @NotNull BrigadierCommand createPartyBrigadier(final ProxyServer proxy) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("party")
                .requires(source -> source.hasPermission("pee.parties") && source instanceof Player)
                .then(LiteralArgumentBuilder.<CommandSource>literal("create")
                        .executes(context -> {
                            Player player = (Player) context.getSource();
                            Locale l = player.getEffectiveLocale();
                            if (PLAYER_PARTIES.containsKey(player.getUniqueId())) {
                                player.sendMessage(Translation.component(l, "party.leave-msg.1").color(RED)
                                        .append(Translation.component(l, "party.leave-msg.2", GOLD)
                                                .clickEvent(ClickEvent.runCommand("/party leave"))
                                                .hoverEvent(HoverEvent.showText(Component.text("/party leave"))))
                                        .append(Translation.component(l, "party.leave-msg.3", RED)));
                            } else {
                                UUID uuid = UUID.randomUUID();
                                PARTIES.put(uuid, new HashMap<>(Map.of(player.getUniqueId(), true)));
                                PLAYER_PARTIES.put(player.getUniqueId(), uuid);

                                player.sendMessage(Translation.component(l, "party.create.confirm.1").color(GREEN)
                                        .append(Translation.component(l, "party.create.confirm.2").color(YELLOW)
                                                .clickEvent(ClickEvent.suggestCommand("/party invite "))
                                                .hoverEvent(HoverEvent.showText(Component.text("/party invite <player>")))));
                            }
                            return 1;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("leave")
                        .executes(context -> {
                            Player player = (Player) context.getSource();
                            Locale l = player.getEffectiveLocale();
                            if (PLAYER_PARTIES.containsKey(player.getUniqueId())) {
                                if (PARTIES.get(PLAYER_PARTIES.get(player.getUniqueId())).get(player.getUniqueId())) {
                                    if (PARTIES.get(PLAYER_PARTIES.get(player.getUniqueId())).size() > 1) {
                                        player.sendMessage(Translation.component(l, "party.leave.leader").color(RED));
                                        return 1;
                                    } else {
                                        PARTIES.remove(PLAYER_PARTIES.get(player.getUniqueId()));
                                    }
                                }
                                PARTIES.forEach((uuid, players) -> players.remove(player.getUniqueId()));
                                PLAYER_PARTIES.remove(player.getUniqueId());
                                player.sendMessage(Translation.component(l, "party.leave.confirm").color(YELLOW));
                            } else {
                                player.sendMessage(Translation.component(l, "party.not_in_any").color(RED));
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
                                    Locale l = player.getEffectiveLocale();

                                    if (PARTIES.containsKey(PLAYER_PARTIES.get(player.getUniqueId()))) {
                                        if (!PARTIES.get(PLAYER_PARTIES.get(player.getUniqueId())).get(player.getUniqueId())) {
                                            player.sendMessage(Translation.component(l, "party.invite.not_leader").color(RED));
                                            return 1;
                                        }

                                        Optional<Player> optionalTarget = proxy.getPlayer(context.getArgument("player", String.class));
                                        if (optionalTarget.isPresent()) {
                                            Player target = optionalTarget.get();
                                            target.sendMessage(Translation.component(l, "party.invite.msg.1", player.getUsername()).color(YELLOW)
                                                    .append(Translation.component(l, "party.invite.msg.2").color(GOLD)
                                                            .clickEvent(ClickEvent.runCommand("/party accept " + player.getUsername()))
                                                            .hoverEvent(HoverEvent.showText(Translation.component(l, "party.invite.msg.2.tooltip").color(GOLD))))
                                                    .append(Translation.component(l, "party.invite.msg.3").color(YELLOW)));

                                            player.sendMessage(Translation.component(l, "party.invite.confirm", target.getUsername()).color(GREEN));

                                            INVITES.put(player.getUniqueId(), target.getUniqueId());
                                        } else {
                                            player.sendMessage(Translation.component(l, "cmd.player_not_found", context.getArgument("player", String.class)).color(RED));
                                        }
                                    } else {
                                        player.sendMessage(Translation.component(l, "party.not_in_any").color(RED));
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
                                    Locale l = player.getEffectiveLocale();

                                    Optional<Player> optionalPlayer = proxy.getPlayer(context.getArgument("player", String.class));
                                    if (optionalPlayer.isPresent()) {
                                        Player inviter = optionalPlayer.get();

                                        if (PLAYER_PARTIES.containsKey(player.getUniqueId())) {
                                            player.sendMessage(Translation.component(l, "party.leave-msg.1").color(RED)
                                                    .append(Translation.component(l, "party.leave-msg.2", GOLD)
                                                            .clickEvent(ClickEvent.runCommand("/party leave"))
                                                            .hoverEvent(HoverEvent.showText(Component.text("/party leave"))))
                                                    .append(Translation.component(l, "party.leave-msg.3", RED)));
                                            return 1;
                                        }

                                        if (INVITES.containsKey(inviter.getUniqueId()) && INVITES.containsValue(player.getUniqueId())) {
                                            if (PLAYER_PARTIES.containsKey(inviter.getUniqueId())) {
                                                PLAYER_PARTIES.put(player.getUniqueId(), PLAYER_PARTIES.get(inviter.getUniqueId()));
                                                PARTIES.get(PLAYER_PARTIES.get(inviter.getUniqueId())).put(player.getUniqueId(), false);

                                                player.sendMessage(Translation.component(l, "party.accept.confirm").color(GREEN));
                                                PARTIES.get(PLAYER_PARTIES.get(inviter.getUniqueId())).keySet().forEach(uuid -> proxy.getPlayer(uuid).ifPresent(player1 -> player1.sendMessage(Component.text(player.getUsername() + " joined the party!", YELLOW))));
                                            } else {
                                                player.sendMessage(Translation.component(l, "party.accept.too_late", inviter.getUsername()).color(RED));
                                            }
                                            INVITES.remove(inviter.getUniqueId(), player.getUniqueId());
                                        } else {
                                            player.sendMessage(Translation.component(l, "party.accept.no_invite", inviter.getUsername()).color(RED));
                                        }
                                    } else {
                                        player.sendMessage(Translation.component(l, "cmd.player_not_found", context.getArgument("player", String.class)).color(RED));
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
                                    Locale l = player.getEffectiveLocale();

                                    if (PARTIES.containsKey(PLAYER_PARTIES.get(player.getUniqueId()))) {
                                        if (!PARTIES.get(PLAYER_PARTIES.get(player.getUniqueId())).get(player.getUniqueId())) {
                                            player.sendMessage(Translation.component(l, "party.promote.not_leader").color(RED));
                                            return 1;
                                        }

                                        Optional<Player> target = proxy.getPlayer(context.getArgument("player", String.class));
                                        if (target.isPresent()) {
                                            if (PARTIES.get(PLAYER_PARTIES.get(player.getUniqueId())).containsKey(target.get().getUniqueId())) {
                                                PARTIES.get(PLAYER_PARTIES.get(player.getUniqueId())).put(player.getUniqueId(), false);
                                                PARTIES.get(PLAYER_PARTIES.get(player.getUniqueId())).put(target.get().getUniqueId(), true);

                                                player.sendMessage(Translation.component(l, "party.promote.confirm", target.get().getUsername()).color(GREEN));
                                                target.get().sendMessage(Translation.component(l, "party.promote.msg").color(GREEN));
                                            } else {
                                                player.sendMessage(Translation.component(l, "party.promote.player_not_in_party", target.get().getUsername()).color(RED));
                                            }
                                        } else {
                                            player.sendMessage(Translation.component(l, "cmd.player_not_found", context.getArgument("player", String.class)).color(RED));
                                        }
                                    } else {
                                        player.sendMessage(Translation.component(l, "party.not_in_any").color(RED));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("message")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("content", StringArgumentType.greedyString())
                                .executes(context -> {
                                    Player player = (Player) context.getSource();

                                    if (PLAYER_PARTIES.containsKey(player.getUniqueId())) {
                                        String content = context.getArgument("content", String.class);

                                        MessageLogging.saveMessage(player, new MessageLogging.MessageData(new Date(), content, MessageLogging.MessageData.Type.PARTY, null));
                                        for (UUID uuid : PARTIES.get(PLAYER_PARTIES.get(player.getUniqueId())).keySet()) {
                                            proxy.getPlayer(uuid).ifPresent(target -> target.sendMessage(Translation.component(target.getEffectiveLocale(), "party.message", player.getUsername(), content).color(DARK_AQUA)));
                                        }
                                    } else {
                                        player.sendMessage(Translation.component(player.getEffectiveLocale(), "party.not_in_any").color(RED));
                                    }

                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("list")
                        .executes(context -> {
                            Player player = (Player) context.getSource();
                            if (PLAYER_PARTIES.containsKey(player.getUniqueId())) {
                                for (Map.Entry<UUID, Boolean> playerData : PARTIES.get(PLAYER_PARTIES.get(player.getUniqueId())).entrySet()) {
                                    Optional<Player> optionalPlayer = proxy.getPlayer(playerData.getKey());
                                    optionalPlayer.ifPresent(target -> player.sendMessage(Component.text("- ").append(Component.text(target.getUsername() + (playerData.getValue() ? Translation.string(player.getEffectiveLocale(), "party.list.leader") : ""), playerData.getValue() ? GOLD : WHITE))));
                                }
                            } else {
                                player.sendMessage(Translation.component(player.getEffectiveLocale(), "party.not_in_any").color(RED));
                            }
                            return 1;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .executes(context -> {
                            context.getSource().sendMessage(Component.text("""
                                    §l§nHelp:§r §l/party§r
                                    The command /party provides all kinds of utilities for managing your party/group.
                                    
                                    §l§nArguments:§r
                                    - §lcreate§r: Creates a brand new party with only you.
                                    - §lleave§r: Leaves your current party, if in any.
                                    - §linvite§r: Invites someone from your friend list into your party.
                                    - §laccept§r: Accepts someone's party invite, if he sent any.
                                    - §lpromote§r: Promotes someone else to be the party leader
                                    - §lmessage§r: Sends a message to your party's private chat.
                                    - §llist§r: Lists of players in your current party.
                                    """));
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }
}

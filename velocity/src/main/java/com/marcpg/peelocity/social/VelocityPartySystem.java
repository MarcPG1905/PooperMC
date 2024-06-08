package com.marcpg.peelocity.social;

import com.marcpg.libpg.lang.Translation;
import com.marcpg.libpg.util.Randomizer;
import com.marcpg.peelocity.PeelocityPlugin;
import com.marcpg.common.optional.PlayerCache;
import com.marcpg.peelocity.common.VelocityPlayer;
import com.marcpg.common.entity.OnlinePlayer;
import com.marcpg.common.features.MessageLogging;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class VelocityPartySystem {
    private static final HashMap<UUID, HashSet<UUID>> INVITATIONS = new HashMap<>();
    public static final Map<UUID, HashMap<UUID, Boolean>> PARTIES = new HashMap<>();
    public static final Map<UUID, UUID> PLAYER_PARTIES = new HashMap<>();

    @Subscribe(order = PostOrder.LATE)
    public void onDisconnect(@NotNull DisconnectEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();

        if (PLAYER_PARTIES.containsKey(playerUuid)) {
            Map<UUID, Boolean> party = PARTIES.get(PLAYER_PARTIES.get(playerUuid));
            if (party.get(playerUuid)) {
                if (party.size() > 1) {
                    party.remove(playerUuid);
                    party.put(Randomizer.fromCollection(party.keySet()), true);
                } else {
                    PARTIES.remove(PLAYER_PARTIES.get(playerUuid));
                }
            }
            party.remove(playerUuid);
            PLAYER_PARTIES.remove(playerUuid);
        }
    }

    public static @NotNull BrigadierCommand command() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("party")
                .requires(source -> source instanceof Player)
                .then(LiteralArgumentBuilder.<CommandSource>literal("create")
                        .executes(context -> {
                            Player player = (Player) context.getSource();
                            UUID playerUuid = player.getUniqueId();
                            Locale l = player.getEffectiveLocale();

                            if (PLAYER_PARTIES.containsKey(playerUuid)) {
                                player.sendMessage(Translation.component(l, "party.leave-msg.1").color(RED)
                                        .appendSpace()
                                        .append(Translation.component(l, "party.leave-msg.2", GOLD)
                                                .hoverEvent(HoverEvent.showText(Component.text("/party leave")))
                                                .clickEvent(ClickEvent.runCommand("/party leave")))
                                        .appendSpace()
                                        .append(Translation.component(l, "party.leave-msg.3", RED)));
                            } else {
                                UUID partyUuid = UUID.randomUUID();
                                PARTIES.put(partyUuid, new HashMap<>(Map.of(playerUuid, true)));
                                PLAYER_PARTIES.put(playerUuid, partyUuid);

                                player.sendMessage(Translation.component(l, "party.create.confirm.1").color(GREEN)
                                        .appendSpace()
                                        .append(Translation.component(l, "party.create.confirm.2").color(YELLOW)
                                                .clickEvent(ClickEvent.suggestCommand("/party invite "))
                                                .hoverEvent(HoverEvent.showText(Component.text("/party invite <player>")))));
                            }

                            return 1;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("invite")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    UUID playerUuid = ((Player) context.getSource()).getUniqueId();
                                    if (PLAYER_PARTIES.containsKey(playerUuid)) {
                                        Set<UUID> playersInParty = PARTIES.get(PLAYER_PARTIES.get(playerUuid)).keySet();
                                        PeelocityPlugin.SERVER.getAllPlayers().parallelStream()
                                                .filter(player -> !playersInParty.contains(playerUuid) && player != context.getSource())
                                                .map(Player::getUsername)
                                                .forEach(builder::suggest);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Player player = (Player) context.getSource();
                                    UUID playerUuid = player.getUniqueId();
                                    Locale l = player.getEffectiveLocale();
                                    String targetArg = context.getArgument("player", String.class);
                                    UUID targetUuid = PlayerCache.getUuid(targetArg);

                                    UUID partyUuid = PLAYER_PARTIES.get(playerUuid);

                                    if (partyUuid == null) {
                                        player.sendMessage(Translation.component(l, "party.not_in_any").color(RED));
                                        return 1;
                                    }
                                    if (!PARTIES.get(partyUuid).get(playerUuid)) {
                                        player.sendMessage(Translation.component(l, "party.invite.not_leader").color(RED));
                                        return 1;
                                    }
                                    if (PARTIES.get(partyUuid).containsKey(targetUuid)) {
                                        player.sendMessage(Translation.component(l, "party.invite.already_in_party", targetArg).color(RED));
                                        return 1;
                                    }

                                    PeelocityPlugin.SERVER.getPlayer(targetUuid).ifPresentOrElse(
                                            target -> {
                                                if (INVITATIONS.containsKey(targetUuid)) {
                                                    INVITATIONS.get(targetUuid).add(playerUuid);
                                                } else {
                                                    INVITATIONS.put(targetUuid, new HashSet<>(Set.of(playerUuid)));
                                                }
                                                player.sendMessage(Translation.component(l, "party.invite.confirm", targetArg));

                                                Locale tl = target.getEffectiveLocale();
                                                target.sendMessage(Translation.component(tl, "party.invite.msg.1", player.getUsername()).color(YELLOW)
                                                        .appendSpace()
                                                        .append(Translation.component(tl, "party.invite.msg.2").color(GOLD)
                                                                .clickEvent(ClickEvent.runCommand("/party accept " + player.getUsername()))
                                                                .hoverEvent(HoverEvent.showText(Translation.component(tl, "party.invite.msg.2.tooltip"))))
                                                        .appendSpace()
                                                        .append(Translation.component(tl, "party.invite.msg.3").color(YELLOW)));
                                            },
                                            () -> player.sendMessage(Translation.component(l, "cmd.player_not_found", targetArg).color(RED))
                                    );

                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("accept")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    UUID playerUuid = ((Player) context.getSource()).getUniqueId();
                                    if (INVITATIONS.containsKey(playerUuid)) {
                                        INVITATIONS.get(playerUuid).parallelStream().forEach(uuid -> {
                                            if (uuid != playerUuid)
                                                builder.suggest(PlayerCache.PLAYERS.get(uuid));
                                        });
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Player player = (Player) context.getSource();
                                    UUID playerUuid = player.getUniqueId();
                                    Locale l = player.getEffectiveLocale();
                                    String targetArg = context.getArgument("player", String.class);
                                    UUID targetUuid = PlayerCache.getUuid(targetArg);

                                    if (PLAYER_PARTIES.containsKey(playerUuid)) {
                                        player.sendMessage(Translation.component(l, "party.leave-msg.1").color(RED)
                                                .appendSpace()
                                                .append(Translation.component(l, "party.leave-msg.2", GOLD)
                                                        .clickEvent(ClickEvent.runCommand("/party leave"))
                                                        .hoverEvent(HoverEvent.showText(Component.text("/party leave"))))
                                                .appendSpace()
                                                .append(Translation.component(l, "party.leave-msg.3", RED)));
                                        return 1;
                                    }
                                    if (!INVITATIONS.containsKey(playerUuid) || !INVITATIONS.get(playerUuid).contains(targetUuid)) {
                                        player.sendMessage(Translation.component(l, "party.accept.no_invite", targetArg).color(RED));
                                        return 1;
                                    }
                                    if (!PLAYER_PARTIES.containsKey(targetUuid)) {
                                        player.sendMessage(Translation.component(l, "party.accept.too_late", targetArg).color(RED));
                                        return 1;
                                    }

                                    PeelocityPlugin.SERVER.getPlayer(targetUuid).ifPresentOrElse(
                                            target -> {
                                                PARTIES.get(PLAYER_PARTIES.get(targetUuid)).keySet().forEach(uuid -> PeelocityPlugin.SERVER.getPlayer(uuid).ifPresent(
                                                        p -> p.sendMessage(Translation.component(player.getEffectiveLocale(), "party.accept.msg", player.getUsername()).color(YELLOW))));

                                                PLAYER_PARTIES.put(playerUuid, PLAYER_PARTIES.get(targetUuid));
                                                PARTIES.get(PLAYER_PARTIES.get(targetUuid)).put(playerUuid, false);

                                                player.sendMessage(Translation.component(l, "party.accept.confirm").color(GREEN));

                                                INVITATIONS.get(playerUuid).remove(targetUuid);
                                                if (INVITATIONS.get(playerUuid).isEmpty())
                                                    INVITATIONS.remove(playerUuid);
                                            },
                                            () -> player.sendMessage(Translation.component(l, "cmd.player_not_found", targetArg).color(RED))
                                    );
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("deny")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    UUID playerUuid = ((Player) context.getSource()).getUniqueId();
                                    if (INVITATIONS.containsKey(playerUuid)) {
                                        INVITATIONS.get(playerUuid).parallelStream().forEach(uuid -> {
                                            if (uuid != playerUuid)
                                                builder.suggest(PlayerCache.PLAYERS.get(uuid));
                                        });
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Player player = (Player) context.getSource();
                                    UUID playerUuid = player.getUniqueId();
                                    String targetArg = context.getArgument("player", String.class);
                                    UUID targetUuid = PlayerCache.getUuid(targetArg);

                                    if (!INVITATIONS.containsKey(playerUuid) || !INVITATIONS.get(playerUuid).contains(targetUuid)) {
                                        player.sendMessage(Translation.component(player.getEffectiveLocale(), "party.accept.no_invite", targetArg).color(RED));
                                        return 1;
                                    }

                                    INVITATIONS.get(playerUuid).remove(targetUuid);
                                    if (INVITATIONS.get(playerUuid).isEmpty())
                                        INVITATIONS.remove(playerUuid);

                                    player.sendMessage(Translation.component(player.getEffectiveLocale(), "party.deny.confirm").color(YELLOW));
                                    PeelocityPlugin.SERVER.getPlayer(targetUuid).ifPresent(target -> target.sendMessage(Translation.component(target.getEffectiveLocale(), "party.deny.msg", player.getUsername()).color(RED)));

                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("leave")
                        .executes(context -> {
                            Player player = (Player) context.getSource();
                            UUID playerUuid = player.getUniqueId();
                            Locale l = player.getEffectiveLocale();

                            if (PLAYER_PARTIES.containsKey(playerUuid)) {
                                Map<UUID, Boolean> party = PARTIES.get(PLAYER_PARTIES.get(playerUuid));
                                if (party.get(playerUuid)) {
                                    if (party.size() > 1) {
                                        player.sendMessage(Translation.component(l, "party.leave.leader").color(RED));
                                        return 1;
                                    } else {
                                        PARTIES.remove(PLAYER_PARTIES.get(playerUuid));
                                    }
                                } else {
                                    PARTIES.get(PLAYER_PARTIES.get(playerUuid)).keySet().forEach(uuid -> PeelocityPlugin.SERVER.getPlayer(uuid).ifPresent(
                                            p -> p.sendMessage(Translation.component(player.getEffectiveLocale(), "party.leave.msg", player.getUsername()).color(YELLOW))));
                                }
                                party.remove(playerUuid);
                                PLAYER_PARTIES.remove(playerUuid);
                                player.sendMessage(Translation.component(l, "party.leave.confirm").color(YELLOW));
                            } else {
                                player.sendMessage(Translation.component(l, "party.not_in_any").color(RED));
                            }
                            return 1;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("promote")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    UUID playerUuid = ((Player) context.getSource()).getUniqueId();
                                    if (PLAYER_PARTIES.containsKey(playerUuid)) {
                                        PARTIES.get(PLAYER_PARTIES.get(playerUuid)).entrySet().stream()
                                                .filter(e -> !e.getValue() && e.getKey() != playerUuid)
                                                .forEach(e -> PeelocityPlugin.SERVER.getPlayer(e.getKey()).ifPresent(p -> builder.suggest(p.getUsername())));
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Player player = (Player) context.getSource();
                                    UUID playerUuid = player.getUniqueId();
                                    Locale l = player.getEffectiveLocale();
                                    String targetArg = context.getArgument("player", String.class);
                                    UUID targetUuid = PlayerCache.getUuid(targetArg);

                                    UUID partyUuid = PLAYER_PARTIES.get(playerUuid);

                                    if (partyUuid == null) {
                                        player.sendMessage(Translation.component(l, "party.not_in_any").color(RED));
                                        return 1;
                                    }
                                    Map<UUID, Boolean> party = PARTIES.get(partyUuid);
                                    if (!party.get(playerUuid)) {
                                        player.sendMessage(Translation.component(l, "party.promote.not_leader").color(RED));
                                        return 1;
                                    }
                                    if (!party.containsKey(targetUuid)) {
                                        player.sendMessage(Translation.component(l, "party.promote.player_not_in_party", targetArg).color(RED));
                                        return 1;
                                    }

                                    PeelocityPlugin.SERVER.getPlayer(targetUuid).ifPresentOrElse(
                                            target -> {
                                                party.put(playerUuid, false);
                                                party.put(targetUuid, true);
                                                player.sendMessage(Translation.component(l, "party.promote.confirm", targetArg).color(YELLOW));
                                                target.sendMessage(Translation.component(target.getEffectiveLocale(), "party.promote.msg").color(GREEN));
                                            },
                                            () -> player.sendMessage(Translation.component(l, "cmd.player_not_found", targetArg).color(RED))
                                    );
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("message")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("content", StringArgumentType.greedyString())
                                .executes(context -> {
                                    OnlinePlayer<Player> player = VelocityPlayer.ofPlayer((Player) context.getSource());
                                    UUID playerUuid = player.uuid();

                                    if (PLAYER_PARTIES.containsKey(playerUuid)) {
                                        String content = context.getArgument("content", String.class);

                                        MessageLogging.saveMessage(player, new MessageLogging.MessageData(new Date(), content, MessageLogging.MessageData.Type.PARTY, null));

                                        for (UUID uuid : PARTIES.get(PLAYER_PARTIES.get(playerUuid)).keySet()) {
                                            PeelocityPlugin.SERVER.getPlayer(uuid).ifPresent(t ->
                                                    t.sendMessage(Translation.component(t.getEffectiveLocale(), "party.message", player.name(), content).color(DARK_AQUA)));
                                        }
                                    } else {
                                        player.sendMessage(Translation.component(player.locale(), "party.not_in_any").color(RED));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("list")
                        .executes(context -> {
                            Player player = (Player) context.getSource();
                            UUID playerUuid = player.getUniqueId();
                            Locale l = player.getEffectiveLocale();

                            if (PLAYER_PARTIES.containsKey(playerUuid)) {
                                for (Map.Entry<UUID, Boolean> target : PARTIES.get(PLAYER_PARTIES.get(playerUuid)).entrySet()) {
                                    player.sendMessage(Component.text("- " + PlayerCache.PLAYERS.get(target.getKey()) + (target.getValue() ? " " + Translation.string(l, "party.list.leader") : ""), target.getValue() ? GOLD : WHITE));
                                }
                            } else {
                                player.sendMessage(Translation.component(l, "party.not_in_any").color(RED));
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
                                    -§l create§r: Creates a brand new party with only you.
                                    -§l leave§r: Leaves your current party, if in any.
                                    -§l invite§r: Invites someone from your friend list into your party.
                                    -§l accept§r: Accepts someone's party invite, if he sent any.
                                    -§l promote§r: Promotes someone else to be the party leader
                                    -§l message§r: Sends a message to your party's private chat.
                                    -§l list§r: Lists of players in your current party.
                                    """));
                            return 1;
                        })
                )
                .build()
        );
    }
}

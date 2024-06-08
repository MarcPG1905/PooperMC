package com.marcpg.peelocity.social;

import com.marcpg.common.entity.IdentifiablePlayer;
import com.marcpg.common.entity.OfflinePlayer;
import com.marcpg.common.social.FriendSystem;
import com.marcpg.common.util.InvalidCommandArgsException;
import com.marcpg.common.util.ThrowingBiConsumer;
import com.marcpg.libpg.lang.Translation;
import com.marcpg.peelocity.PeelocityPlugin;
import com.marcpg.common.optional.PlayerCache;
import com.marcpg.peelocity.common.VelocityPlayer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class VelocityFriendSystem {
    public static @NotNull BrigadierCommand command() {
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
                                    handle((Player) context.getSource(), context.getArgument("player", String.class), "add", FriendSystem::add);
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("remove")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    UUID playerUuid = ((Player) context.getSource()).getUniqueId();
                                    FriendSystem.getFriendships(((Player) context.getSource()).getUniqueId()).forEach(m -> {
                                        UUID friend = (UUID) (m.get("player1").equals(playerUuid) ? m.get("player2") : m.get("player1"));
                                        builder.suggest(PlayerCache.PLAYERS.get(friend));
                                    });
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    handle((Player) context.getSource(), context.getArgument("player", String.class), "remove", FriendSystem::remove);
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("accept")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    UUID sourceUuid = ((Player) context.getSource()).getUniqueId();
                                    if (FriendSystem.REQUESTS.containsKey(sourceUuid))
                                        FriendSystem.REQUESTS.get(sourceUuid).forEach(uuid -> builder.suggest(PlayerCache.PLAYERS.get(uuid)));
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    handle((Player) context.getSource(), context.getArgument("player", String.class), "accept", FriendSystem::accept);
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("deny")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    UUID sourceUuid = ((Player) context.getSource()).getUniqueId();
                                    if (FriendSystem.REQUESTS.containsKey(sourceUuid))
                                        FriendSystem.REQUESTS.get(sourceUuid).forEach(uuid -> builder.suggest(PlayerCache.PLAYERS.get(uuid)));
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    handle((Player) context.getSource(), context.getArgument("player", String.class), "deny", FriendSystem::deny);
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("list")
                        .executes(context -> {
                            Player player = (Player) context.getSource();
                            UUID playerUuid = player.getUniqueId();

                            List<Map<String, Object>> friendships = FriendSystem.getFriendships(playerUuid);

                            if (friendships.isEmpty()) {
                                player.sendMessage(Translation.component(player.getEffectiveLocale(), "friend.list.none").color(YELLOW));
                            } else {
                                player.sendMessage(Translation.component(player.getEffectiveLocale(), "friend.list.list", friendships.size()).color(GREEN));
                                friendships.forEach(m -> player.sendMessage(Component.text("- " + PlayerCache.PLAYERS.get((UUID) (m.get("player1").equals(playerUuid) ? m.get("player2") : m.get("player1"))))));
                            }
                            return 1;
                        })
                )
                .build()
        );
    }

    public static void handle(Player player, String target, String operationName, ThrowingBiConsumer<VelocityPlayer, IdentifiablePlayer, InvalidCommandArgsException> operation) {
        UUID targetUuid = PlayerCache.getUuid(target);
        if (targetUuid == null) {
            player.sendMessage(Translation.component(player.getEffectiveLocale(), "cmd.player_not_found", target).color(RED));
        } else {
            try {
                Optional<Player> p = PeelocityPlugin.SERVER.getPlayer(targetUuid);
                operation.accept(VelocityPlayer.ofPlayer(player), p.isPresent() ? new VelocityPlayer(p.get()) : new OfflinePlayer(target, targetUuid));
                player.sendMessage(Translation.component(player.getEffectiveLocale(), "friend." + operationName + ".confirm", target).color(YELLOW));
            } catch (InvalidCommandArgsException e) {
                player.sendMessage(e.translatable(player.getEffectiveLocale()));
            }
        }
    }
}

package com.marcpg.peelocity.moderation;

import com.marcpg.libpg.data.time.Time;
import com.marcpg.libpg.lang.Translation;
import com.marcpg.peelocity.PlayerCache;
import com.marcpg.peelocity.Peelocity;
import com.marcpg.peelocity.common.VelocityPlayer;
import com.marcpg.common.entity.OfflinePlayer;
import com.marcpg.common.moderation.Muting;
import com.marcpg.common.util.InvalidCommandArgsException;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;

public final class VelocityMuting {
    private static final List<String> TIME_TYPES = List.of("sec", "min", "h", "d", "wk", "mo");

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChat(@NotNull PlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Locale l = player.getEffectiveLocale();

        if (!Muting.STORAGE.contains(uuid)) return;

        Map<String, Object> mute = Muting.STORAGE.get(uuid);

        if (player.hasPermission("poo.mute") || player.hasPermission("poo.admin")) {
            Muting.STORAGE.remove(uuid);
        } else if ((System.currentTimeMillis() * 0.001) > (Long) mute.get("expires")) {
            Muting.STORAGE.remove(uuid);
            player.sendMessage(Translation.component(l, "moderation.mute.expired.msg").color(NamedTextColor.GREEN));
        } else {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            player.sendMessage(Translation.component(l, "moderation.mute.warning").color(NamedTextColor.RED));
            player.sendMessage(Translation.component(l, "moderation.expiration", Time.preciselyFormat((Long) mute.get("expires") - Instant.now().getEpochSecond())).color(NamedTextColor.GOLD));
            player.sendMessage(Translation.component(l, "moderation.reason", mute.get("reason")).color(NamedTextColor.GOLD));
        }
    }

    @Contract(" -> new")
    public static @NotNull BrigadierCommand muteCommand() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("mute")
                .requires(source -> source.hasPermission("poo.mute"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            String sourceName = context.getSource() instanceof Player player ? player.getUsername() : "";
                            PlayerCache.PLAYERS.values().parallelStream()
                                    .filter(s -> !sourceName.equals(s))
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("time", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    String input = context.getArguments().size() == 2 ? builder.getInput().split(" ")[0] : "";
                                    TIME_TYPES.forEach(string -> builder.suggest(input.replaceAll("[^-\\d.]+", "") + string));
                                    return builder.buildFuture();
                                })
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("reason", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            CommandSource source = context.getSource();
                                            Locale l = source instanceof Player player ? player.getEffectiveLocale() : Locale.getDefault();
                                            String targetArg = context.getArgument("player", String.class);
                                            UUID targetUuid = PlayerCache.getUuid(targetArg);

                                            if (targetUuid == null) {
                                                source.sendMessage(Translation.component(l, "cmd.player_not_found", targetArg).color(NamedTextColor.RED));
                                            } else {
                                                Optional<Player> target = Peelocity.SERVER.getPlayer(targetUuid);
                                                Time time = Time.parse(context.getArgument("time", String.class));
                                                String reason = context.getArgument("reason", String.class);

                                                try {
                                                    if (target.isPresent()) {
                                                        Muting.mute(source instanceof Player player ? player.getUsername() : "Console",
                                                                new VelocityPlayer(target.get()), time, reason);
                                                    } else {
                                                        Muting.mute(source instanceof Player player ? player.getUsername() : "Console",
                                                                new OfflinePlayer(targetArg, targetUuid), time, reason);
                                                    }
                                                    source.sendMessage(Translation.component(l, "moderation.mute.confirm", targetArg, time.getPreciselyFormatted(), reason).color(NamedTextColor.YELLOW));
                                                } catch (InvalidCommandArgsException e) {
                                                    source.sendMessage(e.translatable(l));
                                                }
                                            }
                                            return 1;
                                        })
                                )
                        )
                )
                .build()
        );
    }

    @Contract(" -> new")
    public static @NotNull BrigadierCommand unmuteCommand() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("unmute")
                .requires(source -> source.hasPermission("poo.mute"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            String sourceName = context.getSource() instanceof Player player ? player.getUsername() : "";
                            Muting.STORAGE.getAll().parallelStream()
                                    .map(m -> (UUID) m.get("player"))
                                    .map(PlayerCache.PLAYERS::get)
                                    .filter(s -> !sourceName.equals(s))
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            Locale l = source instanceof Player player ? player.getEffectiveLocale() : Locale.getDefault();
                            String targetArg = context.getArgument("player", String.class);

                            if (PlayerCache.PLAYERS.containsValue(targetArg)) {
                                source.sendMessage(Translation.component(l, "cmd.player_not_found", targetArg).color(NamedTextColor.RED));
                            } else {
                                try {
                                    Muting.unmute(source instanceof Player player ? player.getUsername() : "Console", new OfflinePlayer(targetArg, PlayerCache.getUuid(targetArg)));
                                    source.sendMessage(Translation.component(l, "moderation.unmute.confirm", targetArg).color(NamedTextColor.YELLOW));
                                } catch (InvalidCommandArgsException e) {
                                    source.sendMessage(e.translatable(l));
                                }
                            }
                            return 1;
                        })
                )
                .build()
        );
    }
}

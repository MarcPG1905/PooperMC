package com.marcpg.peelocity.moderation;

import com.marcpg.libpg.data.time.Time;
import com.marcpg.libpg.lang.Translation;
import com.marcpg.peelocity.PlayerCache;
import com.marcpg.peelocity.Peelocity;
import com.marcpg.peelocity.common.VelocityPlayer;
import com.marcpg.common.entity.OfflinePlayer;
import com.marcpg.common.moderation.Banning;
import com.marcpg.common.util.InvalidCommandArgsException;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;

public final class VelocityBanning {
    @Subscribe(order = PostOrder.FIRST)
    public void onLogin(@NotNull LoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Locale l = event.getPlayer().getEffectiveLocale();

        if (!Banning.STORAGE.contains(uuid)) return;

        Map<String, Object> ban = Banning.STORAGE.get(uuid);

        if (player.hasPermission("poo.ban") || player.hasPermission("poo.admin")) {
            Banning.STORAGE.remove(uuid);
        } else if ((Boolean) ban.get("permanent") && (System.currentTimeMillis() * 0.001) > (Long) ban.get("expires")) {
            Banning.STORAGE.remove(uuid);
            player.sendMessage(Translation.component(l, "moderation.ban.expired.msg").color(NamedTextColor.GREEN));
        } else {
            event.setResult(ResultedEvent.ComponentResult.denied(Translation.component(l, "moderation.ban.join.title").color(NamedTextColor.RED)
                    .appendNewline().appendNewline()
                    .append(Translation.component(l, "moderation.expiration", "").color(NamedTextColor.GRAY)
                            .append((Boolean) ban.get("permanent") ? Translation.component(l, "moderation.time.permanent").color(NamedTextColor.RED) :
                                    Component.text(Time.preciselyFormat((Long) ban.get("expires") - Instant.now().getEpochSecond()), NamedTextColor.BLUE)))
                    .appendNewline()
                    .append(Translation.component(l, "moderation.reason", "").color(NamedTextColor.GRAY).append(Component.text((String) ban.get("reason"), NamedTextColor.BLUE)))
            ));
        }
    }

    @Contract(" -> new")
    public static @NotNull BrigadierCommand banCommand() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("ban")
                .requires(source -> source.hasPermission("poo.ban"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            List<UUID> excluded = new ArrayList<>(Banning.STORAGE.getAll().stream().map(m -> (UUID) m.get("player")).toList());
                            if (context.getSource() instanceof Player player)
                                excluded.add(player.getUniqueId());

                            PlayerCache.PLAYERS.entrySet().parallelStream()
                                    .filter(p -> !excluded.contains(p.getKey()))
                                    .map(Map.Entry::getValue)
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("time", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    String input = context.getArguments().size() == 2 ? builder.getInput().split(" ")[0] : "";
                                    Banning.TIME_UNITS.forEach(unit -> builder.suggest(input.replaceAll("[^-\\d.]+", "") + unit));
                                    builder.suggest("permanent");
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
                                                boolean permanent = context.getArgument("time", String.class).equalsIgnoreCase("permanent");
                                                Time time = permanent ? new Time(0) : Time.parse(context.getArgument("time", String.class));
                                                String reason = context.getArgument("reason", String.class);

                                                try {
                                                    if (target.isPresent()) {
                                                        Banning.ban(source instanceof Player player ? player.getUsername() : "Console",
                                                                new VelocityPlayer(target.get()), permanent, time, reason);
                                                    } else {
                                                        Banning.ban(source instanceof Player player ? player.getUsername() : "Console",
                                                                new OfflinePlayer(targetArg, targetUuid), permanent, time, reason);
                                                    }
                                                    source.sendMessage(Translation.component(l, "moderation.ban.confirm", targetArg, permanent ? Translation.string(l, "moderation.time.permanent") : time.getPreciselyFormatted(), reason).color(NamedTextColor.YELLOW));
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
    public static @NotNull BrigadierCommand pardonCommand() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("pardon")
                .requires(source -> source.hasPermission("poo.ban"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Banning.STORAGE.getAll().parallelStream()
                                    .map(m -> (UUID) m.get("player"))
                                    .map(PlayerCache.PLAYERS::get)
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
                                    Banning.pardon(source instanceof Player player ? player.getUsername() : "Console", new OfflinePlayer(targetArg, PlayerCache.getUuid(targetArg)));
                                    source.sendMessage(Translation.component(l, "moderation.pardon.confirm", targetArg).color(NamedTextColor.YELLOW));
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

package com.marcpg.peelocity.moderation;

import com.marcpg.data.time.Time;
import com.marcpg.lang.Translation;
import com.marcpg.peelocity.Configuration;
import com.marcpg.peelocity.Peelocity;
import com.marcpg.peelocity.PlayerCache;
import com.marcpg.peelocity.storage.Storage;
import com.marcpg.web.discord.Embed;
import com.marcpg.web.discord.Webhook;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class Muting {
    private static final List<String> TIME_TYPES = List.of("sec", "min", "h", "d", "wk", "mo");
    private static final Storage<UUID> STORAGE = Configuration.storageType.createStorage("mutes", "player");
    private static final Time MAX_TIME = new Time(1, Time.Unit.YEARS);

    @Subscribe(order = PostOrder.FIRST)
    public void onLogin(@NotNull LoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Locale l = event.getPlayer().getEffectiveLocale();

        if (!STORAGE.contains(uuid)) return;

        Map<String, Object> mute = STORAGE.get(uuid);

        if ((Boolean) mute.get("permanent") && (System.currentTimeMillis() * 0.001) > (Long) mute.get("expires")) {
            STORAGE.remove(uuid);
            player.sendMessage(Translation.component(l, "moderation.ban.expired.msg").color(NamedTextColor.GREEN));
        } else
            event.setResult(ResultedEvent.ComponentResult.denied(Translation.component(l, "moderation.ban.join.title").color(NamedTextColor.RED)
                    .appendNewline().appendNewline()
                    .append(Translation.component(l, "moderation.expiration", "").color(NamedTextColor.GRAY)
                            .append((Boolean) mute.get("permanent") ? Translation.component(l, "moderation.time.permanent").color(NamedTextColor.RED) :
                                    Component.text(Time.preciselyFormat((Long) mute.get("expires") - Instant.now().getEpochSecond()), NamedTextColor.BLUE)))
                    .appendNewline()
                    .append(Translation.component(l, "moderation.reason", "").color(NamedTextColor.GRAY).append(Component.text((String) mute.get("reason"), NamedTextColor.BLUE)))));
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChat(@NotNull PlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Locale l = event.getPlayer().getEffectiveLocale();

        if (!STORAGE.contains(uuid)) return;

        Map<String, Object> mute = STORAGE.get(uuid);

        if ((System.currentTimeMillis() * 0.001) > (Long) mute.get("expires")) {
            STORAGE.remove(uuid);
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
                .requires(source -> source.hasPermission("pee.mute"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            PlayerCache.PLAYERS.values().forEach(builder::suggest);
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
                                            Locale l = source instanceof Player player ? player.getEffectiveLocale() : new Locale("en", "US");
                                            String targetArg = context.getArgument("player", String.class);
                                            UUID targetUuid = PlayerCache.getUuid(targetArg);

                                            if (targetUuid == null) {
                                                source.sendMessage(Translation.component(l, "cmd.player_not_found", targetArg).color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            Time time = Time.parse(context.getArgument("time", String.class));
                                            if (time.get() <= 0) {
                                                source.sendMessage(Translation.component(l, "moderation.time.invalid", time.getPreciselyFormatted()));
                                                return 1;
                                            } else if (time.get() > MAX_TIME.get()) {
                                                source.sendMessage(Translation.component(l, "moderation.time.limit", time.getPreciselyFormatted(), MAX_TIME.getOneUnitFormatted()));
                                                return 1;
                                            }

                                            String reason = context.getArgument("reason", String.class);

                                            if (STORAGE.contains(targetUuid)) {
                                                source.sendMessage(Translation.component(l, "moderation.mute.already_muted", targetArg).color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            // Actually add the mute to the mutes storage.
                                            STORAGE.add(Map.of("player", targetUuid,
                                                    "expires", Instant.now().plusSeconds(time.get()).getEpochSecond(),
                                                    "duration", time.get(),
                                                    "reason", reason));

                                            // Send message to the player, if he's still online.
                                            Peelocity.SERVER.getPlayer(targetUuid).ifPresent(t -> {
                                                Locale tl = t.getEffectiveLocale();
                                                t.sendMessage(Translation.component(tl, "moderation.mute.msg.title").color(NamedTextColor.RED)
                                                        .appendNewline()
                                                        .append(Translation.component(tl, "moderation.expiration", "").color(NamedTextColor.GRAY).append(Component.text(time.getOneUnitFormatted(), NamedTextColor.BLUE)))
                                                        .appendNewline()
                                                        .append(Translation.component(tl, "moderation.reason", "").color(NamedTextColor.GRAY).append(Component.text(reason, NamedTextColor.BLUE))));
                                            });

                                            // Send confirmation to the command source.
                                            source.sendMessage(Translation.component(l, "moderation.mute.confirm", targetArg, time.getPreciselyFormatted(), reason).color(NamedTextColor.YELLOW));

                                            // Log the mute into the console, if it wasn't done by the console itself.
                                            if (source instanceof Player player)
                                                Peelocity.LOG.info(player.getUsername() + " muted " + targetArg + " for " + time.getPreciselyFormatted() + " with the reason: \"" + reason + "\"");

                                            // Send an embed to the moderator webhook to log the mute there too.
                                            try {
                                                if (Configuration.modWebhook != null)
                                                    Configuration.modWebhook.post(new Embed("Minecraft Mute", targetArg + " got muted by " + (source instanceof Player player ? player.getUsername() : " the Console") + ".", Color.YELLOW, List.of(
                                                            new Embed.Field("Muted", targetArg, true),
                                                            new Embed.Field("Moderator", source instanceof Player player ? player.getUsername() : "Console", true),
                                                            new Embed.Field("Time", time.getPreciselyFormatted(), true),
                                                            new Embed.Field("Reason", Webhook.escapeJson(reason).trim(), false)
                                                    )));
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
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
                .requires(source -> source.hasPermission("pee.mute"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            STORAGE.get(m -> true).forEach(m -> builder.suggest(PlayerCache.PLAYERS.get((UUID) m.get("player"))));
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            Locale l = source instanceof Player player ? player.getEffectiveLocale() : new Locale("en", "US");
                            String targetArg = context.getArgument("player", String.class);
                            UUID targetUuid = PlayerCache.getUuid(targetArg);

                            if (targetUuid == null) {
                                source.sendMessage(Translation.component(l, "cmd.player_not_found", targetArg).color(NamedTextColor.RED));
                                return 1;
                            }

                            if (!STORAGE.contains(targetUuid)) {
                                source.sendMessage(Translation.component(l, "moderation.unmute.not_muted", targetArg).color(NamedTextColor.RED));
                                return 1;
                            }

                            STORAGE.remove(targetUuid);

                            source.sendMessage(Translation.component(l, "moderation.unmute.confirm", targetArg).color(NamedTextColor.YELLOW));
                            if (source instanceof Player player)
                                Peelocity.LOG.info(player.getUsername() + " unmuted " + targetArg);

                            try {
                                if (Configuration.modWebhook != null)
                                    Configuration.modWebhook.post(new Embed("Minecraft **Un**mute", targetArg + "'s mute got removed by " + (source instanceof Player player ? player.getUsername() : " the Console") + ".", Color.YELLOW, List.of(
                                            new Embed.Field("Unmuted", targetArg, true),
                                            new Embed.Field("Moderator", source instanceof Player player ? player.getUsername() : "Console", true)
                                    )));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            return 1;
                        })
                )
                .build()
        );
    }
}

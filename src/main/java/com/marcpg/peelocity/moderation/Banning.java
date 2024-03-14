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

public final class Banning {
    private static final List<String> TIME_TYPES = List.of("min", "h", "d", "wk", "mo", "yr");
    private static final Storage<UUID> STORAGE = Configuration.storageType.createStorage("bans", "player");
    private static final Time MAX_TIME = new Time(5, Time.Unit.YEARS);

    @Subscribe(order = PostOrder.FIRST)
    public void onLogin(@NotNull LoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Locale l = event.getPlayer().getEffectiveLocale();

        if (!STORAGE.contains(uuid)) return;

        Map<String, Object> ban = STORAGE.get(uuid);

        if (player.hasPermission("pee.ban") || player.hasPermission("pee.admin")) {
            STORAGE.remove(uuid);
        } else if ((Boolean) ban.get("permanent") && (System.currentTimeMillis() * 0.001) > (Long) ban.get("expires")) {
            STORAGE.remove(uuid);
            player.sendMessage(Translation.component(l, "moderation.ban.expired.msg").color(NamedTextColor.GREEN));
        } else
            event.setResult(ResultedEvent.ComponentResult.denied(Translation.component(l, "moderation.ban.join.title").color(NamedTextColor.RED)
                    .appendNewline().appendNewline()
                    .append(Translation.component(l, "moderation.expiration", "").color(NamedTextColor.GRAY)
                            .append((Boolean) ban.get("permanent") ? Translation.component(l, "moderation.time.permanent").color(NamedTextColor.RED) :
                                    Component.text(Time.preciselyFormat((Long) ban.get("expires") - Instant.now().getEpochSecond()), NamedTextColor.BLUE)))
                    .appendNewline()
                    .append(Translation.component(l, "moderation.reason", "").color(NamedTextColor.GRAY).append(Component.text((String) ban.get("reason"), NamedTextColor.BLUE)))));
    }

    @Contract(" -> new")
    public static @NotNull BrigadierCommand banCommand() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("ban")
                .requires(source -> source.hasPermission("pee.ban"))
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
                                                return 1;
                                            }

                                            String timeArg = context.getArgument("time", String.class);
                                            boolean permanent = timeArg.equalsIgnoreCase("permanent");
                                            Time time = permanent ? new Time(0) : Time.parse(timeArg);

                                            if (!permanent && time.get() <= 0) {
                                                source.sendMessage(Translation.component(l, "moderation.time.invalid", time.getPreciselyFormatted()));
                                                return 1;
                                            } else if (time.get() > MAX_TIME.get()) {
                                                source.sendMessage(Translation.component(l, "moderation.time.limit", time.getPreciselyFormatted(), MAX_TIME.getOneUnitFormatted()));
                                                return 1;
                                            }

                                            String reason = context.getArgument("reason", String.class);

                                            if (STORAGE.contains(targetUuid)) {
                                                source.sendMessage(Translation.component(l, "moderation.ban.already_banned", targetArg).color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            // Actually add the ban to the bans storage.
                                            STORAGE.add(Map.of("player", targetUuid,
                                                    "permanent", permanent,
                                                    "expires", Instant.now().plusSeconds(time.get()).getEpochSecond(),
                                                    "duration", time.get(),
                                                    "reason", reason));

                                            // Kick the player, if he's still online.
                                            Peelocity.SERVER.getPlayer(targetUuid).ifPresent(t -> {
                                                Locale tl = t.getEffectiveLocale();
                                                t.disconnect(Translation.component(tl, "moderation.ban.msg.title").color(NamedTextColor.RED)
                                                        .appendNewline().appendNewline()
                                                        .append(Translation.component(tl, "moderation.expiration", "").color(NamedTextColor.GRAY).append(permanent ? Translation.component(tl, "moderation.time.permanent").color(NamedTextColor.RED) : net.kyori.adventure.text.Component.text(time.getOneUnitFormatted(), NamedTextColor.BLUE)))
                                                        .appendNewline()
                                                        .append(Translation.component(tl, "moderation.reason", "").color(NamedTextColor.GRAY).append(Component.text(reason, NamedTextColor.BLUE))));
                                            });

                                            // Send confirmation to the command source.
                                            source.sendMessage(Translation.component(l, "moderation.ban.confirm", targetArg,
                                                    permanent ? Translation.string(l, "moderation.time.permanent") : time.getPreciselyFormatted(),
                                                    reason).color(NamedTextColor.YELLOW));

                                            // Log the ban into the console, if it wasn't done by the console itself.
                                            if (source instanceof Player player)
                                                Peelocity.LOG.info(player.getUsername() + " banned " + targetArg + (permanent ? " permanently" : " for " + time.getPreciselyFormatted()) + " with the reason: \"" + reason + "\"");

                                            // Send an embed to the moderator webhook to log the ban there too.
                                            try {
                                                if (Configuration.modWebhook != null)
                                                    Configuration.modWebhook.post(new Embed("Minecraft Ban", targetArg + " got banned by " + (source instanceof Player player ? player.getUsername() : " the Console") + ".", Color.ORANGE, List.of(
                                                            new Embed.Field("Banned", targetArg, true),
                                                            new Embed.Field("Moderator", source instanceof Player player ? player.getUsername() : "Console", true),
                                                            new Embed.Field("Time", permanent ? "Permanent" : time.getPreciselyFormatted(), true),
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
    public static @NotNull BrigadierCommand pardonCommand() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("pardon")
                .requires(source -> source.hasPermission("pee.ban"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            STORAGE.getAll().parallelStream()
                                    .map(m -> (UUID) m.get("player"))
                                    .map(PlayerCache.PLAYERS::get)
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            Locale l = source instanceof Player player ? player.getEffectiveLocale() : Locale.getDefault();
                            String targetArg = context.getArgument("player", String.class);
                            UUID targetUuid = PlayerCache.getUuid(targetArg);

                            if (targetUuid == null) {
                                source.sendMessage(Translation.component(l, "cmd.player_not_found", targetArg).color(NamedTextColor.RED));
                                return 1;
                            }

                            if (!STORAGE.contains(targetUuid)) {
                                source.sendMessage(Translation.component(l, "moderation.pardon.not_banned", targetArg).color(NamedTextColor.RED));
                                return 1;
                            }

                            STORAGE.remove(targetUuid);

                            source.sendMessage(Translation.component(l, "moderation.pardon.confirm", targetArg).color(NamedTextColor.YELLOW));
                            if (source instanceof Player player)
                                Peelocity.LOG.info(player.getUsername() + " pardoned/unbanned " + targetArg);

                            try {
                                if (Configuration.modWebhook != null)
                                    Configuration.modWebhook.post(new Embed("Minecraft Pardon", targetArg + "got pardoned/unbanned by " + (source instanceof Player player ? player.getUsername() : " the Console") + ".", Color.YELLOW, List.of(
                                            new Embed.Field("Pardoned", targetArg, true),
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

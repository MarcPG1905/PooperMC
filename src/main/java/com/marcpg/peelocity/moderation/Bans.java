package com.marcpg.peelocity.moderation;

import com.marcpg.data.time.Time;
import com.marcpg.lang.Translation;
import com.marcpg.peelocity.Config;
import com.marcpg.peelocity.Peelocity;
import com.marcpg.peelocity.PlayerCache;
import com.marcpg.peelocity.storage.Storage;
import com.marcpg.web.discord.Embed;
import com.marcpg.web.discord.Webhook;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class Bans {
    public static final List<String> TIME_TYPES = List.of("min", "h", "d", "wk", "mo", "yr");
    public static final Storage<UUID> STORAGE = Config.STORAGE_TYPE.getStorage("bans", "uuid");
    private static final Time MAX_TIME = new Time(5, Time.Unit.YEARS);

    public static @NotNull BrigadierCommand createBanBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("ban")
                .requires(source -> source.hasPermission("pee.ban"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Peelocity.SERVER.getAllPlayers().stream()
                                    .filter(player -> !player.hasPermission("pee.ban") && player != context.getSource())
                                    .map(Player::getUsername)
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
                                            Player source = (Player) context.getSource();
                                            Locale l = source.getEffectiveLocale();
                                            Peelocity.SERVER.getPlayer(context.getArgument("player", String.class)).ifPresentOrElse(
                                                    target -> {
                                                        if (target.hasPermission("pee.ban") && !source.hasPermission("pee.op")) {
                                                            source.sendMessage(Translation.component(l, "moderation.ban.cant").color(NamedTextColor.RED));
                                                            return;
                                                        }

                                                        boolean permanent = context.getArgument("time", String.class).contains("permanent");
                                                        Time time = permanent ? new Time(0) : Time.parse(context.getArgument("time", String.class));
                                                        if (time.get() < 0) {
                                                            source.sendMessage(Translation.component(l, "moderation.time.invalid", time.getPreciselyFormatted()));
                                                            return;
                                                        } else if (time.get() > Time.Unit.DECADES.sec) {
                                                            source.sendMessage(Translation.component(l, "moderation.time.limit", time.getPreciselyFormatted(), MAX_TIME.getOneUnitFormatted()));
                                                            return;
                                                        }

                                                        String reason = context.getArgument("reason", String.class);

                                                        Locale tl = target.getEffectiveLocale();
                                                        target.disconnect(Translation.component(tl, "moderation.ban.msg.title").color(NamedTextColor.RED)
                                                                .appendNewline().appendNewline()
                                                                .append(Translation.component(tl, "moderation.expiration", "").color(NamedTextColor.GRAY).append(permanent ? Translation.component(tl, "moderation.time.permanent").color(NamedTextColor.RED) : Component.text(time.getOneUnitFormatted(), NamedTextColor.BLUE)))
                                                                .appendNewline()
                                                                .append(Translation.component(tl, "moderation.reason", "").color(NamedTextColor.GRAY).append(Component.text(reason, NamedTextColor.BLUE))));

                                                        if (!STORAGE.contains(target.getUniqueId())) {
                                                            STORAGE.add(new UserUtil.Punishment(target.getUniqueId(), permanent, Instant.now().plusSeconds(time.get()), time, reason).toMap());
                                                            source.sendMessage(Translation.component(tl, "moderation.ban.confirm", target.getUsername(), permanent ? Translation.string(tl, "moderation.time.permanent") : time.getPreciselyFormatted(), reason).color(NamedTextColor.YELLOW));
                                                            Peelocity.LOG.info(source.getUsername() + " banned " + target.getUsername() + " for " + time.getPreciselyFormatted() + " with the reason: \"" + reason + "\"");

                                                            try {
                                                                if (Config.MODERATOR_WEBHOOK_ENABLED)
                                                                    Config.MODERATOR_WEBHOOK.post(new Embed("Minecraft Ban", target.getUsername() + " got banned by " + source.getUsername(), Color.ORANGE, List.of(
                                                                            new Embed.Field("Banned", target.getUsername(), true),
                                                                            new Embed.Field("Moderator", source.getUsername(), true),
                                                                            new Embed.Field("Time", permanent ? "Permanent" : time.getPreciselyFormatted(), true),
                                                                            new Embed.Field("Reason", Webhook.escapeJson(reason).trim(), false)
                                                                    )));
                                                            } catch (IOException e) {
                                                                throw new RuntimeException(e);
                                                            }
                                                        } else {
                                                            source.sendMessage(Translation.component(l, "moderation.ban.already_banned", context.getArgument("player", String.class)).color(NamedTextColor.RED));
                                                        }
                                                    },
                                                    () -> source.sendMessage(Translation.component(l, "cmd.player_not_found", context.getArgument("player", String.class)).color(NamedTextColor.RED))
                                            );
                                            return 1;
                                        })
                                )
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .executes(context -> {
                            context.getSource().sendMessage(Component.text("""
                                    §l§nHelp:§r §l/ban§r
                                    The command /ban acts as a utility to ban players from the whole server with additional features, such as expirations.
                                    
                                    §l§nArguments:§r
                                    -§l player§r: The player to ban off the server.
                                    -§l time§r: How long the player should remain banned. One time unit only. A value of 'permanent' will act as the time set to 0, which will never expire.
                                    -§l reason§r: A good reason for banning the player.
                                    
                                    §l§nAdditional Info:§r
                                    - You can currently only ban players who are online.
                                    """));
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }

    public static @NotNull BrigadierCommand createPardonBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("pardon")
                .requires(source -> source.hasPermission("pee.ban"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            PlayerCache.CACHED_USERS.entrySet().stream()
                                    .filter(entry -> STORAGE.contains(entry.getKey()))
                                    .map(Map.Entry::getValue)
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            Player source = (Player) context.getSource();
                            String target = context.getArgument("player", String.class);
                            if (STORAGE.contains(PlayerCache.getUuid(target))) {
                                STORAGE.remove(PlayerCache.getUuid(target));
                                source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.pardon.confirm", target).color(NamedTextColor.YELLOW));
                                Peelocity.LOG.info(source.getUsername() + " pardoned/unbanned " + target);
                            } else {
                                source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.pardon.not_banned", target).color(NamedTextColor.RED));
                            }
                            return 1;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .executes(context -> {
                            context.getSource().sendMessage(Component.text("""
                                    §l§nHelp:§r §l/pardon§r
                                    The command /pardon acts as a utility to pardon/unban players.
                                    
                                    §l§nArguments:§r
                                    -§l player§r: The player to pardon from the server.
                                    """));
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onLogin(@NotNull LoginEvent event) {
        Player player = event.getPlayer();
        if (STORAGE.contains(player.getUniqueId())) {
            UserUtil.Punishment punishment = UserUtil.Punishment.ofMap(STORAGE.get(player.getUniqueId()));
            if (punishment.isExpired()) {
                STORAGE.remove(player.getUniqueId());
                player.sendMessage(Translation.component(player.getEffectiveLocale(), "moderation.ban.expired.msg").color(NamedTextColor.GREEN));
            } else {
                Locale l = event.getPlayer().getEffectiveLocale();
                event.setResult(ResultedEvent.ComponentResult.denied(Translation.component(l, "moderation.ban.join.title").color(NamedTextColor.RED)
                        .appendNewline().appendNewline()
                        .append(Translation.component(l, "moderation.expiration", "").color(NamedTextColor.GRAY)
                                .append(punishment.permanent() ? Translation.component(l, "moderation.time.permanent").color(NamedTextColor.RED) :
                                        Component.text(Time.preciselyFormat(punishment.expires().getEpochSecond() - Instant.now().getEpochSecond()), NamedTextColor.BLUE)))
                        .appendNewline()
                        .append(Translation.component(l, "moderation.reason", "").color(NamedTextColor.GRAY).append(Component.text(punishment.reason(), NamedTextColor.BLUE)))));
            }
        }
    }
}

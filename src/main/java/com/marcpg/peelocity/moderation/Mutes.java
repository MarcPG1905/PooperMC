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
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class Mutes {
    private static final List<String> TIME_TYPES = List.of("min", "h", "d", "wk", "mo");
    private static final Storage<UUID> STORAGE = Config.STORAGE_TYPE.getStorage("mutes", "uuid");
    private static final Time MAX_TIME = new Time(1, Time.Unit.YEARS);

    public static @NotNull BrigadierCommand createMuteBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("mute")
                .requires(source -> source.hasPermission("pee.mute"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Peelocity.SERVER.getAllPlayers().parallelStream()
                                    .filter(player -> !player.hasPermission("pee.mute") && player != context.getSource())
                                    .map(Player::getUsername)
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
                                            Player source = (Player) context.getSource();
                                            Locale l = source.getEffectiveLocale();
                                            Peelocity.SERVER.getPlayer(context.getArgument("player", String.class)).ifPresentOrElse(
                                                    target -> {
                                                        if (target.hasPermission("pee.mute") && !source.hasPermission("pee.op")) {
                                                            source.sendMessage(Translation.component(l, "moderation.mute.cant").color(NamedTextColor.RED));
                                                            return;
                                                        }

                                                        Time time = Time.parse(context.getArgument("time", String.class));
                                                        if (time.get() <= 0) {
                                                            source.sendMessage(Translation.component(l, "moderation.time.invalid", time.getPreciselyFormatted()));
                                                            return;
                                                        } else if (time.get() > Time.Unit.DECADES.sec) {
                                                            source.sendMessage(Translation.component(l, "moderation.time.limit", time.getPreciselyFormatted(), MAX_TIME.getOneUnitFormatted()));
                                                            return;
                                                        }

                                                        String reason = context.getArgument("reason", String.class);

                                                        Locale tl = target.getEffectiveLocale();
                                                        target.sendMessage(Translation.component(tl, "moderation.mute.msg.title").color(NamedTextColor.RED)
                                                                .appendNewline()
                                                                .append(Translation.component(tl, "moderation.expiration", "").color(NamedTextColor.GRAY).append(Component.text(time.getOneUnitFormatted(), NamedTextColor.BLUE))
                                                                .appendNewline()
                                                                .append(Translation.component(tl, "moderation.reason", "").color(NamedTextColor.GRAY).append(Component.text(reason, NamedTextColor.BLUE)))));

                                                        if (!STORAGE.contains(target.getUniqueId())) {
                                                            STORAGE.add(new UserUtil.Punishment(target.getUniqueId(), false, Instant.now().plusSeconds(time.get()), time, reason).toMap());
                                                            source.sendMessage(Translation.component(l, "moderation.mute.confirm", target.getUsername(), time.getPreciselyFormatted(), reason).color(NamedTextColor.YELLOW));
                                                            Peelocity.LOG.info(source.getUsername() + " muted " + target.getUsername() + " for " + time.getPreciselyFormatted() + " with the reason: \"" + reason + "\"");
                                                            try {
                                                                if (Config.MODERATOR_WEBHOOK_ENABLED)
                                                                    Config.MODERATOR_WEBHOOK.post(new Embed("Minecraft Mute", target.getUsername() + " got muted by " + source.getUsername(), Color.YELLOW, List.of(
                                                                            new Embed.Field("Muted", target.getUsername(), true),
                                                                            new Embed.Field("Moderator", source.getUsername(), true),
                                                                            new Embed.Field("Time", time.getPreciselyFormatted(), true),
                                                                            new Embed.Field("Reason", Webhook.escapeJson(reason).trim(), false)
                                                                    )));
                                                            } catch (IOException e) {
                                                                throw new RuntimeException(e);
                                                            }
                                                        } else {
                                                            source.sendMessage(Translation.component(l, "moderation.mute.already_muted", target.getUsername()).color(NamedTextColor.RED));
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
                                    §l§nHelp:§r §l/mute§r
                                    The command /mute acts as a utility to prevent players from using the chat for a specified while.
                                    
                                    §l§nArguments:§r
                                    -§l player§r: The player to mute on the server.
                                    -§l time§r: How long the player should remain muted. One time unit only.
                                    -§l reason§r: A good reason for muting the player.
                                    
                                    §l§nAdditional Info:§r
                                    - You can currently only mute players who are online.
                                    """));
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }

    public static @NotNull BrigadierCommand createUnmuteBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("unmute")
                .requires(source -> source.hasPermission("pee.mute"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Peelocity.SERVER.getAllPlayers().stream()
                                    .filter(player -> !player.hasPermission("pee.mute") && player != context.getSource())
                                    .map(Player::getUsername)
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            Player source = (Player) context.getSource();
                            String target = context.getArgument("player", String.class);
                            if (STORAGE.contains(PlayerCache.getUuid(target))) {
                                STORAGE.remove(PlayerCache.getUuid(target));
                                source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.unmute.confirm", target).color(NamedTextColor.YELLOW));
                                Peelocity.LOG.info(source.getUsername() + " unmuted " + target);
                            } else {
                                source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.unmute.not_muted", target).color(NamedTextColor.RED));
                            }
                            return 1;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .executes(context -> {
                            context.getSource().sendMessage(Component.text("""
                                    §l§nHelp:§r §l/unmute§r
                                    The command /unmute acts as a utility to remove a player's mute.
                                    
                                    §l§nArguments:§r
                                    -§l player§r: The player to unmute on the server.
                                    """));
                            return 1;
                        })
                )
                .build();
        return new BrigadierCommand(node);
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChat(@NotNull PlayerChatEvent event) {
        Player player = event.getPlayer();
        if (STORAGE.contains(player.getUniqueId())) {
            Locale l = player.getEffectiveLocale();
            UserUtil.Punishment punishment = UserUtil.Punishment.ofMap(STORAGE.get(player.getUniqueId()));
            if (punishment.isExpired()) {
                STORAGE.remove(player.getUniqueId());
                player.sendMessage(Translation.component(l, "moderation.mute.expired.msg").color(NamedTextColor.RED));
            } else {
                event.setResult(PlayerChatEvent.ChatResult.denied());
                player.sendMessage(Translation.component(l, "moderation.mute.warning").color(NamedTextColor.RED));
                player.sendMessage(Translation.component(l, "moderation.expiration", Time.preciselyFormat(punishment.expires().getEpochSecond() - Instant.now().getEpochSecond())).color(NamedTextColor.GOLD));
                player.sendMessage(Translation.component(l, "moderation.reason", punishment.reason()).color(NamedTextColor.GOLD));
            }
        }
    }
}

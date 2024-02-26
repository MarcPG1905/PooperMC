package com.marcpg.peelocity_old.moderation;

import com.marcpg.data.time.Time;
import com.marcpg.lang.Translation;
import com.marcpg.peelocity_old.Peelocity;
import com.marcpg.peelocity_old.PlayerCache;
import com.marcpg.peelocity_old.chat.MessageLogging;
import com.marcpg.peelocity_old.chat.MessageLogging.MessageData.Type;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class UserUtil {
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MMMM d, HH:mm");

    public static @NotNull BrigadierCommand createMessageHistoryBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("message-history")
                .requires(source -> source.hasPermission("pee.msg-hist"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            PlayerCache.CACHED_USERS.values().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            Player source = (Player) context.getSource();
                            Locale l = source.getEffectiveLocale();

                            String name = context.getArgument("player", String.class);
                            UUID uuid = PlayerCache.getUuid(name);
                            if (uuid != null) {
                                if (MessageLogging.hasHistory(uuid)) {
                                    source.sendMessage(Translation.component(l, "moderation.chat_history.title", name).color(NamedTextColor.DARK_GREEN));
                                    MessageLogging.getHistory(uuid).forEach(messageData -> {
                                        String time = "[" + FORMATTER.format(LocalDateTime.ofInstant(messageData.time().toInstant(), ZoneId.systemDefault())) + " UTC]";
                                        String additional = messageData.type() == Type.NORMAL ? "" : (messageData.type() == Type.PRIVATE ? Translation.string(l, "moderation.chat_history.private", messageData.receiver()) : (messageData.type() == Type.PARTY ? Translation.string(l, "moderation.chat_history.party") : Translation.string(l, "moderation.chat_history.staff")));
                                        source.sendMessage(Component.text("| " + time + (additional.isEmpty() ? "" : " " + additional) + " ", NamedTextColor.GRAY).append(Component.text(messageData.content().strip(), NamedTextColor.WHITE)));
                                    });
                                    source.sendMessage(Component.text("=========================").color(NamedTextColor.DARK_GREEN));
                                    Peelocity.LOG.info(source.getUsername() + " retrieved " + name + "'s message history");
                                } else {
                                    source.sendMessage(Translation.component(l, "moderation.chat_history.no_history", name).color(NamedTextColor.RED));
                                }
                            } else {
                                source.sendMessage(Translation.component(l, "cmd.player_not_found", name).color(NamedTextColor.RED));
                            }
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }

    public record Punishment(UUID player, boolean permanent, Instant expires, Time duration, String reason) {
        public boolean isExpired() {
            return !permanent && Instant.now().isAfter(expires);
        }

        @Contract(" -> new")
        public @NotNull @Unmodifiable Map<String, Object> toMap() {
            return Map.of(
                    "player", player,
                    "permanent", permanent,
                    "expires", expires.getEpochSecond(),
                    "duration", duration.get(),
                    "reason", reason
            );
        }

        @Contract("_ -> new")
        public static @NotNull Punishment ofMap(@NotNull Map<String, Object> map) {
            return new Punishment(UUID.fromString((String) map.get("player")), (boolean) map.get("permanent"), Instant.ofEpochSecond((Long) map.get("expires")), new Time((Long) map.get("duration")), (String) map.get("reason"));
        }
    }
}

package com.marcpg.peelocity.moderation;

import com.marcpg.lang.Translation;
import com.marcpg.peelocity.Peelocity;
import com.marcpg.peelocity.PlayerCache;
import com.marcpg.peelocity.chat.MessageLogging;
import com.marcpg.peelocity.chat.MessageLogging.MessageData.Type;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
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
}

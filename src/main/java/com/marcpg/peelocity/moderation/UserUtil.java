package com.marcpg.peelocity.moderation;

import com.marcpg.peelocity.Peelocity;
import com.marcpg.peelocity.UserCache;
import com.marcpg.peelocity.chat.MessageLogging;
import com.marcpg.peelocity.chat.MessageLogging.MessageData.Type;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.hectus.lang.Translation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

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
                            UserCache.CACHED_USERS.values().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            Player source = (Player) context.getSource();
                            Locale l = source.getEffectiveLocale();

                            String name = context.getArgument("player", String.class);
                            UUID uuid = UserCache.getUuid(name);
                            if (uuid != null) {
                                source.sendMessage(Translation.component(l, "moderation.chat_history.title", name).color(NamedTextColor.DARK_GREEN));
                                MessageLogging.getHistory(uuid).forEach(messageData -> {
                                    String time = "[" + FORMATTER.format(messageData.time().toInstant()) + "] ";
                                    String additional = messageData.type() == Type.NORMAL ? "" : (messageData.type() == Type.PRIVATE ? Translation.string(l, "moderation.chat_history.private") : (messageData.type() == Type.PARTY ? Translation.string(l, "moderation.chat_history.party") : Translation.string(l, "moderation.chat_history.staff")));
                                    source.sendMessage(Component.text("| " + time + additional + messageData.content()));
                                });
                                source.sendMessage(Component.text("=========================").color(NamedTextColor.DARK_GREEN));
                                Peelocity.LOG.info(source.getUsername() + " retrieved " + name + "'s message history");
                            } else {
                                source.sendMessage(Translation.component(l, "cmd.player_not_found", name).color(NamedTextColor.RED));
                            }
                            return 1;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .executes(context -> {
                            context.getSource().sendMessage(Component.text("""
                                    §l§nHelp:§r §l/message-history§r
                                    The command /message-history will give you the last messages of a user. Logs up to 50 messages.
                                    
                                    §l§nArguments:§r
                                    - §lplayer§r: What user to retrieve the message history from.
                                    """));
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }
}

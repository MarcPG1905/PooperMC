package com.marcpg.peelocity.features;

import com.marcpg.lang.Translation;
import com.marcpg.peelocity.Peelocity;
import com.marcpg.peelocity.PlayerCache;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class MessageHistory {
    public record MessageData(Date time, String content, Type type, @Nullable String receiver) {
        public enum Type { NORMAL, STAFF, PRIVATE, PARTY }

        @Contract("_ -> new")
        public static @NotNull MessageData parse(@NotNull String line) {
            String[] elements = line.split(" \\|\\| ");
            Type type = Type.valueOf(elements[2]);
            try {
                return new MessageData(DATE_FORMAT.parse(elements[0]), elements[1], type, (type == Type.PRIVATE ? elements[3] : null));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static boolean enabled;
    public static int maxHistory;
    public static Path path;
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Subscribe(order = PostOrder.LAST)
    public void onPlayerChat(@NotNull PlayerChatEvent event) {
        if (event.getResult().isAllowed() && enabled) saveMessage(event.getPlayer(), new MessageData(new Date(), event.getMessage().replace(" || ", " \\==|==\\==|== "), MessageData.Type.NORMAL, null));
    }

    public static void saveMessage(@NotNull Player player, @NotNull MessageData data) {
        if (!enabled) return;

        Path filePath = path.resolve(player.getUniqueId().toString());
        try {
            if (!Files.exists(filePath)) Files.createFile(filePath);

            List<String> lines = Files.readAllLines(filePath);
            lines.add(DATE_FORMAT.format(data.time) + " ||  " + data.content + " || " + data.type + (data.receiver == null ? "" : " || " + data.receiver));
            if (lines.size() > maxHistory)
                lines.subList(0, lines.size() - maxHistory).clear();

            Files.write(filePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            Peelocity.LOG.error("Error while saving history of " + player.getUsername() + ": " + e.getMessage());
        }
    }

    public static List<MessageData> getHistory(@NotNull UUID uuid) {
        try {
            return Files.readAllLines(path.resolve(uuid.toString())).stream().map(MessageData::parse).toList();
        } catch (IOException e) {
            Peelocity.LOG.error("Error while getting/loading history of user " + uuid + ": " + e.getMessage());
        }
        return List.of();
    }

    public static boolean hasHistory(@NotNull UUID uuid) {
        return Files.exists(path.resolve(uuid.toString()));
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MMMM d, HH:mm");

    @Contract(" -> new")
    public static @NotNull BrigadierCommand command() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("msg-hist")
                .requires(source -> source.hasPermission("pee.msg-hist"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            PlayerCache.PLAYERS.values().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            Player player = (Player) context.getSource();
                            Locale l = player.getEffectiveLocale();

                            String target = context.getArgument("player", String.class);
                            UUID uuid = PlayerCache.getUuid(target);

                            if (uuid == null) {
                                player.sendMessage(Translation.component(l, "cmd.player_not_found", target).color(NamedTextColor.RED));
                                return 1;
                            }
                            if (!MessageHistory.hasHistory(uuid)) {
                                player.sendMessage(Translation.component(l, "moderation.chat_history.no_history", target).color(NamedTextColor.RED));
                                return 1;
                            }

                            player.sendMessage(Translation.component(l, "moderation.chat_history.title", target).color(NamedTextColor.DARK_GREEN));

                            MessageHistory.getHistory(uuid).forEach(msg -> {
                                String time = "[" + FORMATTER.format(ZonedDateTime.ofInstant(msg.time().toInstant(), ZoneId.of("UTC"))) + " UTC] ";
                                String additional = switch (msg.type()) {
                                    case NORMAL -> "";
                                    case STAFF -> Translation.string(l, "moderation.chat_history.staff") + " ";
                                    case PRIVATE -> Translation.string(l, "moderation.chat_history.private", msg.receiver()) + " ";
                                    case PARTY -> Translation.string(l, "moderation.chat_history.party") + " ";
                                };
                                player.sendMessage(Component.text("| " + time + additional, NamedTextColor.GRAY).append(Component.text(msg.content().strip(), NamedTextColor.WHITE)));
                            });
                            player.sendMessage(Component.text("=========================").color(NamedTextColor.DARK_GREEN));

                            return 1;
                        })
                )
                .build());
    }
}

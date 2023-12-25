package com.marcpg.peelocity.chat;

import com.marcpg.peelocity.Peelocity;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MessageLogging {
    public record MessageData(Date time, String content, Type type, @Nullable String receiver) {
        public enum Type { NORMAL, STAFF, PRIVATE, FRIEND }

        @Contract("_ -> new")
        public static @NotNull MessageData parse(@NotNull String line) throws ParseException {
            String[] elements = line.split(" \\|\\| ");
            Type type = Type.valueOf(elements[2]);
            return new MessageData(DATE_FORMAT.parse(elements[0]), elements[1], type, (type == Type.PRIVATE || type == Type.FRIEND ? elements[3] : null));
        }
    }

    public static final int MAX_HISTORY = 50;
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Subscribe(order = PostOrder.LAST)
    public void onPlayerChat(@NotNull PlayerChatEvent event) {
        saveMessage(event.getPlayer(), new MessageData(new Date(), event.getMessage().replace(" || ", " \\==|==\\==|== "), MessageData.Type.NORMAL, null));
    }

    public static void saveMessage(@NotNull Player player, @NotNull MessageData data) {
        final Path filePath = Path.of(Peelocity.DATA_DIRECTORY.toString() + "/msg-hist/" + player.getUniqueId().toString());

        try {
            if (!filePath.toFile().exists() || !Files.exists(filePath))
                Files.createFile(filePath);

            List<String> lines = Files.readAllLines(filePath);
            lines.add(DATE_FORMAT.format(data.time) + " ||  " + data.content + " || " + data.type + (data.receiver == null ? "" : " || " + data.receiver));

            if (lines.size() > MAX_HISTORY)
                lines.subList(0, lines.size() - MAX_HISTORY).clear();

            Files.write(filePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            Peelocity.LOG.error("Error while saving history of " + player.getUsername() + ": " + e.getMessage());
        }
    }

    public static List<MessageData> getHistory(@NotNull Player player) {
        final Path filePath = Path.of(Peelocity.DATA_DIRECTORY.toString() + "/msg-hist/" + player.getUniqueId().toString());

        try {
            return Files.readAllLines(filePath).stream()
                    .map(line -> {
                        try {
                            return MessageData.parse(line);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
        } catch (IOException e) {
            Peelocity.LOG.error("Error while getting/loading history of " + player.getUsername() + ": " + e.getMessage());
        }

        return List.of();
    }
}

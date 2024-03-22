package com.marcpg.poopermc.features;

import com.marcpg.poopermc.entity.OnlinePlayer;
import com.marcpg.poopermc.Pooper;
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
import java.util.UUID;

public class MessageLogging {
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

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static boolean enabled;
    public static int maxHistory;

    public static void saveMessage(OnlinePlayer<?> player, MessageData data) {
        if (!enabled) return;

        Path filePath = Pooper.DATA_DIR.resolve("message-history/" + player.uuid());

        try {
            if (!Files.exists(filePath)) Files.createFile(filePath);

            List<String> lines = Files.readAllLines(filePath);
            lines.add(DATE_FORMAT.format(data.time) + " || " + data.content + " || " + data.type + " || " + (data.receiver == null ? "" : " || " + data.receiver));

            if (lines.size() > maxHistory)
                lines.subList(0, lines.size() - maxHistory).clear();

            Files.write(filePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            Pooper.LOG.error("Error while saving history of " + player.name() + ": " + e.getMessage());
        }
    }

    public static List<MessageData> getHistory(@NotNull UUID uuid) {
        try {
            return Files.readAllLines(Pooper.DATA_DIR.resolve("message-history/" + uuid)).stream().map(MessageData::parse).toList();
        } catch (IOException e) {
            Pooper.LOG.error("Error while getting/loading history of user " + uuid + ": " + e.getMessage());
        }
        return List.of();
    }

    public static boolean noHistory(@NotNull UUID uuid) {
        return !Files.exists(Pooper.DATA_DIR.resolve("message-history/" + uuid));
    }
}

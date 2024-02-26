package com.marcpg.peelocity_old.chat;

import com.marcpg.peelocity_old.Peelocity;
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

    public static final int MAX_HISTORY = 50;
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Subscribe(order = PostOrder.LAST)
    public void onPlayerChat(@NotNull PlayerChatEvent event) {
        if (event.getResult().isAllowed()) saveMessage(event.getPlayer(), new MessageData(new Date(), event.getMessage().replace(" || ", " \\==|==\\==|== "), MessageData.Type.NORMAL, null));
    }

    public static void saveMessage(@NotNull Player player, @NotNull MessageData data) {
        final Path filePath = Path.of(Peelocity.DATA_DIRECTORY.toString() + "/message-history/" + player.getUniqueId().toString());

        try {
            if (!Files.exists(filePath)) Files.createFile(filePath);

            List<String> lines = Files.readAllLines(filePath);
            lines.add(DATE_FORMAT.format(data.time) + " ||  " + data.content + " || " + data.type + (data.receiver == null ? "" : " || " + data.receiver));
            if (lines.size() > MAX_HISTORY)
                lines.subList(0, lines.size() - MAX_HISTORY).clear();

            Files.write(filePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            Peelocity.LOG.error("Error while saving history of " + player.getUsername() + ": " + e.getMessage());
        }
    }

    public static List<MessageData> getHistory(@NotNull UUID uuid) {
        try {
            return Files.readAllLines(Path.of(Peelocity.DATA_DIRECTORY.toString() + "/message-history/" + uuid)).stream().map(MessageData::parse).toList();
        } catch (IOException e) {
            Peelocity.LOG.error("Error while getting/loading history of user " + uuid + ": " + e.getMessage());
        }
        return List.of();
    }

    public static boolean hasHistory(UUID uuid) {
        return Files.exists(Path.of(Peelocity.DATA_DIRECTORY.toString() + "/message-history/" + uuid));
    }
}

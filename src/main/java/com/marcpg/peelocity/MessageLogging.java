package com.marcpg.peelocity;

import com.marcpg.peelocity.Peelocity;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class MessageLogging {
    public static final int MAX_HISTORY = 50;
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Subscribe(order = PostOrder.NORMAL)
    public void onPlayerChat(@NotNull PlayerChatEvent event) {
        if (event.getMessage().contains(" || ")) {
            event.getPlayer().sendMessage(Component.text("We are very sorry, but due to global plugin functions, you can't send messages containing \" || \"!", TextColor.color(255, 0, 0)));
            event.setResult(PlayerChatEvent.ChatResult.denied());
            return;
        }

        final Path filePath = Path.of(Peelocity.DATA_DIRECTORY.toString() + "/msg-hist/" + event.getPlayer().getUniqueId().toString());

        try {
            List<String> lines = Files.readAllLines(filePath);
            lines.add(DATE_FORMAT.format(new Date()) + " ||  " + event.getMessage());

            if (lines.size() > MAX_HISTORY)
                lines.subList(0, lines.size() - MAX_HISTORY).clear();

            Files.write(filePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            Peelocity.LOG.error("Error while saving history of " + event.getPlayer().getUsername() + ": " + e.getMessage());
        }
    }

    public static Map<Date, String> getHistory(@NotNull Player player) {
        final Path filePath = Path.of(Peelocity.DATA_DIRECTORY.toString() + "/msg-hist/" + player.getUniqueId().toString());

        try {
            return Files.readAllLines(filePath).stream()
                    .map(line -> line.split(" \\|\\| "))
                    .filter(parts -> parts.length == 2)
                    .collect(Collectors.toMap(
                            parts -> {
                                try {
                                    return DATE_FORMAT.parse(parts[0]);
                                } catch (ParseException e) {
                                    throw new RuntimeException(e);
                                }
                            },
                            parts -> parts[1],
                            (existing, replacement) -> existing,
                            LinkedHashMap::new
                    ));
        } catch (IOException e) {
            Peelocity.LOG.error("Error while getting/loading history of " + player.getUsername() + ": " + e.getMessage());
        }

        return Map.of();
    }
}

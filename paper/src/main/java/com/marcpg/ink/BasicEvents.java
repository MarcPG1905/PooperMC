package com.marcpg.ink;

import com.marcpg.common.features.MessageLogging;
import com.marcpg.ink.common.PaperPlayer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Date;

public class BasicEvents implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        if (MessageLogging.enabled) {
            MessageLogging.saveMessage(PaperPlayer.ofPlayer(event.getPlayer()), new MessageLogging.MessageData(
                    new Date(),
                    PlainTextComponentSerializer.plainText().serialize(event.message()).replace(" || ", " \\==|==\\==|== "),
                    MessageLogging.MessageData.Type.NORMAL,
                    null
            ));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommandPreprocess(@NotNull PlayerCommandPreprocessEvent event) {
        // Catch all private messages and fork them to the message logger!
        if (event.getMessage().toLowerCase().startsWith("/msg")) {
            String[] parts = event.getMessage().split(" ");
            if (parts.length < 3) return;
            MessageLogging.saveMessage(PaperPlayer.ofPlayer(event.getPlayer()), new MessageLogging.MessageData(
                    new Date(),
                    String.join(" ", Arrays.copyOfRange(parts, 2, parts.length)),
                    MessageLogging.MessageData.Type.PRIVATE,
                    parts[1]
            ));
        }
    }
}

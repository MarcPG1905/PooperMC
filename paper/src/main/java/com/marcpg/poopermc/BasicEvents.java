package com.marcpg.poopermc;

import com.marcpg.poopermc.common.PaperPlayer;
import com.marcpg.poopermc.features.MessageLogging;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

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
}

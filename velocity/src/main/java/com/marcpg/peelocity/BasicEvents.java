package com.marcpg.peelocity;

import com.marcpg.peelocity.common.VelocityPlayer;
import com.marcpg.common.features.MessageLogging;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class BasicEvents {
    @Subscribe(order = PostOrder.LAST)
    public void onPlayerChat(@NotNull PlayerChatEvent event) {
        if (MessageLogging.enabled && event.getResult().isAllowed()) {
            MessageLogging.saveMessage(VelocityPlayer.ofPlayer(event.getPlayer()), new MessageLogging.MessageData(
                    new Date(),
                    event.getMessage().replace(" || ", " \\==|==\\==|== "),
                    MessageLogging.MessageData.Type.NORMAL,
                    null
            ));
        }
    }
}

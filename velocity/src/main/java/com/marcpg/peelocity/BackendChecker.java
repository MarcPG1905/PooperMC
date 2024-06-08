package com.marcpg.peelocity;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class BackendChecker {
    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("poopermc:internal");

    @Subscribe
    public void onPluginMessage(@NotNull PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) return;

        if (ByteStreams.newDataInput(event.getData()).readUTF().equals("RUNNING_PEELOCITY")) {
            Player player = (Player) event.getSource();

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("PEELOCITY_PRESENT");
            player.sendPluginMessage(CHANNEL, out.toByteArray());
        }
    }
}

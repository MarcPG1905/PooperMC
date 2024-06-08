package com.marcpg.ink;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class PeelocityChecker implements Listener, PluginMessageListener {
    public static final String CHANNEL = "poopermc:internal";

    public static boolean peelocity;
    private static boolean checked;

    public static void check(@NotNull Player player) {
        if (!player.isOnline()) return;

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("RUNNING_PEELOCITY");
        player.sendPluginMessage(InkPlugin.getPlugin(InkPlugin.class), CHANNEL, out.toByteArray());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        if (!checked) {
            check(event.getPlayer());
            checked = true;
        }
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!channel.equals(CHANNEL)) return;
        peelocity = ByteStreams.newDataInput(message).readUTF().equals("PEELOCITY_PRESENT");
    }
}

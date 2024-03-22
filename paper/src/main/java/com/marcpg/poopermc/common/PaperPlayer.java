package com.marcpg.poopermc.common;

import com.marcpg.poopermc.entity.OnlinePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.UUID;

public class PaperPlayer extends OnlinePlayer<Player> {
    public PaperPlayer(Player player) {
        super(player);
    }

    @Override
    public String name() {
        return platformPlayer.getName();
    }

    @Override
    public UUID uuid() {
        return platformPlayer.getUniqueId();
    }

    @Override
    public Locale locale() {
        return platformPlayer.locale();
    }

    @Override
    public void sendMessage(Component message) {
        platformPlayer.sendMessage(message);
    }

    @Override
    public void sendMessage(String plainMessage) {
        platformPlayer.sendPlainMessage(plainMessage);
    }

    @Override
    public void disconnect(Component message) {
        platformPlayer.kick(message);
    }

    public static @NotNull PaperPlayer ofPlayer(@NotNull Player player) {
        return new PaperPlayer(player);
    }
}

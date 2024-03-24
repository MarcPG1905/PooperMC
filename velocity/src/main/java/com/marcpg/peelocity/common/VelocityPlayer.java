package com.marcpg.peelocity.common;

import com.marcpg.common.entity.OnlinePlayer;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.UUID;

public class VelocityPlayer extends OnlinePlayer<Player> {
    public VelocityPlayer(Player player) {
        super(player);
    }

    @Override
    public String name() {
        return platformPlayer.getUsername();
    }

    @Override
    public UUID uuid() {
        return platformPlayer.getUniqueId();
    }

    @Override
    public Locale locale() {
        return platformPlayer.getEffectiveLocale();
    }

    @Override
    public void sendMessage(Component message) {
        platformPlayer.sendMessage(message);
    }

    @Override
    public void disconnect(Component message) {
        platformPlayer.disconnect(message);
    }

    public static @NotNull VelocityPlayer ofPlayer(@NotNull Player player) {
        return new VelocityPlayer(player);
    }
}

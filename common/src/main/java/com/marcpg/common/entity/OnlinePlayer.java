package com.marcpg.common.entity;

import net.kyori.adventure.text.Component;

import java.util.Locale;

public abstract class OnlinePlayer<T> implements IdentifiablePlayer {
    protected final T platformPlayer;

    public OnlinePlayer(T player) {
        this.platformPlayer = player;
    }

    public abstract Locale locale();

    public abstract void sendMessage(Component message);

    public abstract void disconnect(Component message);
}

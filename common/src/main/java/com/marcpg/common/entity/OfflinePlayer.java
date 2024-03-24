package com.marcpg.common.entity;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

// Can be a record for simplicity, so I guess that works lol.
public record OfflinePlayer(String name, UUID uuid) implements IdentifiablePlayer {
    public static @NotNull OfflinePlayer of(@NotNull OnlinePlayer<?> p) {
        return new OfflinePlayer(p.name(), p.uuid());
    }
}

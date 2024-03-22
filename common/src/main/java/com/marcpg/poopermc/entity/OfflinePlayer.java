package com.marcpg.poopermc.entity;

import java.util.UUID;

// Can be a record for simplicity, so I guess that works lol.
public record OfflinePlayer(String name, UUID uuid) implements IdentifiablePlayer {}

package com.marcpg.common;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.io.InputStream;
import java.util.Objects;

public enum Platform {
    /** Planned (not 100% sure) */ SPONGE("Sponge", "Water", NamedTextColor.BLUE, PlatformType.PLUGIN),
    /** Planned */ NUKKIT("Nukkit", "Nugget", TextColor.color(162, 110, 41), PlatformType.PLUGIN),
    /** Planned */ FABRIC("Fabric", "FaBrick", TextColor.color(165, 72, 66), PlatformType.MOD),
    /** Planned (same as fabric) */ QUILT("Quilt", "Pillow", TextColor.color(194, 225, 247), PlatformType.MOD),
    /** Planned (not 100% sure) */ FORGE("Forge", "Forgery", TextColor.color(162, 110, 41), PlatformType.MOD),
    /** Planned */ NEO_FORGE("NeoForge", "PaleoForge", TextColor.color(161, 161, 161), PlatformType.MOD),
    PAPER("Paper", "Ink", NamedTextColor.DARK_BLUE, PlatformType.PLUGIN),
    PURPUR("Purpur", "Poopur", TextColor.color(161, 114, 96), PlatformType.PLUGIN),
    VELOCITY("Velocity", "Peelocity", NamedTextColor.YELLOW, PlatformType.PROXY),
    /** For planned Bot-Integration */ DISCORD("Discord", "Discord", NamedTextColor.BLUE, PlatformType.DISCORD),
    UNKNOWN("Invalid Platform", "Invalid Platform", NamedTextColor.RED, PlatformType.PLUGIN);

    public final String realName;
    public final String specialName;
    public final TextColor color;
    public final PlatformType type;

    Platform(String real, String funny, TextColor color, PlatformType type) {
        this.realName = real;
        this.specialName = funny;
        this.color = color;
        this.type = type;
    }

    public InputStream configResource() {
        return Objects.requireNonNull(getClass().getResourceAsStream("/config-" + type.name().toLowerCase() + ".yml"));
    }

    public enum PlatformType { PROXY, PLUGIN, MOD, DISCORD }
}

package com.marcpg.common;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.io.InputStream;
import java.util.Objects;

public enum Platform {
    SPONGE("Sponge", "Water", NamedTextColor.BLUE, "???", PlatformType.PLUGIN),
    NUKKIT("Nukkit", "Nugget", TextColor.color(162, 110, 41), "???", PlatformType.PLUGIN),
    FABRIC("Fabric", "FaBrick", TextColor.color(165, 72, 66), "???", PlatformType.MOD),
    QUILT("Quilt", "Pillow", TextColor.color(194, 225, 247), "???", PlatformType.MOD),
    FORGE("Forge", "Forgery", TextColor.color(162, 110, 41), "???", PlatformType.MOD),
    NEO_FORGE("NeoForge", "PaleoForge", TextColor.color(161, 161, 161), "???", PlatformType.MOD),
    PAPER("Paper", "Ink", NamedTextColor.DARK_BLUE, "PooperMC", PlatformType.PLUGIN),
    PURPUR("Purpur", "Poopur", TextColor.color(161, 114, 96), "PooperMC", PlatformType.PLUGIN),
    VELOCITY("Velocity", "Peelocity", NamedTextColor.YELLOW, "pooper", PlatformType.PROXY),
    UNKNOWN("Invalid Platform", "Invalid Platform", NamedTextColor.RED, "invalid_platform", PlatformType.PLUGIN);

    public final String realName;
    public final String specialName;
    public final TextColor color;
    public final String dataDirName;
    public final PlatformType type;

    Platform(String real, String funny, TextColor color, String dataDir, PlatformType type) {
        this.realName = real;
        this.specialName = funny;
        this.color = color;
        this.dataDirName = dataDir;
        this.type = type;
    }

    public InputStream configResource() {
        return Objects.requireNonNull(getClass().getResourceAsStream("/config-" + type.name().toLowerCase() + ".yml"));
    }

    public enum PlatformType { PROXY, PLUGIN, MOD }
}

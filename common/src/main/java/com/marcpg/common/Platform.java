package com.marcpg.common;

import java.io.InputStream;
import java.util.Objects;

public enum Platform {
    BUNGEECORD("BungeeCord", "Carabiner", "???", PlatformType.PROXY),
    WATERFALL("Waterfall", "WaterFail", "???", PlatformType.PROXY),
    SPONGE("Sponge", "Water", "???", PlatformType.PLUGIN),
    NUKKIT("Nukkit", "Nugget", "???", PlatformType.PLUGIN),
    FABRIC("Fabric", "FaBrick", "???", PlatformType.MOD),
    QUILT("Quilt", "Pillow", "???", PlatformType.MOD),
    FORGE("Forge", "Forgery", "???", PlatformType.MOD),
    PAPER("Paper", "Ink", "PooperMC", PlatformType.PLUGIN),
    PURPUR("Purpur", "Poopur", "PooperMC", PlatformType.PLUGIN),
    VELOCITY("Velocity", "Peelocity", "pooper", PlatformType.PROXY),
    UNKNOWN("Invalid Platform", "Invalid Platform", "invalid_platform", PlatformType.PLUGIN);

    public final String realName;
    public final String specialName;
    public final String dataDirName;
    public final PlatformType type;

    Platform(String real, String funny, String dataDir, PlatformType type) {
        this.realName = real;
        this.specialName = funny;
        this.dataDirName = dataDir;
        this.type = type;
    }

    public InputStream configResource() {
        return Objects.requireNonNull(getClass().getResourceAsStream("/config-" + type.name().toLowerCase() + ".yml"));
    }

    public boolean isProxy() {
        return this == VELOCITY || this == BUNGEECORD || this == WATERFALL;
    }

    public enum PlatformType { PROXY, PLUGIN, MOD }
}

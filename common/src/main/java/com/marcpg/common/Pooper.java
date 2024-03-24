package com.marcpg.common;

import com.marcpg.common.util.UpdateChecker;
import com.marcpg.libpg.web.discord.Webhook;
import org.slf4j.Logger;

import java.nio.file.Path;

public class Pooper {
    public enum Platform {
        BUNGEECORD("BungeeCord", "Carabiner", "???"),
        WATERFALL("Waterfall", "WaterFail", "???"),
        SPONGE("Sponge", "Water", "???"),
        NUKKIT("Nukkit", "Nugget", "???"),
        FABRIC("Fabric", "FaBrick", "???"),
        QUILT("Quilt", "Pillow", "???"),
        FORGE("Forge", "Forgery", "???"),
        BUKKIT("Bukkit/CraftBukkit", "Fukkit", "PooperMC"),
        SPIGOT("Spigot", "Faucet", "PooperMC"),
        PAPER("Paper", "Ink", "PooperMC"),
        PURPUR("Purpur", "Poopur", "PooperMC"),
        VELOCITY("Velocity", "Peelocity", "pooper"),
        UNKNOWN("Invalid Platform", "Invalid Platform", "invalid_platform");

        public final String realName;
        public final String specialName;
        public final String dataDirName;

        Platform(String real, String funny, String dataDir) {
            this.realName = real;
            this.specialName = funny;
            this.dataDirName = dataDir;
        }
    }


    // ++++++++++ CONSTANTS ++++++++++
    public static final String VERSION = "1.1.0";
    public static final int BUILD = 1;
    public static final UpdateChecker.Version CURRENT_VERSION = new UpdateChecker.Version(5, VERSION + "+build." + BUILD, "ERROR");
    public static final int METRICS_ID = 21102;


    // ++++++++++ PLUGIN INSTANCE ++++++++++
    public static Platform PLATFORM = Platform.UNKNOWN;
    public static Logger LOG;
    public static Path DATA_DIR;
    public static Webhook MOD_WEBHOOK;
}

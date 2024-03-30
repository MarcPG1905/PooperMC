package com.marcpg.common;

import com.marcpg.common.logger.Logger;
import com.marcpg.common.util.AsyncScheduler;
import com.marcpg.common.util.UpdateChecker;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.nio.file.Path;

public class Pooper {
    // ++++++++++ CONSTANTS ++++++++++
    public static final String VERSION = "1.1.0";
    public static final int BUILD = 3;
    public static final UpdateChecker.Version CURRENT_VERSION = new UpdateChecker.Version(5, VERSION + "+build." + BUILD, "ERROR");
    public static final int METRICS_ID = 21102;


    // ++++++++++ PLUGIN INSTANCE ++++++++++
    public static Platform PLATFORM = Platform.UNKNOWN;
    public static Logger<?> LOG;
    public static Path DATA_DIR;
    public static AsyncScheduler SCHEDULER;

    /** Sends the info with the little ASCII art and version to a specific receiver specified by the sending logic. */
    public static void sendInfo(Audience audience) {
        switch (PLATFORM) {
            case SPONGE -> {
                audience.sendMessage(Component.text("   .  . _ ___ __ _ ", PLATFORM.color));
                audience.sendMessage(Component.text("   |  ||_| | |_ |_) PooperMC for Sponge (Water) " + VERSION, PLATFORM.color));
                audience.sendMessage(Component.text("   |/\\|| | | |__| \\\\ https://marcpg.com/pooper/sponge", PLATFORM.color));
            }
            case NUKKIT -> {
                audience.sendMessage(Component.text("   .  ..  . __", PLATFORM.color));
                audience.sendMessage(Component.text("   |\\ ||  |/ __ PooperMC for Nukkit (Nugget) " + VERSION, PLATFORM.color));
                audience.sendMessage(Component.text("   | \\||__|\\__| https://marcpg.com/pooper/nukkit", PLATFORM.color));
            }
            case FABRIC -> {
                audience.sendMessage(Component.text("    _  _ ___ __  ", PLATFORM.color));
                audience.sendMessage(Component.text("   |_)|_) | /  |/ PooperMC for Fabric (FaBrick) " + VERSION, PLATFORM.color));
                audience.sendMessage(Component.text("   |_)| \\_|_\\__|\\ https://marcpg.com/pooper/fabric", PLATFORM.color));
            }
            case QUILT -> {
                audience.sendMessage(Component.text("    _ ___.  .   _ .  .", PLATFORM.color));
                audience.sendMessage(Component.text("   |_) | |  |  / \\|  | PooperMC for Quilt (Pillow) " + VERSION, PLATFORM.color));
                audience.sendMessage(Component.text("   |  _|_|__|__\\_/|/\\| https://marcpg.com/pooper/fabric", PLATFORM.color));
            }
            case FORGE -> {
                audience.sendMessage(Component.text("    __ _  _  __ __ _.   .", PLATFORM.color));
                audience.sendMessage(Component.text("   |_ / \\|_)/__|_ |_)\\ / PooperMC for Forge (Forgery) " + VERSION, PLATFORM.color));
                audience.sendMessage(Component.text("   |  \\_/| \\\\_||__| \\ |  https://marcpg.com/pooper/forge", PLATFORM.color));
            }
            case NEO_FORGE -> {
                audience.sendMessage(Component.text("    _  _ .   __ _  ", PLATFORM.color));
                audience.sendMessage(Component.text("   |_)|_||  |_ / \\ PooperMC for NeoForge (PaleoForge) " + VERSION, PLATFORM.color));
                audience.sendMessage(Component.text("   |  | ||__|__\\_/ https://marcpg.com/pooper/neoforge", PLATFORM.color));
            }
            case PAPER -> {
                audience.sendMessage(Component.text("   ~|~|\\  || /", PLATFORM.color));
                audience.sendMessage(Component.text("    | | \\ ||(   PooperMC for Paper (Ink) " + VERSION, PLATFORM.color));
                audience.sendMessage(Component.text("   _|_|  \\|| \\_ https://marcpg.com/pooper/paper", PLATFORM.color));
            }
            case PURPUR -> {
                audience.sendMessage(Component.text("    _  _  _ ", PLATFORM.color));
                audience.sendMessage(Component.text("   |_)/ \\/ \\ PooperMC for Purpur (Poopur) " + VERSION, PLATFORM.color));
                audience.sendMessage(Component.text("   |  \\_/\\_/ https://marcpg.com/pooper/paper", PLATFORM.color));
            }
            case VELOCITY -> {
                audience.sendMessage(Component.text("    __   __  __", PLATFORM.color));
                audience.sendMessage(Component.text("   |__) |__ |__ PooperMC for Velocity (Peelocity) " + VERSION, PLATFORM.color));
                audience.sendMessage(Component.text("   |    |__ |__ https://marcpg.com/pooper/velocity", PLATFORM.color));
            }
            case UNKNOWN -> {
                audience.sendMessage(Component.text("   ??????", PLATFORM.color));
                audience.sendMessage(Component.text("   ?????? Invalid PooperMC Platform!", PLATFORM.color));
                audience.sendMessage(Component.text("   ?????? Please report this bug to a PooperMC developer!", PLATFORM.color));
            }
        }
        audience.sendMessage(Component.text("   Version: " + VERSION + "+build." + BUILD, NamedTextColor.GRAY));
    }
}

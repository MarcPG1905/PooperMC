package com.marcpg.common;

import com.alessiodp.libby.LibraryManager;
import com.marcpg.common.logger.Logger;
import com.marcpg.common.moderation.Banning;
import com.marcpg.common.moderation.Muting;
import com.marcpg.common.platform.AsyncScheduler;
import com.marcpg.common.platform.CommandManager;
import com.marcpg.common.platform.EventManager;
import com.marcpg.common.platform.FaviconHandler;
import com.marcpg.common.social.FriendSystem;
import com.marcpg.common.storage.DatabaseStorage;
import com.marcpg.common.util.UpdateChecker;
import com.marcpg.libpg.color.Ansi;
import com.marcpg.libpg.lang.Translation;
import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public abstract class Pooper<P, E, C> {
    // ++++++++++ CONSTANTS ++++++++++
    public static final String VERSION = "1.1.2";
    public static final int BUILD = 1;
    public static final UpdateChecker.Version CURRENT_VERSION = new UpdateChecker.Version(7, VERSION + "+build." + BUILD, "ERROR");

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


    // ++++++++++++++++++++++++++++++++++++++
    // ++++++++++ INSTANCE METHODS ++++++++++
    // ++++++++++++++++++++++++++++++++++++++

    public static final List<String> SUPPORTED_AUDIENCES = List.of("@a", "@s", "@r");
    public static Pooper<?, ?, ?> INSTANCE;
    public static Object PLUGIN;

    protected final P plugin;
    protected final EventManager<E, P> eventManager;
    protected final CommandManager<C, P> commandManager;

    protected Pooper(P plugin, EventManager<E, P> eventManager, CommandManager<C, P> commandManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.commandManager = commandManager;
        INSTANCE = this;
        PLUGIN = plugin;
    }

    public void loadBasic(FaviconHandler<?> faviconHandler, LibraryManager libraryManager) throws IOException {
        Configuration.createFileTree();
        Configuration.load(faviconHandler, libraryManager);

        events(eventManager);
        commands(commandManager);

        UpdateChecker.checkUpdates();

        try {
            Translation.loadProperties(DATA_DIR.resolve("lang").toFile());
        } catch (IOException e) {
            Pooper.LOG.error("The downloaded translations are corrupted or missing, so the translations couldn't be loaded!");
        }

        try {
            Path path = DATA_DIR.resolve(".no_setup");
            if (path.toFile().createNewFile()) {
                Files.setAttribute(path, "dos:hidden", true);
                LOG.info(Ansi.formattedString("Please consider checking out the PooperMC setup: https://github.com/MarcPG1905/PooperMC#setup!", Ansi.BRIGHT_BLUE, Ansi.BLINK));
            }
        } catch (IOException ignored) {
        } // Shouldn't really happen and if it does, just ignoring it is probably the best option.
    }

    public final void startup(FaviconHandler<?> faviconHandler, LibraryManager libraryManager) throws IOException {
        long start = System.currentTimeMillis();

        if (Locale.getDefault() == null) Locale.setDefault(new Locale("en", "US"));

        loadBasic(faviconHandler, libraryManager);
        additionalLogic();

        LOG.info(Ansi.green("Loaded all components, took " + (System.currentTimeMillis() - start) + "ms!"));
        sendInfo(Pooper.LOG);
    }

    /**
     * Gets executed after the stuff from {@link #loadBasic(FaviconHandler, LibraryManager)}, but still before the
     * update checking, translation loading and setup hint.
     */
    public void additionalLogic() {}

    /** Gets executed at the end of the configuration loading, but still before the translation download. */
    public void extraConfiguration(YamlDocument doc) {}

    public void unload() {
        eventManager.reset(plugin);
        commandManager.reset(plugin);
    }

    public void shutdown() {
        if (Banning.STORAGE instanceof DatabaseStorage<UUID> storage) storage.shutdown();
        if (Muting.STORAGE instanceof DatabaseStorage<UUID> storage) storage.shutdown();
        if (FriendSystem.STORAGE instanceof DatabaseStorage<UUID> storage) storage.shutdown();
    }

    public void events(EventManager<E, P> manager) {
        LOG.info(Ansi.gray("Registering Events..."));
    }

    public void commands(CommandManager<C, P> manager) {
        LOG.info(Ansi.gray("Registering Commands..."));
    }

    public abstract Locale getLocale(Audience audience);
    public abstract Audience parseAudience(String[] args, Audience sender);
}

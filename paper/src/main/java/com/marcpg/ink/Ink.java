package com.marcpg.ink;

import com.alessiodp.libby.BukkitLibraryManager;
import com.marcpg.common.Configuration;
import com.marcpg.common.Platform;
import com.marcpg.common.Pooper;
import com.marcpg.common.logger.SLF4JLogger;
import com.marcpg.common.storage.Storage;
import com.marcpg.common.util.UpdateChecker;
import com.marcpg.ink.common.PaperAsyncScheduler;
import com.marcpg.ink.common.PaperFaviconHandler;
import com.marcpg.ink.features.PaperChatUtilities;
import com.marcpg.ink.features.PaperServerList;
import com.marcpg.ink.moderation.*;
import com.marcpg.ink.social.PaperFriendSystem;
import com.marcpg.libpg.color.Ansi;
import com.marcpg.libpg.lang.Translation;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

public class Ink extends JavaPlugin {
    static { setPlatform(); }

    @Override
    public void onEnable() {
        // Ensure that the PaperAPI is supported!
        try {
            Class.forName("io.papermc.paper.text.PaperComponents");
        } catch (ClassNotFoundException e) {
            getLogger().severe("==================================================================");
            getLogger().severe("          PooperMC can only run on Paper or forks of it!          ");
            getLogger().severe("Running PooperMC on pure CraftBukkit or SpigotMC is not supported!");
            getLogger().severe("==================================================================");
            throw new RuntimeException("Unsupported platform, read message above!");
        }

        long start = System.currentTimeMillis();

        if (Locale.getDefault() == null) Locale.setDefault(new Locale("en", "US"));

        Pooper.LOG = new SLF4JLogger(getSLF4JLogger());
        Pooper.DATA_DIR = getDataFolder().toPath();
        Pooper.SCHEDULER = new PaperAsyncScheduler(this, Bukkit.getScheduler());

        try {
            Configuration.createFileTree();
            Configuration.load(
                    new PaperFaviconHandler(),
                    new BukkitLibraryManager(this, getDataFolder().getName()),
                    Pooper.SCHEDULER
            );
        } catch (IOException e) {
            Pooper.LOG.error("Couldn't create/load the configuration!");
        }

        metrics(new Metrics(this, Pooper.METRICS_ID));
        events(getServer().getPluginManager());
        commands();

        UpdateChecker.checkUpdates();

        Pooper.LOG.info(Ansi.green("Loaded all components, took " + (System.currentTimeMillis() - start) + "ms!"));

        sendWelcome();

        try {
            Translation.loadProperties(new File(getDataFolder(), "lang"));
        } catch (IOException e) {
            Pooper.LOG.error("The downloaded translations are corrupted or missing, so the translations couldn't be loaded!");
        }

        try {
            Path path = Pooper.DATA_DIR.resolve(".no_setup");
            if (path.toFile().createNewFile()) {
                Files.setAttribute(path, "dos:hidden", true);
                Pooper.LOG.info(Ansi.formattedString("Please consider checking out the PooperMC setup, by running PooperMC-?.jar as a java program.", Ansi.BRIGHT_BLUE, Ansi.BLINK));
                Pooper.LOG.info(Ansi.formattedString("See further instructions on https://github.com/MarcPG1905/PooperMC#setup!", Ansi.BRIGHT_BLUE, Ansi.BLINK));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void sendWelcome() {
        switch (Pooper.PLATFORM) {
            case PAPER -> {
                Pooper.LOG.info(Ansi.yellow("   ~|~|\\  || /"));
                Pooper.LOG.info(Ansi.yellow("    | | \\ ||(   PooperMC for Paper (Ink) " + Pooper.VERSION));
                Pooper.LOG.info(Ansi.yellow("   _|_|  \\|| \\_ https://marcpg.com/pooper/bukkit"));
            }
            case PURPUR -> {
                Pooper.LOG.info(Ansi.yellow("    _  _  _ "));
                Pooper.LOG.info(Ansi.yellow("   |_)/ \\/ \\ PooperMC for Purpur (Poopur) " + Pooper.VERSION));
                Pooper.LOG.info(Ansi.yellow("   |  \\_/\\_/ https://marcpg.com/pooper/bukkit"));
            }
        }
        Pooper.LOG.info(Ansi.gray("   Version: " + Pooper.VERSION + "+build." + Pooper.BUILD));
    }

    void metrics(@NotNull Metrics metrics) {
        Pooper.LOG.info(Ansi.gray("Sending Metrics to bStats..."));
        metrics.addCustomChart(new SimplePie("chat_utils", () -> String.valueOf(Configuration.chatUtilities.getBoolean("enabled"))));
        metrics.addCustomChart(new SimplePie("server_list", () -> String.valueOf(Configuration.doc.getBoolean("server-list.enabled"))));
        metrics.addCustomChart(new SimplePie("storage_method", () -> Storage.storageType.name().toLowerCase()));
        metrics.addCustomChart(new SimplePie("translations", () -> String.valueOf(Configuration.downloadTranslations)));
    }

    void events(@NotNull PluginManager manager) {
        Pooper.LOG.info(Ansi.gray("Registering Events..."));
        manager.registerEvents(new PaperBanning(), this);
        manager.registerEvents(new PaperMuting(), this);
        manager.registerEvents(new BasicEvents(), this);
        manager.registerEvents(new PaperBanning(), this);
        manager.registerEvents(new PaperMuting(), this);
        if (Configuration.chatUtilities.getBoolean("enabled")) manager.registerEvents(new PaperChatUtilities(), this);
        if (Configuration.doc.getBoolean("server-list.enabled")) manager.registerEvents(new PaperServerList(), this);
    }

    void commands() {
        Objects.requireNonNull(getCommand("msg-hist")).setExecutor(new Commands.MsgHistCommand());
        Objects.requireNonNull(getCommand("ban")).setExecutor(new PaperBanning.BanCommand());
        Objects.requireNonNull(getCommand("pardon")).setExecutor(new PaperBanning.PardonCommand());
        Objects.requireNonNull(getCommand("mute")).setExecutor(new PaperMuting.MuteCommand());
        Objects.requireNonNull(getCommand("unmute")).setExecutor(new PaperMuting.UnmuteCommand());
        Objects.requireNonNull(getCommand("kick")).setExecutor(new PaperKicking());
        Objects.requireNonNull(getCommand("report")).setExecutor(new PaperReporting());
        Objects.requireNonNull(getCommand("friend")).setExecutor(new PaperFriendSystem());
        Objects.requireNonNull(getCommand("staff")).setExecutor(new PaperStaffChat());
    }

    private static void setPlatform() {
        Pooper.PLATFORM = Platform.PAPER;
        try {
            Class.forName("org.purpurmc.purpur.event.PlayerAFKEvent");
            Pooper.PLATFORM = Platform.PURPUR;
        } catch (ClassNotFoundException ignored) {}
    }
}

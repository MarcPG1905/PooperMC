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

public class Ink extends JavaPlugin {
    public static Ink INSTANCE;

    static { setPlatform(); }

    @Override
    public void onEnable() {
        INSTANCE = this;

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

        try {
            Translation.loadProperties(new File(getDataFolder(), "lang"));
        } catch (IOException e) {
            Pooper.LOG.error("The downloaded translations are corrupted or missing, so the translations couldn't be loaded!");
        }

        try {
            Path path = Pooper.DATA_DIR.resolve(".no_setup");
            if (path.toFile().createNewFile()) {
                Files.setAttribute(path, "dos:hidden", true);
                Pooper.LOG.info(Ansi.formattedString("Please consider checking out the PooperMC setup: https://github.com/MarcPG1905/PooperMC#setup!", Ansi.BRIGHT_BLUE, Ansi.BLINK));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Pooper.sendInfo(Pooper.LOG);
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

    @SuppressWarnings("DataFlowIssue")
    void commands() {
        getCommand("ban").setExecutor(new PaperBanning.BanCommand());
        getCommand("config").setExecutor(new Commands.ConfigCommand());
        getCommand("friend").setExecutor(new PaperFriendSystem());
        getCommand("ink").setExecutor(new Commands.InkCommand());
        getCommand("kick").setExecutor(new PaperKicking());
        getCommand("msg-hist").setExecutor(new Commands.MsgHistCommand());
        getCommand("mute").setExecutor(new PaperMuting.MuteCommand());
        getCommand("pardon").setExecutor(new PaperBanning.PardonCommand());
        getCommand("report").setExecutor(new PaperReporting());
        getCommand("staff").setExecutor(new PaperStaffChat());
        getCommand("unmute").setExecutor(new PaperMuting.UnmuteCommand());
    }

    private static void setPlatform() {
        Pooper.PLATFORM = Platform.PAPER;
        try {
            Class.forName("org.purpurmc.purpur.event.PlayerAFKEvent");
            Pooper.PLATFORM = Platform.PURPUR;
        } catch (ClassNotFoundException ignored) {}
    }

    void reload() throws IOException {
        Configuration.createFileTree();
        Configuration.load(
                new PaperFaviconHandler(),
                new BukkitLibraryManager(this, getDataFolder().getName()),
                Pooper.SCHEDULER
        );

        metrics(new Metrics(this, Pooper.METRICS_ID));
        events(getServer().getPluginManager());
        commands();

        UpdateChecker.checkUpdates();

        try {
            Translation.loadProperties(new File(getDataFolder(), "lang"));
        } catch (IOException e) {
            Pooper.LOG.error("The downloaded translations are corrupted or missing, so the translations couldn't be loaded!");
        }
    }
}

package com.marcpg.fukkit;

import com.alessiodp.libby.BukkitLibraryManager;
import com.marcpg.common.Pooper;
import com.marcpg.common.storage.DatabaseStorage;
import com.marcpg.common.storage.Storage;
import com.marcpg.common.util.UpdateChecker;
import com.marcpg.fukkit.features.ChatUtilities;
import com.marcpg.fukkit.features.ServerList;
import com.marcpg.fukkit.moderation.*;
import com.marcpg.fukkit.social.PaperFriendSystem;
import com.marcpg.libpg.color.Ansi;
import com.marcpg.libpg.lang.Translation;
import com.marcpg.libpg.web.discord.Webhook;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

public class Fukkit extends JavaPlugin {
    static { setPlatform(); }

    public static FileConfiguration configuration;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();

        if (Locale.getDefault() == null) Locale.setDefault(new Locale("en", "US"));

        Pooper.LOG = getSLF4JLogger();
        Pooper.DATA_DIR = getDataFolder().toPath();

        configuration();
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
                Pooper.LOG.info(Ansi.formattedString("See further instructions on https://github.com/MarcPG1905/Peelocity#setup!", Ansi.BRIGHT_BLUE, Ansi.BLINK));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void sendWelcome() {
        Pooper.LOG.info(Ansi.yellow("   ~|~|\\  || /"));
        Pooper.LOG.info(Ansi.yellow("    | | \\ ||(   PooperMC for Paper (Ink) " + Pooper.VERSION));
        Pooper.LOG.info(Ansi.yellow("   _|_|  \\|| \\_ https://marcpg.com/poopermc/paper"));
        Pooper.LOG.info(Ansi.gray("   Version: " + Pooper.VERSION + "+build." + Pooper.BUILD));
    }

    void metrics(@NotNull Metrics metrics) {
        Pooper.LOG.info(Ansi.gray("Sending Metrics to bStats..."));
        metrics.addCustomChart(new SimplePie("chat_utils", () -> String.valueOf(configuration.getBoolean("chatutilities.enabled"))));
        metrics.addCustomChart(new SimplePie("server_list", () -> String.valueOf(configuration.getBoolean("server-list.enabled"))));
        metrics.addCustomChart(new SimplePie("storage_method", () -> Objects.requireNonNull(configuration.getString("storage-method")).toLowerCase()));
        metrics.addCustomChart(new SimplePie("translations", () -> String.valueOf(configuration.getBoolean("enable-translations"))));
    }

    void events(@NotNull PluginManager manager) {
        Pooper.LOG.info(Ansi.gray("Registering Events..."));
        manager.registerEvents(new PaperBanning(), this);
        manager.registerEvents(new PaperMuting(), this);
        manager.registerEvents(new BasicEvents(), this);
        manager.registerEvents(new PaperBanning(), this);
        manager.registerEvents(new PaperMuting(), this);
        if (configuration.getBoolean("chatutility.enabled")) manager.registerEvents(new ChatUtilities(), this);
        if (configuration.getBoolean("server-list.enabled")) manager.registerEvents(new ServerList(), this);
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

    void configuration() {
        saveDefaultConfig();
        configuration = getConfig();

        Storage.storageType = Storage.StorageType.valueOf(Objects.requireNonNull(configuration.getString("storage-method")).toUpperCase());
        if (Storage.storageType == Storage.StorageType.DATABASE)
            DatabaseStorage.loadDependency(new BukkitLibraryManager(this, Pooper.DATA_DIR.toFile().getName()));

        try {
            Pooper.MOD_WEBHOOK = new Webhook(new URL(Objects.requireNonNull(configuration.getString("moderator-webhook"))));
        } catch (Exception e) {
            Pooper.LOG.warn("The `moderator-webhook` is not properly configured! You can safely ignore this if you don't have a webhook.");
        }

        if (configuration.getBoolean("server-list.enabled")) {
            ServerList.motd = configuration.getBoolean("server-list.custom-motd");
            if (ServerList.motd) ServerList.motdList = configuration.getStringList("server-list.custom-motd-messages").stream()
                    .map(s -> MiniMessage.miniMessage().deserialize(s))
                    .toList();

            ServerList.favicon = configuration.getBoolean("server-list.custom-favicon");
            if (ServerList.favicon) ServerList.faviconList = configuration.getStringList("server-list.custom-favicon-urls").stream()
                    .map(s -> {
                        try {
                            return Bukkit.loadServerIcon(ImageIO.read(new URI(s).toURL()));
                        } catch (IOException | URISyntaxException e) {
                            throw new RuntimeException(e);
                        } catch (Exception e) {
                            Pooper.LOG.error("One or more of the provided favicons is not 64x64 pixels!");
                            return null;
                        }
                    })
                    .toList();

            ServerList.showMaxPlayers = configuration.getInt("server-list.show-max-players");
            ServerList.showCurrentPlayers = configuration.getInt("server-list.show-current-players");
        }
    }

    private static void setPlatform() {
        try {
            // Spigot - org.spigotmc.*
            Class.forName("org.spigotmc.event.player.PlayerSpawnLocationEvent");
            Pooper.PLATFORM = Pooper.Platform.SPIGOT;

            // Paper - io.papermc.paper.*
            Class.forName("io.papermc.paper.util.Tick");
            Pooper.PLATFORM = Pooper.Platform.PAPER;

            // Purpur - org.purpurmc.purpur.*
            Class.forName("org.purpurmc.purpur.event.PlayerAFKEvent");
            Pooper.PLATFORM = Pooper.Platform.PURPUR;
        } catch (ClassNotFoundException ignored) {}
    }
}

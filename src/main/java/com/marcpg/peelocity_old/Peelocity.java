package com.marcpg.peelocity_old;

import com.google.inject.Inject;
import com.marcpg.color.Ansi;
import com.marcpg.lang.Translation;
import com.marcpg.peelocity_old.admin.Announcements;
import com.marcpg.peelocity_old.chat.MessageLogging;
import com.marcpg.peelocity_old.chat.PrivateMessaging;
import com.marcpg.peelocity_old.chat.StaffChat;
import com.marcpg.peelocity_old.moderation.*;
import com.marcpg.peelocity_old.modules.ChatUtilities;
import com.marcpg.peelocity_old.modules.ServerList;
import com.marcpg.peelocity_old.modules.Whitelist;
import com.marcpg.peelocity_old.social.FriendSystem;
import com.marcpg.peelocity_old.social.PartySystem;
import com.marcpg.peelocity_old.storage.DatabaseStorage;
import com.marcpg.peelocity_old.storage.Storage;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bstats.charts.SimplePie;
import org.bstats.velocity.Metrics;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

@Plugin(
        id = "peelocity",
        name = "Peelocity",
        version = Peelocity.PEELOCITY_VERSION,
        description = "General purpose Velocity plugin with all kinds of features.",
        url = "https://marcpg.com/peelocity",
        authors = { "MarcPG" }
)
public class Peelocity {
    @SuppressWarnings("unused")
    public enum ReleaseType { ALPHA, BETA, SNAPSHOT, PRE, RELEASE }  public static final ReleaseType PEELOCITY_RELEASE_TYPE = ReleaseType.BETA;
    public static final String PEELOCITY_VERSION = "0.2.0";
    public static final String PEELOCITY_BUILD_NUMBER = "2";

    public static final List<String> COMMANDS = List.of("announce", "ban", "config", "friend", "hub", "join",
            "kick", "message-history", "msg", "mute", "pardon", "party", "peeload", "report", "staff", "unmute", "w");

    public static Peelocity PLUGIN;
    public static ProxyServer SERVER;
    public static Logger LOG;
    public static Path DATA_DIRECTORY;
    public static Metrics.Factory METRICS_FACTORY;


    @Inject
    public Peelocity(@NotNull ProxyServer server, @NotNull Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        PLUGIN = this;
        SERVER = server;
        LOG = logger;
        DATA_DIRECTORY = dataDirectory;
        METRICS_FACTORY = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws IOException {
        SERVER.getChannelRegistrar().register(JoinLogic.IDENTIFIER);
        loadLogic(getClass().getResourceAsStream("/pee.yml"));
        sendWelcome(SERVER.getConsoleCommandSource()); // Sends the cool little info message
    }

    /**
     * Sends this welcome message with the PEE text using {@code |} and {@code _} into the specified console:
     * <pre>      __   __  __
     *    |__) |__ |__ Peelocity
     *    |    |__ |__ <a href="https://marcpg.com/peelocity">https://marcpg.com/peelocity</a>
     *    Build: VERSION-BUILD_NR (RELEASE_TYPE)</pre>
     * @param console The console (-command source) to send the message to.
     */
    static void sendWelcome(@NotNull ConsoleCommandSource console) {
        console.sendMessage(Component.text("    __   __  __", NamedTextColor.YELLOW));
        console.sendMessage(Component.text("   |__) |__ |__ Peelocity " + PEELOCITY_VERSION, NamedTextColor.YELLOW));
        console.sendMessage(Component.text("   |    |__ |__ https://marcpg.com/peelocity", NamedTextColor.YELLOW));
        console.sendMessage(Component.text("   Build: " + PEELOCITY_VERSION + "-" + PEELOCITY_BUILD_NUMBER + " (" + PEELOCITY_RELEASE_TYPE + ")", NamedTextColor.DARK_GRAY));
    }

    public static void loadLogic(InputStream peeYml) throws IOException {
        long start = System.currentTimeMillis();

        Config.createDataDirectory();
        Config.load(peeYml);

        PlayerCache.load(); // Loads all players into cache

        if (Config.STORAGE_TYPE == Storage.StorageType.DATABASE)
            DatabaseStorage.loadDependency(); // Downloads and loads the database's JDBC driver

        LOG.info("Sending Peelocity Metrics...");
        bStatsMetrics(21102); // Sends the metrics on bStats

        LOG.info("Registering Events...");
        registerEvents(SERVER.getEventManager()); // Registers the events, if enabled

        LOG.info("Registering Commands...");
        registerCommands(SERVER.getCommandManager()); // Registers the commands, if enabled

        LOG.info(Ansi.formattedString("Loaded all components, took " + (System.currentTimeMillis() - start) + "ms!", Ansi.GREEN));

        try {
            Translation.loadProperties(Peelocity.DATA_DIRECTORY.resolve("lang").toFile());
        } catch (IOException e) {
            Peelocity.LOG.warn("The downloaded translations are corrupted or missing, so the translations couldn't be loaded!");
        }
    }

    /**
     * Sends all metrics about the current instance to the bStats plugin.
     * @param pluginId The plugin's ID on bStats.
     */
    public static void bStatsMetrics(int pluginId) {
        Metrics metrics = METRICS_FACTORY.make(Peelocity.PLUGIN, pluginId);
        metrics.addCustomChart(new SimplePie("storage_method", () -> Config.STORAGE_TYPE.toString().toLowerCase()));
        metrics.addCustomChart(new SimplePie("server_list", () -> String.valueOf(Config.SL_ENABLED)));
        metrics.addCustomChart(new SimplePie("chat_utils", () -> String.valueOf(Config.CHATUTILITY_BOOLEANS.getBoolean("enabled"))));
        metrics.addCustomChart(new SimplePie("translations", () -> String.valueOf(Config.CONFIG.getBoolean("enable-translations"))));
        metrics.addCustomChart(new SimplePie("whitelist", () -> String.valueOf(Config.WHITELIST_ENABLED)));
    }

    /**
     * Goes through all events and registers them, if they need to be registered.
     * @param manager The instance's event manager, that events are registered on.
     */
    public static void registerEvents(@NotNull EventManager manager) {
        manager.unregisterListeners(Peelocity.PLUGIN);

        if (Config.CHATUTILITY_BOOLEANS.getBoolean("enabled")) manager.register(Peelocity.PLUGIN, new ChatUtilities());
        if (Config.SL_ENABLED) manager.register(Peelocity.PLUGIN, new ServerList());
        if (Config.WHITELIST_ENABLED) manager.register(Peelocity.PLUGIN, new Whitelist());
        manager.register(Peelocity.PLUGIN, new Bans());
        manager.register(Peelocity.PLUGIN, new JoinLogic());
        manager.register(Peelocity.PLUGIN, new MessageLogging());
        manager.register(Peelocity.PLUGIN, new Mutes());
    }

    /**
     * Goes through all commands and registers them, if they need to be registered.
     * @param manager The instance's command manager, that commands are registered on.
     */
    public static void registerCommands(@NotNull CommandManager manager) {
        if (manager.hasCommand("announce")) COMMANDS.forEach(manager::unregister);

        manager.register("announce", Announcements.createAnnounceBrigadier());
        manager.register("ban", Bans.createBanBrigadier());
        manager.register("config", Config.createConfigBrigadier());
        manager.register("friend", FriendSystem.createFriendBrigadier());
        manager.register("hub", JoinLogic.createHubBrigadier(), "lobby", "leave");
        manager.register("kick", Kicks.createKickBrigadier());
        manager.register("message-history", UserUtil.createMessageHistoryBrigadier(), "msg-hist", "history");
        manager.register("msg", PrivateMessaging.createMsgBrigadier(), "pm");
        manager.register("mute", Mutes.createMuteBrigadier(), "timeout");
        manager.register("pardon", Bans.createPardonBrigadier(), "unban");
        manager.register("party", PartySystem.createPartyBrigadier(SERVER));
        manager.register("peeload", Config.createPeeloadBrigadier(Peelocity.PLUGIN), "reload-peelocity");
        manager.register("staff", StaffChat.createStaffBrigadier(), "sc", "staff-chat");
        manager.register("unmute", Mutes.createUnmuteBrigadier(), "remove-timeout");
        manager.register("w", PrivateMessaging.createWBrigadier());

        if (!Config.GAMEMODES.isEmpty())
            manager.register("join", JoinLogic.createJoinBrigadier(), "play");
        else LOG.warn("Skipping /join registration, as there are no game modes configured.");

        if (Config.MODERATOR_WEBHOOK_ENABLED)
            manager.register("report", Reporting.createReportBrigadier(), "snitch");
        else LOG.info("Skipping /report registration, as the moderator-webhook is not configured.");

        if (Config.WHITELIST_ENABLED)
            manager.register("whitelist", Whitelist.createWhitelistBrigadier());
        else LOG.info("Skipping /whitelist registration, as the whitelist is disabled.");

        manager.register("ping", (SimpleCommand) invocation -> {
            Player source = (Player) invocation.source();
            long ping = source.getPing();
            int limitedPing = (int) Math.min(254, ping);
            int limitedPingPurple = (int) Math.min(508, ping);
            source.sendMessage(Translation.component(source.getEffectiveLocale(), "cmd.ping.info").color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC));
            source.sendMessage(Translation.component(source.getEffectiveLocale(), "cmd.ping.stat", ping).color(TextColor.color(limitedPing, 254 - limitedPing, (ping > 260 ? limitedPingPurple - 254 : 0))));
        }, "latency");

        manager.register("peelocity", (SimpleCommand) invocation -> {
            Player source = (Player) invocation.source();
            source.sendMessage(Component.text("Peelocity").decorate(TextDecoration.BOLD).append(Component.text(" " + PEELOCITY_VERSION + "-" + PEELOCITY_RELEASE_TYPE + " (" + PEELOCITY_BUILD_NUMBER + ")").decoration(TextDecoration.BOLD, false)).color(TextColor.color(0, 170, 170)));
            source.sendMessage(Translation.component(source.getEffectiveLocale(), "cmd.peelocity.info"));
        }, "velocity-plugin");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) throws IOException {
        PlayerCache.save();
    }

    /**
     * Logs an important error to the {@link Peelocity#LOG instance's logger}, that is hard to overlook. <br>
     * Should only be used for really important things, that really need to be seen by the user.
     * @param msg A brief description of what went wrong.
     */
    public static void importantError(String msg) {
        msg = " " + msg + " ";
        Peelocity.LOG.error("----------------------------------------------------------------------------------------");
        Peelocity.LOG.error("                                       IMPORTANT                                        ");
        Peelocity.LOG.error(Ansi.bold(msg));
        Peelocity.LOG.error("----------------------------------------------------------------------------------------");
    }
}

package com.marcpg.peelocity;

import com.google.inject.Inject;
import com.marcpg.color.Ansi;
import com.marcpg.peelocity.admin.Announcements;
import com.marcpg.peelocity.chat.MessageLogging;
import com.marcpg.peelocity.chat.PrivateMessaging;
import com.marcpg.peelocity.chat.StaffChat;
import com.marcpg.peelocity.moderation.*;
import com.marcpg.peelocity.social.FriendSystem;
import com.marcpg.peelocity.social.PartySystem;
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
import net.hectus.lang.Translation;
import net.hectus.sql.PostgreConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

@Plugin(
        id = "peelocity",
        name = "Peelocity",
        version = Peelocity.PEELOCITY_VERSION,
        description = "General purpose Velocity plugin with all kinds of features.",
        url = "https://marcpg.com/peelocity",
        authors = { "MarcPG" }
)
public class Peelocity {
    public enum ReleaseType { ALPHA, BETA, SNAPSHOT, PRE, RELEASE }

    public static final ReleaseType PEELOCITY_RELEASE_TYPE = ReleaseType.BETA;
    public static final String PEELOCITY_VERSION = "0.1.4";
    public static final String PEELOCITY_BUILD_NUMBER = "3";

    public static Peelocity PLUGIN;
    public static ProxyServer SERVER;
    public static Logger LOG;
    public static Path DATA_DIRECTORY;
    public static PostgreConnection DATABASE;

    @Inject
    public Peelocity(@NotNull ProxyServer server, @NotNull Logger logger, @DataDirectory Path dataDirectory) {
        PLUGIN = this;
        SERVER = server;
        LOG = logger;
        DATA_DIRECTORY = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws IOException, SQLException, ClassNotFoundException {
        long start = System.currentTimeMillis();

        Config.saveDefaultConfig();
        Config.load();

        PlayerCache.loadCachedUsers();

        if (Config.DATABASE_URL != null) {
            Class.forName("org.postgresql.Driver");
            DATABASE = new PostgreConnection(Config.DATABASE_URL, Config.DATABASE_USER, Config.DATABASE_PASSWD, "playerdata");
        } else {
            LOG.error("Please configure the database first, before running Peelocity!");
        }

        registerEvents(SERVER.getEventManager());
        registerCommands(SERVER.getCommandManager());
        sendWelcome(SERVER.getConsoleCommandSource());
        LOG.info(Ansi.formattedString("Loaded all components, took " + (System.currentTimeMillis() - start) + "ms!", Ansi.GREEN));
    }

    public void registerEvents(@NotNull EventManager manager) {
        manager.register(this, new PlayerEvents());
        manager.register(this, new MessageLogging());
        manager.register(this, new Bans());
        manager.register(this, new Mutes());
    }

    public void registerCommands(@NotNull CommandManager manager) {
        manager.register("announce", Announcements.createAnnounceBrigadier());
        manager.register("ban", Bans.createBanBrigadier());
        manager.register("friend", FriendSystem.createFriendBrigadier());
        manager.register("hub", JoinLogic.createHubBrigadier(), "lobby", "leave");
        manager.register("join", JoinLogic.createJoinBrigadier(), "play");
        manager.register("kick", Kicks.createKickBrigadier());
        manager.register("message-history", UserUtil.createMessageHistoryBrigadier(), "msg-hist", "history");
        manager.register("msg", PrivateMessaging.createMsgBrigadier(), "pm");
        manager.register("mute", Mutes.createMuteBrigadier(), "timeout");
        manager.register("pardon", Bans.createPardonBrigadier(), "unban");
        manager.register("party", PartySystem.createPartyBrigadier(SERVER));
        manager.register("report", Reporting.createReportBrigadier(), "snitch");
        manager.register("staff", StaffChat.createStaffBrigadier(), "sc", "staff-chat");
        manager.register("unmute", Mutes.createUnmuteBrigadier(), "remove-timeout");
        manager.register("w", PrivateMessaging.createWBrigadier());

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

    static void sendWelcome(@NotNull ConsoleCommandSource console) {
        console.sendMessage(Component.text("    __   __  __", NamedTextColor.YELLOW));
        console.sendMessage(Component.text("   |__) |__ |__ Peelocity " + PEELOCITY_VERSION, NamedTextColor.YELLOW));
        console.sendMessage(Component.text("   |    |__ |__ https://marcpg.com/peelocity", NamedTextColor.YELLOW));
        console.sendMessage(Component.text("   Build: " + PEELOCITY_VERSION + "-" + PEELOCITY_BUILD_NUMBER + " (" + PEELOCITY_RELEASE_TYPE + ")", NamedTextColor.DARK_GRAY));
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) throws IOException {
        PlayerCache.saveCachedUsers();
    }
}

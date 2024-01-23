package com.marcpg.peelocity;

import com.google.inject.Inject;
import com.marcpg.peelocity.admin.Announcements;
import com.marcpg.peelocity.chat.MessageLogging;
import com.marcpg.peelocity.chat.PrivateMessaging;
import com.marcpg.peelocity.chat.StaffChat;
import com.marcpg.peelocity.moderation.*;
import com.marcpg.peelocity.social.FriendSystem;
import com.marcpg.peelocity.social.PartySystem;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.hectus.lang.Translation;
import net.hectus.sql.PostgreConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

@Plugin(
        id = "peelocity",
        name = "Peelocity",
        version = "0.1.2",
        description = "General purpose Velocity plugin for MarcPG's Minecraft projects, like SpellBound.",
        url = "https://marcpg.com/peelocity",
        authors = { "MarcPG" }
)
public class Peelocity {
    @SuppressWarnings("unused")
    public enum ReleaseType { ALPHA, BETA, SNAPSHOT, PRE, RELEASE }

    public static final ReleaseType PEELOCITY_RELEASE_TYPE = ReleaseType.BETA;
    public static final String PEELOCITY_VERSION = "0.1.2";
    public static final String PEELOCITY_BUILD_NUMBER = "1";

    public static Peelocity PLUGIN;
    public static ProxyServer SERVER;
    public static Logger LOG;
    public static Path DATA_DIRECTORY;
    public static PostgreConnection DATABASE;

    @Inject
    public Peelocity(@NotNull ProxyServer server, @NotNull Logger logger, @DataDirectory Path dataDirectory) throws IOException, SQLException, ClassNotFoundException {
        PLUGIN = this;
        SERVER = server;
        LOG = logger;
        DATA_DIRECTORY = dataDirectory;

        Class.forName("org.postgresql.Driver"); // Ensures that the driver is actually loaded
        DATABASE = new PostgreConnection(Config.DATABASE_URL, Config.DATABASE_USER, Config.DATABASE_PASSWD, "playerdata");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws IOException {
        UserCache.loadCachedUsers();
        Translation.load(new File(DATA_DIRECTORY.toFile(), "/lang/"));

        Config.saveDefaultConfig();
        Config.load();

        SERVER.getEventManager().register(this, new PlayerEvents());
        SERVER.getEventManager().register(this, new MessageLogging());
        SERVER.getEventManager().register(this, new Bans());
        SERVER.getEventManager().register(this, new Mutes());

        final CommandManager commandManager = SERVER.getCommandManager();
        commandManager.register("announce", Announcements.createAnnounceBrigadier());
        commandManager.register("report", Reporting.createComplexReportBrigadier(), "snitch");
        commandManager.register("message-history", UserUtil.createMessageHistoryBrigadier(), "msg-hist", "history");
        commandManager.register("ban", Bans.createBanBrigadier());
        commandManager.register("pardon", Bans.createPardonBrigadier(), "unban");
        commandManager.register("kick", Kicks.createKickBrigadier());
        commandManager.register("mute", Mutes.createMuteBrigadier(), "timeout");
        commandManager.register("unmute", Mutes.createUnmuteBrigadier(), "remove-timeout");
        commandManager.register("msg", PrivateMessaging.createMsgBrigadier(), "pm");
        commandManager.register("w", PrivateMessaging.createWBrigadier());
        commandManager.register("staff", StaffChat.createStaffBrigadier(), "sc", "staff-chat");
        commandManager.register("friend", FriendSystem.createFriendBrigadier());
        commandManager.register("party", PartySystem.createPartyBrigadier(SERVER));
        commandManager.register("join", JoinLogic.createJoinBrigadier(), "play");

        commandManager.register("ping", (SimpleCommand) invocation -> {
            Player source = (Player) invocation.source();
            long ping = source.getPing();
            int limitedPing = (int) Math.min(254, ping);
            int limitedPingPurple = (int) Math.min(508, ping);
            source.sendMessage(Translation.component(source.getEffectiveLocale(), "cmd.ping.info").color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC));
            source.sendMessage(Translation.component(source.getEffectiveLocale(), "cmd.ping.stat").append(Component.text(ping).color(TextColor.color(limitedPing, 254 - limitedPing, (ping > 260 ? limitedPingPurple - 254 : 0)))));
        }, "latency");

        commandManager.register("peelocity", (SimpleCommand) invocation -> {
            Player source = (Player) invocation.source();
            source.sendMessage(Component.text("Peelocity").decorate(TextDecoration.BOLD).append(Component.text(" " + PEELOCITY_VERSION + "-" + PEELOCITY_RELEASE_TYPE + " (" + PEELOCITY_BUILD_NUMBER + ")").decoration(TextDecoration.BOLD, false)).color(TextColor.color(0, 170, 170)));
            source.sendMessage(Translation.component(source.getEffectiveLocale(), "cmd.peelocity.info"));
        }, "velocity-plugin");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) throws IOException {
        UserCache.saveCachedUsers();
    }

    @Subscribe
    public EventTask onProxyPing(ProxyPingEvent event) {
        return EventTask.async(() -> event.setPing(event.getPing().asBuilder().version(new ServerPing.Version(765, Config.SERVERLIST_VERSION)).clearSamplePlayers().build()));
    }
}

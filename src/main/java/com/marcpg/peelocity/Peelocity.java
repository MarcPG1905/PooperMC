package com.marcpg.peelocity;

import com.google.inject.Inject;
import com.marcpg.peelocity.chat.MessageLogging;
import com.marcpg.peelocity.chat.PrivateMessaging;
import com.marcpg.peelocity.chat.StaffChat;
import com.marcpg.peelocity.social.FriendSystem;
import com.marcpg.peelocity.util.Config;
import com.marcpg.peelocity.util.Debugging;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "peelocity",
        name = "Peelocity",
        version = "0.0.7",
        description = "General purpose Velocity plugin for MarcPG's Minecraft projects, like SpellBound.",
        url = "https://marcpg.com/peelocity",
        authors = { "MarcPG" }
)
public class Peelocity {
    @SuppressWarnings("unused")
    public enum ReleaseType { ALPHA, BETA, SNAPSHOT, PRE, RELEASE }

    public static final ReleaseType PEELOCITY_RELEASE_TYPE = ReleaseType.ALPHA;
    public static final String PEELOCITY_VERSION = "0.0.7";
    public static final String PEELOCITY_BUILD_NUMBER = "7";

    public static ProxyServer SERVER;
    public static Logger LOG;
    public static Path DATA_DIRECTORY;

    @Inject
    public Peelocity(@NotNull ProxyServer server, @NotNull Logger logger, @DataDirectory Path dataDirectory) {
        Peelocity.SERVER = server;
        Peelocity.LOG = logger;
        Peelocity.DATA_DIRECTORY = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        SERVER.getEventManager().register(this, new PlayerEvents());
        SERVER.getEventManager().register(this, new MessageLogging());

        CommandManager commandManager = SERVER.getCommandManager();

        // ========== /DEBUG COMMAND ==========
        commandManager.register("debug", new Debugging());

        // =========== /MSG COMMAND ===========
        commandManager.register("msg", PrivateMessaging.createMsgBrigadier(SERVER), "pm", "w");

        // ======= /STAFF-CHAT COMMAND =======
        commandManager.register("staff", StaffChat.createStaffChatBrigadier(SERVER), "sc", "staff-chat");

        // ========= /FRIEND COMMAND =========
        commandManager.register("friend", FriendSystem.createFriendBrigadierCommand(SERVER));

        // ========== /PING COMMAND ==========
        commandManager.register("ping", (SimpleCommand) invocation -> {
            CommandSource source = invocation.source();
            long ping = ((Player) source).getPing();
            int limitedPing = (int) Math.min(254, ping);
            int limitedPingPurple = (int) Math.min(508, ping);
            source.sendMessage(Component.text("Please keep in mind that ping is only about 30 seconds accurate, due to technical limitations!").color(TextColor.color(160, 160, 160)).decorate(TextDecoration.ITALIC));
            source.sendMessage(Component.text("Your ping: ").append(Component.text(ping).color(TextColor.color(limitedPing, 254 - limitedPing, (ping > 260 ? limitedPingPurple - 254 : 0)))));
        }, "latency");

        // ======== /PEELOCITY COMMAND ========
        commandManager.register("peelocity", (SimpleCommand) invocation -> {
            CommandSource source = invocation.source();
            source.sendMessage(Component.text("Peelocity").decorate(TextDecoration.BOLD).append(Component.text(" " + PEELOCITY_VERSION + "-" + PEELOCITY_RELEASE_TYPE + " (" + PEELOCITY_BUILD_NUMBER + ")").decoration(TextDecoration.BOLD, false)).color(TextColor.color(0, 170, 170)));
            source.sendMessage(Component.text("Copyright 2023 MarcPG.COM. All rights reserved. \nFor requests regarding plugin use, contact me on Discord:"));
            source.sendMessage(Component.text("Contact: ").color(TextColor.color(0, 255, 0)).append(Component.text("me@marcpg.com").clickEvent(ClickEvent.openUrl("mailto:me@marcpg.com")).hoverEvent(HoverEvent.showText(Component.text("Click to send an email to me@marcpg.com")))));
        }, "velocity-plugin");

        // ========== /PLAY COMMAND ==========
        commandManager.register("play", (SimpleCommand) invocation -> {
            Player player = (Player) invocation.source();
            player.sendMessage(Component.text("Finding a match for you...", TextColor.color(255, 255, 0)));
            for (RegisteredServer regServer : SERVER.getAllServers()) {
                try {
                    ServerPing ping = regServer.ping().get(5, TimeUnit.SECONDS);
                    if (regServer.getServerInfo().getName().startsWith("wd") && regServer.getPlayersConnected().size() < 8 && ping != null) {
                        player.sendMessage(Component.text("Connecting you to a match...", TextColor.color(50, 230, 50)));

                        ConnectionRequestBuilder requestBuilder = player.createConnectionRequest(regServer);
                        SERVER.getScheduler().buildTask(this, requestBuilder::fireAndForget)
                                .delay(Duration.ZERO)
                                .schedule();

                        player.sendMessage(Component.text("Connected you successfully!", TextColor.color(0, 255, 0)));
                        return;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            player.sendMessage(Component.text("Couldn't find any match for you. Please try again later!", TextColor.color(255, 0, 0)));
        }, "game");

        Config.load();
    }
}

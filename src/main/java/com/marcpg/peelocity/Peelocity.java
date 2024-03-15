package com.marcpg.peelocity;

import com.google.inject.Inject;
import com.marcpg.color.Ansi;
import com.marcpg.lang.Translation;
import com.marcpg.peelocity.features.*;
import com.marcpg.peelocity.moderation.*;
import com.marcpg.peelocity.social.FriendSystem;
import com.marcpg.peelocity.social.PartySystem;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bstats.charts.SimplePie;
import org.bstats.velocity.Metrics;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@SuppressWarnings("unused")
@Plugin(
        id = "peelocity",
        name = "Peelocity",
        version = Peelocity.VERSION + "+build." + Peelocity.BUILD,
        description = "An all-in-one solution for Server networks. Everything from administration tools, to moderation utilities and database support.",
        url = "https://marcpg.com/peelocity",
        authors = { "MarcPG" },
        dependencies = { @Dependency(id = "signedvelocity", optional = true) }
)
public final class Peelocity {
    public static final String VERSION = "1.0.2";
    public static final int BUILD = 3;
    public static final UpdateChecker.Version CURRENT_VERSION = new UpdateChecker.Version(4, VERSION + "+build." + BUILD, "ERROR");

    public static Logger LOG;
    public static ProxyServer SERVER;
    public static Path DATA_DIR;
    public static Peelocity INSTANCE;

    @Inject private Metrics.Factory metricsFactory;

    private final List<String> commands = List.of("ban", "config", "friend", "hub", "join", "kick", "msg",
            "mute", "pardon", "party", "peelocity", "report", "staff", "unmute", "w", "whitelist", "msg-hist");

    @Inject
    public Peelocity(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory) {
        LOG = logger;
        SERVER = server;
        DATA_DIR = dataDirectory;
        INSTANCE = this;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws IOException {
        // Not sure if this even does anything, just making sure:
        if (Locale.getDefault() == null) Locale.setDefault(new Locale("en", "US"));

        long start = System.currentTimeMillis();

        SERVER.getChannelRegistrar().register(Joining.PLUGIN_MESSAGE_IDENTIFIER);

        ChatUtilities.signedVelocityInstalled = SERVER.getPluginManager().isLoaded("signedvelocity");

        Configuration.createDataDirectory();
        Configuration.load(Objects.requireNonNull(this.getClass().getResourceAsStream("/pee.yml")));

        this.metrics(this.metricsFactory.make(this, 21102));
        this.events(SERVER.getEventManager());
        this.commands(SERVER.getCommandManager());

        PlayerCache.load();
        UpdateChecker.checkUpdates();

        LOG.info(Ansi.green("Loaded all components, took " + (System.currentTimeMillis() - start) + "ms!"));

        this.sendWelcome();

        try {
            Translation.loadProperties(DATA_DIR.resolve("lang").toFile());
        } catch (IOException e) {
            LOG.error("The downloaded translations are corrupted or missing, so the translations couldn't be loaded!");
        }

        Path path = DATA_DIR.resolve(".no_setup");
        if (path.toFile().createNewFile()) {
            Files.setAttribute(path, "dos:hidden", true);
            LOG.info(Ansi.formattedString("Please consider checking out the Peelocity setup, by running Peelocity-?.jar as a java program.", Ansi.BRIGHT_BLUE, Ansi.BLINK));
            LOG.info(Ansi.formattedString("See further instructions on https://github.com/MarcPG1905/Peelocity#setup!", Ansi.BRIGHT_BLUE, Ansi.BLINK));
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) throws IOException {
        PlayerCache.save();
    }

    void sendWelcome() {
        LOG.info(Ansi.yellow("    __   __  __"));
        LOG.info(Ansi.yellow("   |__) |__ |__ Peelocity " + VERSION));
        LOG.info(Ansi.yellow("   |    |__ |__ https://marcpg.com/peelocity"));
        LOG.info(Ansi.formattedString("   Version: " + VERSION + "+build." + BUILD, Ansi.DARK_GRAY));
    }

    void metrics(@NotNull Metrics metrics) {
        LOG.info(Ansi.formattedString("Sending Metrics to bStats...", Ansi.DARK_GRAY));
        metrics.addCustomChart(new SimplePie("chat_utils", () -> String.valueOf(Configuration.chatUtilities.getBoolean("enabled"))));
        metrics.addCustomChart(new SimplePie("server_list", () -> String.valueOf(Configuration.serverList.getBoolean("enabled"))));
        metrics.addCustomChart(new SimplePie("storage_method", () -> Configuration.storageType.name().toLowerCase()));
        metrics.addCustomChart(new SimplePie("translations", () -> String.valueOf(Configuration.translations)));
        metrics.addCustomChart(new SimplePie("whitelist", () -> String.valueOf(Configuration.whitelist)));
    }

    void events(@NotNull EventManager manager) {
        LOG.info(Ansi.formattedString("Registering Events...", Ansi.DARK_GRAY));
        manager.register(this, new PartySystem());
        manager.register(this, new Joining());
        manager.register(this, new Banning());
        manager.register(this, new Muting());

        if (Configuration.chatUtilities.getBoolean("enabled")) manager.register(this, new ChatUtilities());
        if (Configuration.serverList.getBoolean("enabled")) manager.register(this, new ServerList());
        if (Configuration.whitelist) manager.register(this, new Whitelist());
        if (MessageHistory.enabled) manager.register(this, new MessageHistory());

        manager.register(this, LoginEvent.class, PostOrder.LAST, event -> PlayerCache.PLAYERS.put(event.getPlayer().getUniqueId(), event.getPlayer().getUsername()));
    }

    void commands(@NotNull CommandManager manager) {
        LOG.info(Ansi.formattedString("Registering Commands...", Ansi.DARK_GRAY));

        manager.register("ban", Banning.banCommand());
        manager.register("config", Configuration.command(), "peelocity-configuration");
        manager.register("friend", FriendSystem.command());
        manager.register("hub", Joining.hubCommand(), "lobby");
        manager.register("join", Joining.joinCommand(), "play");
        manager.register("kick", Kicking.command());
        manager.register("msg", PrivateMessaging.msgCommand(), "dm", "tell", "whisper");
        manager.register("mute", Muting.muteCommand());
        manager.register("pardon", Banning.pardonCommand(), "unban");
        manager.register("party", PartySystem.command());
        manager.register("peelocity", this.command(), "velocity-plugin");
        manager.register("report", Reporting.command());
        manager.register("staff", StaffChat.command(), "staff-chat", "sc");
        manager.register("unmute", Muting.unmuteCommand());
        manager.register("w", PrivateMessaging.wCommand(), "reply");

        if (Configuration.whitelist) manager.register("whitelist", Whitelist.command());
        if (MessageHistory.enabled) manager.register("msg-hist", MessageHistory.command(), "message-history", "chat-activity");
    }

    void reload() throws IOException {
        this.commands.forEach(SERVER.getCommandManager()::unregister);
        SERVER.getEventManager().unregisterListeners(this);

        ChatUtilities.signedVelocityInstalled = SERVER.getPluginManager().isLoaded("signedvelocity");

        Configuration.createDataDirectory();
        Configuration.load(Objects.requireNonNull(this.getClass().getResourceAsStream("/pee.yml")));

        this.metrics(this.metricsFactory.make(this, 21102));
        this.events(SERVER.getEventManager());
        this.commands(SERVER.getCommandManager());

        UpdateChecker.checkUpdates();

        try {
            Translation.loadProperties(DATA_DIR.resolve("lang").toFile());
        } catch (IOException e) {
            Peelocity.LOG.error("The downloaded translations are corrupted or missing, so the translations couldn't be loaded!");
            throw new IOException();
        }
    }

    @Contract(" -> new")
    @NotNull BrigadierCommand command() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("peelocity")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    Locale l = source instanceof Player player ? player.getEffectiveLocale() : Locale.getDefault();
                    source.sendMessage(Component.text("Peelocity ").decorate(TextDecoration.BOLD).append(Component.text(VERSION + "+build." + BUILD).decoration(TextDecoration.BOLD, false)).color(NamedTextColor.YELLOW));
                    source.sendMessage(Translation.component(l, "cmd.peelocity.info"));
                    return 1;
                })
                .then(LiteralArgumentBuilder.<CommandSource>literal("reload")
                        .requires(source -> source.hasPermission("pee.admin"))
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            Locale l = source instanceof Player player ? player.getEffectiveLocale() : Locale.getDefault();

                            try {
                                this.reload();
                            } catch (IOException e) {
                                source.sendMessage(Translation.component(l, "cmd.reload.error").color(NamedTextColor.RED));
                            } finally {
                                source.sendMessage(Translation.component(l, "cmd.reload.confirm").color(NamedTextColor.GREEN));
                            }

                            return 1;
                        })
                )
        );
    }
}

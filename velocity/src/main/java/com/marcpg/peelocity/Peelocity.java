package com.marcpg.peelocity;

import com.alessiodp.libby.VelocityLibraryManager;
import com.google.inject.Inject;
import com.marcpg.common.Configuration;
import com.marcpg.common.Platform;
import com.marcpg.common.Pooper;
import com.marcpg.common.features.MessageLogging;
import com.marcpg.common.logger.SLF4JLogger;
import com.marcpg.common.storage.Storage;
import com.marcpg.common.util.UpdateChecker;
import com.marcpg.libpg.color.Ansi;
import com.marcpg.libpg.lang.Translation;
import com.marcpg.peelocity.common.VelocityAsyncScheduler;
import com.marcpg.peelocity.common.VelocityFaviconHandler;
import com.marcpg.peelocity.features.VelocityChatUtilities;
import com.marcpg.peelocity.features.VelocityPrivateMessaging;
import com.marcpg.peelocity.features.VelocityServerList;
import com.marcpg.peelocity.features.VelocityWhitelist;
import com.marcpg.peelocity.moderation.*;
import com.marcpg.peelocity.social.VelocityFriendSystem;
import com.marcpg.peelocity.social.VelocityPartySystem;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static com.marcpg.common.Configuration.doc;

@Plugin(
        id = "pooper",
        name = "PooperMC",
        version = Pooper.VERSION + "+build." + Pooper.BUILD,
        description = "An all-in-one solution for servers. Everything from administration tools, to moderation utilities and database support.",
        url = "https://marcpg.com/pooper/velocity",
        authors = { "MarcPG" },
        dependencies = { @Dependency(id = "signedvelocity", optional = true) }
)
public final class Peelocity {
    static { Pooper.PLATFORM = Platform.VELOCITY; }

    private static final List<String> commands = List.of("ban", "config", "friend", "hub", "join", "kick", "msg",
            "mute", "pardon", "party", "peelocity", "report", "staff", "unmute", "w", "whitelist", "msg-hist");

    public static ProxyServer SERVER;
    public static Peelocity INSTANCE;

    @Inject private Metrics.Factory metricsFactory;

    @Inject
    public Peelocity(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory) {
        SERVER = server;
        INSTANCE = this;
        Pooper.LOG = new SLF4JLogger(logger);
        Pooper.DATA_DIR = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent ignoredEvent) throws IOException {
        long start = System.currentTimeMillis();

        if (Locale.getDefault() == null) Locale.setDefault(new Locale("en", "US"));

        SERVER.getChannelRegistrar().register(Joining.PLUGIN_MESSAGE_IDENTIFIER);

        VelocityChatUtilities.signedVelocityInstalled = SERVER.getPluginManager().isLoaded("signedvelocity");

        Configuration.createFileTree();
        Configuration.load(
                new VelocityFaviconHandler(),
                new VelocityLibraryManager<>(this, (Logger) Pooper.LOG.getNativeLogger(), Pooper.DATA_DIR, SERVER.getPluginManager()),
                new VelocityAsyncScheduler(this, SERVER.getScheduler())
        );

        this.metrics(metricsFactory.make(this, Pooper.METRICS_ID));
        this.events(SERVER.getEventManager());
        this.commands(SERVER.getCommandManager());

        PlayerCache.load();
        UpdateChecker.checkUpdates();

        Pooper.LOG.info(Ansi.green("Loaded all components, took " + (System.currentTimeMillis() - start) + "ms!"));

        sendWelcome();

        try {
            Translation.loadProperties(Pooper.DATA_DIR.resolve("lang").toFile());
        } catch (IOException e) {
            Pooper.LOG.error("The downloaded translations are corrupted or missing, so the translations couldn't be loaded!");
        }

        Path path = Pooper.DATA_DIR.resolve(".no_setup");
        if (path.toFile().createNewFile()) {
            Files.setAttribute(path, "dos:hidden", true);
            Pooper.LOG.info(Ansi.formattedString("Please consider checking out the Peelocity setup, by running Peelocity-?.jar as a java program.", Ansi.BRIGHT_BLUE, Ansi.BLINK));
            Pooper.LOG.info(Ansi.formattedString("See further instructions on https://github.com/MarcPG1905/Peelocity#setup!", Ansi.BRIGHT_BLUE, Ansi.BLINK));
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent ignoredEvent) throws IOException {
        PlayerCache.save();
    }

    void sendWelcome() {
        Pooper.LOG.info(Ansi.yellow("    __   __  __"));
        Pooper.LOG.info(Ansi.yellow("   |__) |__ |__ PooperMC for Velocity (Peelocity) " + Pooper.VERSION));
        Pooper.LOG.info(Ansi.yellow("   |    |__ |__ https://marcpg.com/pooper/velocity"));
        Pooper.LOG.info(Ansi.gray("   Version: " + Pooper.VERSION + "+build." + Pooper.BUILD));
    }

    void metrics(@NotNull Metrics metrics) {
        Pooper.LOG.info(Ansi.gray("Sending Metrics to bStats..."));
        metrics.addCustomChart(new SimplePie("chat_utils", () -> String.valueOf(Configuration.chatUtilities.getBoolean("enabled"))));
        metrics.addCustomChart(new SimplePie("server_list", () -> String.valueOf(doc.getBoolean("server-list.enabled"))));
        metrics.addCustomChart(new SimplePie("storage_method", () -> Storage.storageType.name().toLowerCase()));
        metrics.addCustomChart(new SimplePie("translations", () -> String.valueOf(Configuration.downloadTranslations)));
        metrics.addCustomChart(new SimplePie("whitelist", () -> String.valueOf(Configuration.whitelist)));
    }

    void events(@NotNull EventManager manager) {
        Pooper.LOG.info(Ansi.gray("Registering Events..."));
        manager.register(this, new VelocityPartySystem());
        manager.register(this, new Joining());
        manager.register(this, new VelocityBanning());
        manager.register(this, new VelocityMuting());

        if (Configuration.chatUtilities.getBoolean("enabled")) manager.register(this, new VelocityChatUtilities());
        if (doc.getBoolean("server-list.enabled")) manager.register(this, new VelocityServerList());
        if (Configuration.whitelist) manager.register(this, new VelocityWhitelist());
        if (MessageLogging.enabled) manager.register(this, new BasicEvents());

        manager.register(this, LoginEvent.class, PostOrder.LAST, event -> PlayerCache.PLAYERS.put(event.getPlayer().getUniqueId(), event.getPlayer().getUsername()));
    }

    void commands(@NotNull CommandManager manager) {
        Pooper.LOG.info(Ansi.gray("Registering Commands..."));

        manager.register("ban", VelocityBanning.banCommand());
        manager.register("config", configCommand(), "peelocity-configuration", "pooper-velocity-configuration");
        manager.register("friend", VelocityFriendSystem.command());
        manager.register("hub", Joining.hubCommand(), "lobby");
        manager.register("join", Joining.joinCommand(), "play");
        manager.register("kick", VelocityKicking.command());
        manager.register("msg", VelocityPrivateMessaging.msgCommand(), "dm", "tell", "whisper");
        manager.register("mute", VelocityMuting.muteCommand());
        manager.register("pardon", VelocityBanning.pardonCommand(), "unban");
        manager.register("party", VelocityPartySystem.command());
        manager.register("peelocity", command(), "velocity-plugin", "pooper-velocity");
        manager.register("report", VelocityReporting.command(), "snitch");
        manager.register("staff", VelocityStaffChat.command(), "staff-chat", "sc");
        manager.register("unmute", VelocityMuting.unmuteCommand());
        manager.register("w", VelocityPrivateMessaging.wCommand(), "reply");

        if (Configuration.whitelist) manager.register("whitelist", VelocityWhitelist.command());
        if (MessageLogging.enabled) manager.register("msg-hist", Commands.msgHist(), "message-history", "chat-activity");
    }

    void reload() throws IOException {
        commands.forEach(SERVER.getCommandManager()::unregister);
        SERVER.getEventManager().unregisterListeners(this);

        VelocityChatUtilities.signedVelocityInstalled = SERVER.getPluginManager().isLoaded("signedvelocity");

        Configuration.createFileTree();
        Configuration.load(
                new VelocityFaviconHandler(),
                new VelocityLibraryManager<>(this, (Logger) Pooper.LOG.getNativeLogger(), Pooper.DATA_DIR, SERVER.getPluginManager()),
                new VelocityAsyncScheduler(this, SERVER.getScheduler())
        );

        this.metrics(metricsFactory.make(this, Pooper.METRICS_ID));
        this.events(SERVER.getEventManager());
        this.commands(SERVER.getCommandManager());

        UpdateChecker.checkUpdates();

        try {
            Translation.loadProperties(Pooper.DATA_DIR.resolve("lang").toFile());
        } catch (IOException e) {
            Pooper.LOG.error("The downloaded translations are corrupted or missing, so the translations couldn't be loaded!");
            throw new IOException();
        }
    }

    @NotNull BrigadierCommand command() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("peelocity")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    Locale l = source instanceof Player player ? player.getEffectiveLocale() : Locale.getDefault();
                    source.sendMessage(Component.text("Peelocity ").decorate(TextDecoration.BOLD).append(Component.text(Pooper.VERSION + "+build." + Pooper.BUILD).decoration(TextDecoration.BOLD, false)).color(NamedTextColor.YELLOW));
                    source.sendMessage(Translation.component(l, "cmd.peelocity.info"));
                    return 1;
                })
                .then(LiteralArgumentBuilder.<CommandSource>literal("reload")
                        .requires(source -> source.hasPermission("poo.admin"))
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

    @NotNull BrigadierCommand configCommand() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("config")
                .requires(source -> source.hasPermission("poo.admin"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("entry", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Configuration.routes.forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(LiteralArgumentBuilder.<CommandSource>literal("get")
                                .executes(context -> {
                                    CommandSource source = context.getSource();
                                    Locale l = source instanceof Player player ? player.getEffectiveLocale() : Locale.getDefault();
                                    String route = context.getArgument("entry", String.class);

                                    if (doc.isList(route)) {
                                        source.sendMessage(Translation.component(l, "cmd.config.get.list", route).color(NamedTextColor.YELLOW));
                                        doc.getList(route).forEach(o -> source.sendMessage(Component.text("- " + o.toString())));
                                    } else if (doc.contains(route)) {
                                        source.sendMessage(Translation.component(l, "cmd.config.get.object", route, doc.getString(route)).color(NamedTextColor.YELLOW));
                                    } else {
                                        source.sendMessage(Translation.component(l, "cmd.config.key_not_existing", route).color(NamedTextColor.RED));
                                    }
                                    return 1;
                                })
                        )
                        .then(LiteralArgumentBuilder.<CommandSource>literal("set")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("value", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> {
                                            if (doc.isBoolean(context.getArgument("entry", String.class))) {
                                                builder.suggest("true");
                                                builder.suggest("false");
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            CommandSource source = context.getSource();
                                            Locale l = source instanceof Player player ? player.getEffectiveLocale() : Locale.getDefault();
                                            String route = context.getArgument("entry", String.class);

                                            if (!doc.contains(route)) {
                                                source.sendMessage(Translation.component(l, "cmd.config.key_not_existing", route).color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            String stringValue = context.getArgument("value", String.class);

                                            if (doc.isSection(route) || doc.isList(route)) {
                                                source.sendMessage(Translation.component(l, "cmd.config.set.section_list").color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            if (doc.isBoolean(route))
                                                doc.set(route, Boolean.parseBoolean(stringValue));
                                            else if (doc.isInt(route))
                                                doc.set(route, Integer.parseInt(stringValue));
                                            else
                                                doc.set(route, stringValue);

                                            try {
                                                doc.save();
                                            } catch (IOException e) {
                                                source.sendMessage(Translation.component(l, "cmd.config.error").color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            source.sendMessage(Translation.component(l, "cmd.config.set.confirm", route, stringValue).color(NamedTextColor.YELLOW));
                                            source.sendMessage(Translation.component(l, "cmd.config.reload_to_apply").color(NamedTextColor.GRAY));

                                            return 1;
                                        })
                                )
                        )
                        .then(LiteralArgumentBuilder.<CommandSource>literal("add")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("value", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            CommandSource source = context.getSource();
                                            Locale l = source instanceof Player player ? player.getEffectiveLocale() : Locale.getDefault();
                                            String route = context.getArgument("entry", String.class);

                                            if (!doc.contains(route)) {
                                                source.sendMessage(Translation.component(l, "cmd.config.key_not_existing", route).color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            List<String> list = doc.getStringList(route);
                                            list.add(context.getArgument("value", String.class));
                                            doc.set(route, list);

                                            try {
                                                doc.save();
                                            } catch (IOException e) {
                                                source.sendMessage(Translation.component(l, "cmd.config.error").color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            source.sendMessage(Translation.component(l, "cmd.config.add.confirm", route, context.getArgument("value", String.class)).color(NamedTextColor.YELLOW));
                                            source.sendMessage(Translation.component(l, "cmd.config.reload_to_apply").color(NamedTextColor.GRAY));

                                            return 1;
                                        })
                                )
                        )
                )
                .build()
        );
    }
}

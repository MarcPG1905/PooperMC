package com.marcpg.peelocity;

import com.alessiodp.libby.VelocityLibraryManager;
import com.google.inject.Inject;
import com.marcpg.common.Configuration;
import com.marcpg.common.Platform;
import com.marcpg.common.Pooper;
import com.marcpg.common.logger.SLF4JLogger;
import com.marcpg.libpg.lang.Translation;
import com.marcpg.peelocity.common.VelocityAsyncScheduler;
import com.marcpg.peelocity.common.VelocityFaviconHandler;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
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
public final class PeelocityPlugin {
    static { Pooper.PLATFORM = Platform.VELOCITY; }

    public static ProxyServer SERVER;
    public static PeelocityPlugin INSTANCE;

    @Inject
    public PeelocityPlugin(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory) {
        SERVER = server;
        INSTANCE = this;
        Pooper.LOG = new SLF4JLogger(logger);
        Pooper.DATA_DIR = dataDirectory;
        Pooper.SCHEDULER = new VelocityAsyncScheduler(this, SERVER.getScheduler());
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent ignoredEvent) {
        try { // The actual startup logic:
            new Peelocity(this);
            Pooper.INSTANCE.startup(new VelocityFaviconHandler(), new VelocityLibraryManager<>(this, (Logger) Pooper.LOG.getNativeLogger(), Pooper.DATA_DIR, SERVER.getPluginManager()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent ignoredEvent) {
        Pooper.INSTANCE.shutdown();
    }

    public static @NotNull BrigadierCommand command() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("peelocity")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    Locale l = source instanceof Player player ? player.getEffectiveLocale() : Locale.getDefault();
                    source.sendMessage(Component.text("Peelocity ").decorate(TextDecoration.BOLD).append(Component.text(Pooper.VERSION + "+build." + Pooper.BUILD).decoration(TextDecoration.BOLD, false)).color(NamedTextColor.YELLOW));
                    source.sendMessage(Translation.component(l, "license"));
                    return 1;
                })
                .then(LiteralArgumentBuilder.<CommandSource>literal("reload")
                        .requires(source -> source.hasPermission("poo.admin"))
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            Locale l = source instanceof Player player ? player.getEffectiveLocale() : Locale.getDefault();

                            try {
                                Pooper.INSTANCE.loadBasic(Configuration.serverListFavicons, new VelocityLibraryManager<>(PeelocityPlugin.INSTANCE, (Logger) Pooper.LOG.getNativeLogger(), Pooper.DATA_DIR, SERVER.getPluginManager()));
                                source.sendMessage(Translation.component(l, "cmd.reload.confirm").color(NamedTextColor.GREEN));
                            } catch (IOException e) {
                                source.sendMessage(Translation.component(l, "cmd.reload.error").color(NamedTextColor.RED));
                            }

                            return 1;
                        })
                )
        );
    }

    public static @NotNull BrigadierCommand configCommand() {
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
                                            if (doc.isSection(route) || doc.isList(route)) {
                                                source.sendMessage(Translation.component(l, "cmd.config.set.section_list").color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            String value = context.getArgument("value", String.class);

                                            if (doc.isBoolean(route)) {
                                                doc.set(route, Boolean.parseBoolean(value));
                                            } else if (doc.isInt(route)) {
                                                doc.set(route, Integer.parseInt(value));
                                            } else {
                                                doc.set(route, value);
                                            }

                                            try {
                                                doc.save();
                                            } catch (IOException e) {
                                                source.sendMessage(Translation.component(l, "cmd.config.error").color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            source.sendMessage(Translation.component(l, "cmd.config.set.confirm", route, value).color(NamedTextColor.YELLOW));
                                            source.sendMessage(Translation.component(l, "cmd.config.reload_to_apply", "peelocity").color(NamedTextColor.GRAY));

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

                                            source.sendMessage(Translation.component(l, "cmd.config.add.confirm", context.getArgument("value", String.class), route).color(NamedTextColor.YELLOW));
                                            source.sendMessage(Translation.component(l, "cmd.config.reload_to_apply", "peelocity").color(NamedTextColor.GRAY));

                                            return 1;
                                        })
                                )
                        )
                        .then(LiteralArgumentBuilder.<CommandSource>literal("remove")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("value", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            CommandSource source = context.getSource();
                                            Locale l = source instanceof Player player ? player.getEffectiveLocale() : Locale.getDefault();
                                            String route = context.getArgument("entry", String.class);
                                            String arg = context.getArgument("value", String.class);

                                            if (!doc.contains(route)) {
                                                source.sendMessage(Translation.component(l, "cmd.config.key_not_existing", route).color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            List<String> list = doc.getStringList(route);
                                            if (!list.contains(arg)) {
                                                source.sendMessage(Translation.component(l, "cmd.config.remove.not_containing", arg, route).color(NamedTextColor.RED));
                                                return 1;
                                            }
                                            list.remove(context.getArgument("value", String.class));
                                            doc.set(route, list);

                                            try {
                                                doc.save();
                                            } catch (IOException e) {
                                                source.sendMessage(Translation.component(l, "cmd.config.error").color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            source.sendMessage(Translation.component(l, "cmd.config.remove.confirm", arg, route).color(NamedTextColor.YELLOW));
                                            source.sendMessage(Translation.component(l, "cmd.config.reload_to_apply", "peelocity").color(NamedTextColor.GRAY));

                                            return 1;
                                        })
                                )
                        )
                )
                .build()
        );
    }
}

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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent ignoredEvent) {
        Pooper.INSTANCE.shutdown();
    }
}

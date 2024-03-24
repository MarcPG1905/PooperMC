package com.marcpg.peelocity;

import com.alessiodp.libby.VelocityLibraryManager;
import com.marcpg.libpg.color.Ansi;
import com.marcpg.libpg.data.database.sql.SQLConnection;
import com.marcpg.libpg.lang.Translation;
import com.marcpg.libpg.web.Downloads;
import com.marcpg.libpg.web.discord.Webhook;
import com.marcpg.peelocity.features.ServerList;
import com.marcpg.common.Pooper;
import com.marcpg.common.features.MessageLogging;
import com.marcpg.common.storage.DatabaseStorage;
import com.marcpg.common.storage.Storage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.Favicon;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.route.Route;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.marcpg.libpg.data.database.sql.SQLConnection.DatabaseType.*;

public final class Configuration {
    private static final List<SQLConnection.DatabaseType> ALLOWED_DATABASES = List.of(POSTGRESQL, MYSQL, MARIADB, MS_SQL_SERVER, ORACLE);

    public static YamlDocument doc;
    public static List<String> routes;

    public static Map<String, Integer> gamemodes;
    public static boolean translations;
    public static boolean globalChat;
    public static boolean whitelist;

    public static Section chatUtilities;
    public static Section serverList;

    public static void createDataDirectory() throws IOException {
        Files.createDirectories(Pooper.DATA_DIR.resolve("lang"));
        Files.createDirectories(Pooper.DATA_DIR.resolve("message-history"));
        try {
            Files.createFile(Pooper.DATA_DIR.resolve("playercache"));
        } catch (FileAlreadyExistsException ignored) {}
    }

    public static void load(@NotNull InputStream config) throws IOException {
        Pooper.LOG.info(Ansi.gray("Loading the Configuration (config.yml)..."));

        doc = YamlDocument.create(
                Pooper.DATA_DIR.resolve("config.yml").toFile(),
                config,
                GeneralSettings.DEFAULT,
                LoaderSettings.builder().setAutoUpdate(true).build(),
                DumperSettings.DEFAULT,
                UpdaterSettings.builder()
                        .setVersioning(new BasicVersioning("version"))
                        .addIgnoredRoute("6", Route.fromString("gamemodes"))
                        .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS)
                        .build()
        );
        routes = doc.getRoutesAsStrings(true).stream().filter(r -> !doc.isSection(r)).toList();

        try {
            Pooper.MOD_WEBHOOK = new Webhook(new URL(doc.getString("moderator-webhook")));
        } catch (MalformedURLException e) {
            Pooper.LOG.warn("The `moderator-webhook` is not properly configured! You can safely ignore this if you don't have a webhook.");
        }

        translations = doc.getBoolean("enable-translations");
        globalChat = doc.getBoolean("global-chat");
        whitelist = doc.getBoolean("whitelist-enabled");
        chatUtilities = doc.getSection("chatutility");
        serverList = doc.getSection("server-list");
        gamemodes = doc.getSection("gamemodes").getStringRouteMappedValues(false).entrySet().stream()
                .filter(e -> e.getValue() instanceof Integer)
                .filter(e -> !(e.getKey().equals("game1") || e.getKey().equals("game2")))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (Integer) e.getValue()));

        MessageLogging.enabled = doc.getBoolean("message-logging.enabled");
        MessageLogging.maxHistory = doc.getInt("message-logging.max-history");

        if (serverList.getBoolean("enabled")) {
            ServerList.motd = serverList.getBoolean("custom-motd");
            if (ServerList.motd) ServerList.motdList = serverList.getStringList("custom-motd-messages").stream()
                    .map(s -> MiniMessage.miniMessage().deserialize(s))
                    .toList();

            ServerList.favicon = serverList.getBoolean("custom-favicon");
            if (ServerList.favicon) ServerList.faviconList = serverList.getStringList("custom-favicon-urls").stream()
                    .map(s -> {
                        try {
                            return Favicon.create(ImageIO.read(new URI(s).toURL()));
                        } catch (IOException | URISyntaxException e) {
                            throw new RuntimeException(e);
                        } catch (IllegalArgumentException e) {
                            Pooper.LOG.error("One or more of the provided favicons is not 64x64 pixels!");
                            return null;
                        }
                    })
                    .toList();

            ServerList.showMaxPlayers = serverList.getInt("show-max-players");
            ServerList.showCurrentPlayers = serverList.getInt("show-current-players");
        }

        try {
            Storage.storageType = Storage.StorageType.valueOf(doc.getString("storage-method").toUpperCase());
        } catch (IllegalArgumentException e) {
            Storage.storageType = Storage.StorageType.YAML;
            Pooper.LOG.warn("The specified storage type is invalid! Using the default (yaml) now.");
        }

        if (Storage.storageType == Storage.StorageType.DATABASE) {
            try {
                DatabaseStorage.TYPE = SQLConnection.DatabaseType.valueOf(doc.getString("database.type").toUpperCase());

                if (!ALLOWED_DATABASES.contains(DatabaseStorage.TYPE)) {
                    DatabaseStorage.TYPE = SQLConnection.DatabaseType.POSTGRESQL;
                    Pooper.LOG.warn("The specified database type is invalid! Using the default (postgresql) now.");
                }
            } catch (IllegalArgumentException e) {
                DatabaseStorage.TYPE = SQLConnection.DatabaseType.POSTGRESQL;
                Pooper.LOG.warn("The specified database type is invalid! Using the default (postgresql) now.");
            }

            DatabaseStorage.ADDRESS = doc.getString("database.address");
            DatabaseStorage.PORT = doc.getInt("database.port");
            DatabaseStorage.NAME = doc.getString("database.database");
            DatabaseStorage.USERNAME = doc.getString("database.user");
            DatabaseStorage.PASSWORD = doc.getString("database.passwd");

            if (DatabaseStorage.USERNAME.equals(Objects.requireNonNull(doc.getDefaults()).getString("database.user")) ||
                    DatabaseStorage.PASSWORD.equals(doc.getDefaults().getString("database.passwd"))) {
                Pooper.LOG.error("Please configure the database user and password, before running Peelocity!");
            } else {
                DatabaseStorage.loadDependency(new VelocityLibraryManager<>(Peelocity.INSTANCE, Pooper.LOG, Pooper.DATA_DIR, Peelocity.SERVER.getPluginManager()));
            }
        }

        if (translations) {
            new Thread(new TranslationDownloadTask()).start();
        }
    }

    @Contract(" -> new")
    public static @NotNull BrigadierCommand command() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("config")
                .requires(source -> source.hasPermission("poo.admin"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("entry", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            routes.forEach(builder::suggest);
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

    private final static class TranslationDownloadTask implements Runnable {
        private final Path langFolder = Pooper.DATA_DIR.resolve("lang");
        private static final int MAX_RETRIES = 3;

        @Override
        public void run() {
            int attempt = 1;
            while (attempt <= MAX_RETRIES) {
                try {
                    Downloads.simpleDownload(new URI("https://marcpg.com/poopermc/translations/available_locales").toURL(), this.langFolder.resolve("available_locales.temp").toFile());
                    for (String locale : Files.readAllLines(this.langFolder.resolve("available_locales.temp"))) {
                        Downloads.simpleDownload(new URI("https://marcpg.com/poopermc/translations/" + locale).toURL(), this.langFolder.resolve(locale).toFile());
                    }
                    Files.deleteIfExists(this.langFolder.resolve("available_locales.temp"));
                    return;
                } catch (Exception e) {
                    if (attempt == MAX_RETRIES) {
                        Pooper.LOG.error("Translation download failed. The maximum amount of retries (" + MAX_RETRIES + ") has been reached!");
                    } else {
                        Pooper.LOG.warn("Translation download failed on attempt " + attempt + ", retrying in 3 seconds...");
                        try {
                            this.wait(3000); // Wait for 3s before retrying
                        } catch (InterruptedException ignored) {}
                    }
                    attempt++;
                }
            }
            Pooper.LOG.info("Downloaded and loaded all recent translations!");
        }
    }
}

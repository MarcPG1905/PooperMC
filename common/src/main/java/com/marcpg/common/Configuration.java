package com.marcpg.common;

import com.alessiodp.libby.LibraryManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.marcpg.common.features.MessageLogging;
import com.marcpg.common.platform.FaviconHandler;
import com.marcpg.common.storage.DatabaseStorage;
import com.marcpg.common.storage.Storage;
import com.marcpg.libpg.color.Ansi;
import com.marcpg.libpg.data.database.sql.SQLConnection;
import com.marcpg.libpg.lang.Translation;
import com.marcpg.libpg.web.discord.Webhook;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.route.Route;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static com.marcpg.libpg.data.database.sql.SQLConnection.DatabaseType.*;

public final class Configuration {
    private static final List<SQLConnection.DatabaseType> SUPPORTED_DATABASES = List.of(POSTGRESQL, MYSQL, MARIADB, MS_SQL_SERVER, ORACLE);

    public static YamlDocument doc;
    public static List<String> routes;

    public static Webhook modWebhook;
    public static Map<String, Integer> gamemodes;
    public static boolean downloadTranslations;

    public static Section chatUtilities;

    // ++++++++++++ Proxy ++++++++++++
    public static boolean globalChat;
    public static boolean whitelist;
    // ------------ Proxy ------------

    // ++++++++++++ Server List ++++++++++++
    public static boolean serverListMotd;
    public static List<Component> serverListMotdList;
    public static boolean serverListFavicon;
    public static FaviconHandler<?> serverListFavicons;
    public static int serverListShowCurrentPlayers;
    public static int serverListShowMaxPlayers;
    // ------------ Server List ------------

    public static void createFileTree() throws IOException {
        Files.createDirectories(Pooper.DATA_DIR.resolve("message-history"));
        try {
            Files.createFile(Pooper.DATA_DIR.resolve("playercache"));
        } catch (FileAlreadyExistsException ignored) {}
    }

    public static void load(FaviconHandler<?> faviconHandler, LibraryManager libraryManager) throws IOException, URISyntaxException, InterruptedException {
        Pooper.LOG.info(Ansi.gray("Loading the PooperMC Configuration..."));

        doc = YamlDocument.create(
                Pooper.DATA_DIR.resolve("config.yml").toFile(),
                Pooper.PLATFORM.configResource(),
                GeneralSettings.DEFAULT,
                LoaderSettings.builder().setAutoUpdate(true).build(),
                DumperSettings.DEFAULT,
                UpdaterSettings.builder()
                        .setVersioning(new BasicVersioning("version"))
                        .addIgnoredRoute("6", Route.fromString("gamemodes")) // Proxy-Only!
                        .addIgnoredRoute("7", Route.fromString("gamemodes")) // Proxy-Only!
                        .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS)
                        .build()
        );
        routes = doc.getRoutesAsStrings(true).stream()
                .filter(r -> !doc.isSection(r))
                .toList();

        // moderator-webhook
        try {
            Configuration.modWebhook = new Webhook(new URL(Objects.requireNonNull(doc.getString("moderator-webhook"))));
        } catch (Exception e) {
            Pooper.LOG.warn("The `moderator-webhook` is not properly configured! You can safely ignore this if you don't have a webhook.");
        }

        // enable-translations
        downloadTranslations = doc.getBoolean("enable-translations");

        // Proxy-Only Stuff
        if (Pooper.PLATFORM == Platform.VELOCITY) {
            // gamemodes
            gamemodes = doc.getSection("gamemodes").getStringRouteMappedValues(false).entrySet().stream()
                    .filter(e -> e.getValue() instanceof Integer)
                    .filter(e -> !(e.getKey().equals("game1") || e.getKey().equals("game2")))
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> (Integer) e.getValue()));
            // global-chat
            globalChat = doc.getBoolean("global-chat");
            // whitelist-enabled
            whitelist = doc.getBoolean("whitelist-enabled");
        }

        // storage-method
        try {
            Storage.storageType = Storage.StorageType.valueOf(doc.getString("storage-method").toUpperCase());
        } catch (IllegalArgumentException e) {
            Storage.storageType = Storage.StorageType.YAML;
            Pooper.LOG.warn("The specified storage type is invalid! Using the default (yaml) now.");
        }

        // Database Configuration
        if (Storage.storageType == Storage.StorageType.DATABASE) {
            // database.type
            try {
                DatabaseStorage.type = SQLConnection.DatabaseType.valueOf(doc.getString("database.type").toUpperCase());
                if (!SUPPORTED_DATABASES.contains(DatabaseStorage.type))
                    throw new IllegalArgumentException();
            } catch (IllegalArgumentException e) {
                DatabaseStorage.type = SQLConnection.DatabaseType.POSTGRESQL;
                Pooper.LOG.warn("The specified database type is invalid! Using the default (postgresql) now.");
            }

            DatabaseStorage.address = doc.getString("database.address");
            DatabaseStorage.port = doc.getInt("database.port");
            DatabaseStorage.databaseName = doc.getString("database.database");
            DatabaseStorage.username = doc.getString("database.user");
            DatabaseStorage.password = doc.getString("database.passwd");

            if (DatabaseStorage.username.equals(Objects.requireNonNull(doc.getDefaults()).getString("database.user")) ||
                    DatabaseStorage.password.equals(doc.getDefaults().getString("database.passwd"))) {
                Pooper.LOG.error("Please configure the database before running PooperMC!");
            } else {
                DatabaseStorage.loadDependency(libraryManager);
            }
        }

        MessageLogging.enabled = doc.getBoolean("message-logging.enabled");
        MessageLogging.maxHistory = doc.getInt("message-logging.max-history");

        chatUtilities = doc.getSection("chatutility");

        // Server List Configuration
        if (doc.getBoolean("server-list.enabled")) {
            serverListMotd = doc.getBoolean("server-list.custom-motd");
            if (serverListMotd) serverListMotdList = doc.getStringList("server-list.custom-motd-messages").stream()
                    .map(s -> MiniMessage.miniMessage().deserialize(s))
                    .toList();

            serverListFavicon = doc.getBoolean("server-list.custom-favicon");
            if (serverListFavicon) {
                doc.getStringList("server-list.custom-favicon-urls").forEach(s -> {
                    try {
                        faviconHandler.addIcon(ImageIO.read(new URI(s).toURL()));
                    } catch (IOException | URISyntaxException e) {
                        Pooper.LOG.error("One or more provided favicon URLs is invalid or not an image!");
                    } catch (FaviconHandler.InvalidSizeException e) {
                        Pooper.LOG.error("One or more of the provided favicons is not 64x64 pixels!");
                    }
                });
                serverListFavicons = faviconHandler;
            }

            serverListShowMaxPlayers = doc.getInt("server-list.show-max-players");
            serverListShowCurrentPlayers = doc.getInt("server-list.show-current-players");
        }

        Pooper.INSTANCE.extraConfiguration(doc);

        if (downloadTranslations) {
            HttpRequest request = HttpRequest.newBuilder(new URI("https://marcpg.com/pooper/lang/all")).GET().build();
            String response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
            Translation.loadMaps(new Gson().fromJson(response, new TypeToken<Map<Locale, Map<String, String>>>(){}.getType()));
        } else {
            Properties properties = new Properties();
            properties.load(Pooper.class.getResourceAsStream("/en_US.properties"));
            Translation.loadSingleProperties(Locale.getDefault(), properties);
        }
    }
}

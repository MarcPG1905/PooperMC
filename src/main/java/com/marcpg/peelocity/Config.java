package com.marcpg.peelocity;

import com.marcpg.data.database.sql.SQLConnection;
import com.marcpg.data.database.sql.SQLConnection.DatabaseType;
import com.marcpg.lang.Translation;
import com.marcpg.web.Downloads;
import com.marcpg.web.discord.Webhook;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.extensions.compactnotation.CompactConstructor;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

public class Config {
    public static final List<DatabaseType> ALLOWED_DATABASES = List.of(DatabaseType.POSTGRESQL, DatabaseType.ORACLE, DatabaseType.MYSQL, DatabaseType.MS_SQL_SERVER, DatabaseType.MARIADB);

    public static Webhook MODERATOR_WEBHOOK;
    public static boolean ENABLE_TRANSLATIONS;
    public static Map<String, Integer> GAMEMODES;
    public static int CONFIG_VERSION;

    public static SQLConnection.DatabaseType DATABASE_TYPE;
    public static String DATABASE_ADDRESS;
    public static int DATABASE_PORT;
    public static String DATABASE_NAME;
    public static String DATABASE_USER;
    public static String DATABASE_PASSWD;

    public static boolean WHITELIST;
    public static List<String> WHITELISTED_NAMES;

    /** Creates the default plugins/peelocity/* structure with all required files and folders and downloads the newest configuration, if it doesn't exist yet. */
    public static void saveDefaultConfig() throws IOException, URISyntaxException {
        if (Peelocity.DATA_DIRECTORY.resolve("lang/").toFile().mkdirs()) Peelocity.LOG.info("Created plugins/peelocity/lang/, as it didn't exist before!");
        if (Peelocity.DATA_DIRECTORY.resolve("message-history/").toFile().mkdirs()) Peelocity.LOG.info("Created plugins/peelocity/message-history/, as it didn't exist before!");
        if (Peelocity.DATA_DIRECTORY.resolve("playercache").toFile().createNewFile()) Peelocity.LOG.info("Created plugins/peelocity/playercache/, as it didn't exist before!");

        File config = Peelocity.DATA_DIRECTORY.resolve("pee.yml").toFile();
        if (!config.exists()) {
            Peelocity.LOG.info("Downloading Peelocity configuration from https://marcpg.com/peelocity/pee.yml...");
            Downloads.simpleDownload(new URI("https://marcpg.com/peelocity/pee.yml").toURL(), config);
            Peelocity.LOG.info("Successfully downloaded the Peelocity configuration.");
        }
    }

    @SuppressWarnings("unchecked")
    public static boolean load() throws IOException, URISyntaxException {
        try (Reader reader = new FileReader(Peelocity.DATA_DIRECTORY.resolve("pee.yml").toFile())) {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(reader);

            MODERATOR_WEBHOOK = new Webhook(new URI((String) config.get("moderator-webhook")).toURL());
            ENABLE_TRANSLATIONS = (Boolean) config.get("enable-translations");
            GAMEMODES = (Map<String, Integer>) config.get("gamemodes");

            Map<String, Object> database = (Map<String, Object>) config.get("database");
            DATABASE_TYPE = DatabaseType.valueOf(((String) database.get("type")).toUpperCase());
            DATABASE_ADDRESS = (String) database.get("address");
            DATABASE_PORT = (Integer) database.get("port");
            DATABASE_NAME = (String) database.get("database");
            DATABASE_USER = (String) database.get("user");
            DATABASE_PASSWD = (String) database.get("passwd");

            Map<String, Object> whitelist = (Map<String, Object>) config.get("whitelist");
            WHITELIST = (Boolean) whitelist.get("enabled");
            WHITELISTED_NAMES = (List<String>) whitelist.get("names");

            if (!ALLOWED_DATABASES.contains(DATABASE_TYPE))
                throw new IncorrectConfigurationException("database.type", "Invalid database type " + DATABASE_TYPE.name + "!");

            if (DATABASE_PORT == 0)
                DATABASE_PORT = DATABASE_TYPE.defaultPort;
        } catch (NullPointerException | ClassCastException e) {
            throw new IncorrectConfigurationException("???", "Invalid setting type! Please check for typos.");
        } catch (IllegalArgumentException e) {
            throw new IncorrectConfigurationException("database.type", "Invalid database type " + DATABASE_TYPE.name + "!");
        }

        if (ENABLE_TRANSLATIONS) new Thread(new TranslationDownloadTask()).start();

        return true;
    }

    /** Checks if there is a newer config version than the current and if there is, downloads it and migrates to it. */
    public static void checkVersionAndMigrate() throws IOException, URISyntaxException {
        Yaml yaml = new Yaml(new CompactConstructor());

        Map<String, Object> config = yaml.load(new FileReader(Peelocity.DATA_DIRECTORY.resolve("pee.yml").toFile()));
        if (getNewestVersion() == (Integer) config.get("version")) return;

        Peelocity.LOG.info("Found newer configuration version. Downloading and migrating now!");

        File currentConfig = Peelocity.DATA_DIRECTORY.resolve("pee.yml").toFile();
        File newConfig = Files.createTempFile("new_pee", ".yml").toFile();
        Downloads.simpleDownload(new URI("https://marcpg.com/peelocity/pee.yml").toURL(), newConfig);

        Map<String, Object> currentValues = yaml.load(new FileReader(currentConfig));

        String currentIndent = null;

        List<String> lines = Files.readAllLines(newConfig.toPath());
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            if (line.isBlank() || line.stripLeading().startsWith("#")) continue;

            String[] entry = line.split(":");

            if (line.stripTrailing().endsWith(":")) {
                currentIndent = entry[0];
                continue;
            }

            if (line.startsWith(" ") && currentIndent != null) {
                if (currentValues.containsKey(currentIndent) && currentValues.get(currentIndent) instanceof Map<?,?> && ((Map<String, Object>) currentValues.get(currentIndent)).containsKey(entry[0])) {
                    lines.set(0, "  " + entry[0] + ": " + currentValues.get(entry[0]));
                }
            } else currentIndent = null;

            if (currentValues.containsKey(entry[0]))
                lines.set(0, entry[0] + ": " + currentValues.get(entry[0]));
        }
        Files.write(currentConfig.toPath(), lines, StandardOpenOption.TRUNCATE_EXISTING);
        Files.deleteIfExists(newConfig.toPath());

        Peelocity.LOG.info("Successfully downloaded and migrated to the newer configuration!");
    }

    private static int getNewestVersion() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URI("https://marcpg.com/peelocity/config-latest").toURL().openStream()))) {
            String line = reader.readLine();
            return line == null ? 0 : Integer.parseInt(line);
        } catch (IOException | URISyntaxException e) {
            return 0;
        }
    }

    private static class TranslationDownloadTask implements Runnable {
        private final Path langFolder = Peelocity.DATA_DIRECTORY.resolve("lang");
        private static final int MAX_RETRIES = 3;

        @Override
        public void run() {
            int attempt = 1;
            while (attempt <= MAX_RETRIES) {
                try {
                    Downloads.simpleDownload(new URI("https://marcpg.com/peelocity/translations/available_locales").toURL(), langFolder.resolve("available_locales.temp").toFile());
                    for (String locale : Files.readAllLines(langFolder.resolve("available_locales.temp"))) {
                        Downloads.simpleDownload(new URI("https://marcpg.com/peelocity/translations/" + locale).toURL(), langFolder.resolve(locale).toFile());
                    }
                    Files.deleteIfExists(langFolder.resolve("available_locales.temp"));
                    return;
                } catch (Exception e) {
                    if (attempt == MAX_RETRIES) {
                        Peelocity.LOG.warn("Translation download failed. The maximum amount of retries (" + MAX_RETRIES + ") has been reached!");
                    } else {
                        Peelocity.LOG.warn("Translation download failed on attempt " + attempt + ", retrying in 3 seconds...");
                        try {
                            Thread.sleep(3000); // Sleep for 3 seconds
                        } catch (InterruptedException ignored) {}
                    }
                    attempt++;
                }
            }
            try {
                Translation.loadProperties(langFolder.toFile());
            } catch (IOException e) {
                Peelocity.LOG.warn("The downloaded translations are corrupted or missing, so the translations couldn't be loaded!");
            }
            Peelocity.LOG.info("Downloaded and loaded all recent translations!");
        }
    }

    public static class IncorrectConfigurationException extends IOException {
        public IncorrectConfigurationException(String path, @NotNull String message) {
            super(path + " - " + message.stripTrailing() + (message.stripTrailing().matches("\\.$|\\?|!") ? "" : ".") + " Read the configuration for more info.");
        }
    }
}

package com.marcpg.peelocity;

import com.marcpg.discord.Webhook;
import com.marcpg.util.Downloads;
import net.hectus.lang.Translation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class Config {
    public static String DATABASE_URL;
    public static String DATABASE_USER;
    public static String DATABASE_PASSWD;
    public static boolean WHITELIST;
    public static List<String> WHITELISTED_NAMES;
    public static Webhook MOD_ONLY_WEBHOOK;
    public static Map<String, Integer> GAMEMODES;

    public static void saveDefaultConfig() throws IOException {
        if (Peelocity.DATA_DIRECTORY.resolve("lang/").toFile().mkdirs()) Peelocity.LOG.info("Created plugins/peelocity/lang/, as it didn't exist before!");
        if (Peelocity.DATA_DIRECTORY.resolve("playercache/").toFile().mkdirs()) Peelocity.LOG.info("Created plugins/peelocity/playercache/, as it didn't exist before!");
        if (Peelocity.DATA_DIRECTORY.resolve("message-history/").toFile().mkdirs()) Peelocity.LOG.info("Created plugins/peelocity/message-history/, as it didn't exist before!");

        File config = Peelocity.DATA_DIRECTORY.resolve("pee.properties").toFile();
        if (!config.exists()) {
            Peelocity.LOG.info("Downloading Peelocity configuration from https://marcpg.com/peelocity/pee.properties...");
            Downloads.simpleDownload(new URL("https://marcpg.com/peelocity/pee.properties"), config);
            Peelocity.LOG.info("Successfully downloaded the Peelocity configuration.");
        }
    }

    public static boolean load() throws IOException {
        Properties config = new Properties();
        config.load(new FileInputStream(new File(Peelocity.DATA_DIRECTORY.toFile(), "pee.properties")));

        try {
            DATABASE_URL = config.getProperty("db-url");
            DATABASE_USER = config.getProperty("db-user");
            DATABASE_PASSWD = config.getProperty("db-passwd");
            WHITELIST = Boolean.parseBoolean(config.getProperty("whitelist"));
            WHITELISTED_NAMES = List.of(config.getProperty("whitelisted-names").split(",|, "));
            MOD_ONLY_WEBHOOK = new Webhook(new URL(config.getProperty("mod-only-webhook")));
            GAMEMODES = Arrays.stream(config.getProperty("gamemodes").split(",|, "))
                    .map(entry -> entry.split("-"))
                    .collect(Collectors.toMap(
                            keyValue -> keyValue[0],
                            keyValue -> Integer.parseInt(keyValue[1])
                    ));
        } catch (IOException e) {
            return false;
        }

        if (Boolean.parseBoolean(config.getProperty("download-translations"))) {
            downloadTranslations();
            Peelocity.LOG.info("Downloaded and loaded all recent translations!");
        }
        return true;
    }

    public static void downloadTranslations() throws IOException {
        Path langFolder = Peelocity.DATA_DIRECTORY.resolve("lang");
        Downloads.simpleDownload(new URL("https://marcpg.com/peelocity/translations/available_locales"), langFolder.resolve("available_locales.temp").toFile());
        for (String locale : Files.readAllLines(langFolder.resolve("available_locales.temp"))) {
            Downloads.simpleDownload(new URL("https://marcpg.com/peelocity/translations/" + locale), langFolder.resolve(locale).toFile());
        }
        Files.deleteIfExists(langFolder.resolve("available_locales.temp"));
        Translation.load(langFolder.toFile());
    }
}

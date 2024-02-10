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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void saveDefaultConfig() throws IOException {
        Peelocity.DATA_DIRECTORY.toFile().mkdir();
        Peelocity.DATA_DIRECTORY.resolve("lang/").toFile().mkdir();
        Peelocity.DATA_DIRECTORY.resolve("playercache").toFile().createNewFile();

        File config = Peelocity.DATA_DIRECTORY.resolve("pee.properties").toFile();
        if (!config.exists()) {
            Peelocity.LOG.info("Downloading Peelocity configuration from https://marcpg.com/peelocity/pee.properties...");
            Downloads.simpleDownload(new URL("https://marcpg.com/peelocity/pee.properties"), config);
            Peelocity.LOG.info("Successfully downloaded the Peelocity configuration.");
        }
    }

    /** Also loads the translations! */
    public static void load() throws IOException {
        Properties config = new Properties();
        config.load(new FileInputStream(new File(Peelocity.DATA_DIRECTORY.toFile(), "pee.properties")));

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

        if (Boolean.parseBoolean(config.getProperty("download-translations"))) {
            getTranslations();
            Peelocity.LOG.info("Downloaded and loaded all recent translations!");
        }
    }

    public static void getTranslations() throws IOException {
        Path langFolder = Peelocity.DATA_DIRECTORY.resolve("lang");
        Downloads.simpleDownload(new URL("https://marcpg.com/peelocity/translations/available_locales"), langFolder.resolve("available_locales.temp").toFile());
        for (String locale : Files.readAllLines(langFolder.resolve("available_locales.temp"))) {
            Downloads.simpleDownload(new URL("https://marcpg.com/peelocity/translations/" + locale), langFolder.resolve(locale).toFile());
        }
        Translation.load(langFolder.toFile());
    }
}

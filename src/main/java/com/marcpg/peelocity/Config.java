package com.marcpg.peelocity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
    public static URL REPORT_WEBHOOK;
    public static Map<String, Integer> GAMEMODES;
    public static String SERVERLIST_VERSION;

    public static void saveDefaultConfig() {
        Path config = Peelocity.DATA_DIRECTORY.resolve("pee.properties");
        if (!Files.exists(config)) {
            try (InputStream in = Peelocity.class.getResourceAsStream("pee.properties")) {
                if (in == null) throw new IOException("Plugin .jar file is corrupted!");
                Files.copy(in, config, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                Peelocity.LOG.error("Error while saving the default configuration: " + e.getMessage());
            }
        }
    }

    public static void load() throws IOException {
        Properties config = new Properties();
        config.load(new FileInputStream(new File(Peelocity.DATA_DIRECTORY.toFile(), "pee.properties")));

        DATABASE_URL = config.getProperty("db-url");
        DATABASE_USER = config.getProperty("db-user");
        DATABASE_PASSWD = config.getProperty("db-passwd");
        WHITELIST = Boolean.parseBoolean(config.getProperty("whitelist"));
        WHITELISTED_NAMES = List.of(config.getProperty("whitelisted-names").split(",|, "));
        REPORT_WEBHOOK = new URL(config.getProperty("report-webhook"));
        GAMEMODES = Arrays.stream(config.getProperty("gamemodes").split(",|, "))
                .map(entry -> entry.split("-"))
                .collect(Collectors.toMap(
                        keyValue -> keyValue[0],
                        keyValue -> Integer.parseInt(keyValue[1])
                ));
        SERVERLIST_VERSION = config.getProperty("serverlist-version");
    }
}

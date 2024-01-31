package com.marcpg.peelocity;

import com.marcpg.discord.Webhook;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
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
            download(new URL("https://marcpg.com/peelocity/pee.properties"), config);
            Peelocity.LOG.info("Successfully downloaded the Peelocity configuration.");
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
        MOD_ONLY_WEBHOOK = new Webhook(new URL(config.getProperty("mod-only-webhook")));
        GAMEMODES = Arrays.stream(config.getProperty("gamemodes").split(",|, "))
                .map(entry -> entry.split("-"))
                .collect(Collectors.toMap(
                        keyValue -> keyValue[0],
                        keyValue -> Integer.parseInt(keyValue[1])
                ));
    }

    public static void download(@NotNull URL url, File destination) throws IOException {
        ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
        try (FileOutputStream fileOutputStream = new FileOutputStream(destination)) {
            FileChannel fileChannel = fileOutputStream.getChannel();
            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        }
    }
}

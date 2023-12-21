package com.marcpg.peelocity;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

public class Config {
    public static boolean CLOSED_TESTING;
    public static List<UUID> OPERATOR_UUIDS;
    public static boolean WHITELIST;
    public static List<UUID> WHITELISTED_UUIDS;

    public static void load() {
        Properties properties = new Properties();

        try (FileInputStream stream = new FileInputStream(new File(Peelocity.DATA_DIRECTORY.toFile(), "pee.properties"))) {
            properties.load(stream);
        } catch (IOException e) {
            Peelocity.SERVER.shutdown();
            Peelocity.LOG.error("Couldn't load Peelocity configuration (pee.properties): " + e);
        }

        CLOSED_TESTING = Boolean.parseBoolean(properties.getProperty("closed-testing"));
        OPERATOR_UUIDS = parseUUIDs(properties.getProperty("op-uuids"));
        WHITELIST = Boolean.parseBoolean(properties.getProperty("whitelist"));
        WHITELISTED_UUIDS = parseUUIDs(properties.getProperty("whitelisted-uuids"));
    }

    public static void save() {
        Properties properties = new Properties();

        properties.setProperty("closed-testing", String.valueOf(CLOSED_TESTING));
        properties.setProperty("op-uuids", parseUUIDs(OPERATOR_UUIDS));
        properties.setProperty("whitelist", String.valueOf(WHITELIST));
        properties.setProperty("whitelisted-uuids", parseUUIDs(WHITELISTED_UUIDS));

        try (FileOutputStream stream = new FileOutputStream(new File(Peelocity.DATA_DIRECTORY.toFile(), "pee.properties"))) {
            properties.store(stream, "Save Peelocity configuration to pee.properties file");

        } catch (IOException e) {
            Peelocity.LOG.error("Couldn't save/store Peelocity configuration (pee.properties): " + e);
        }
    }

    private static List<UUID> parseUUIDs(@NotNull String uuids) {
        return Arrays.stream(uuids.split(","))
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }

    private static String parseUUIDs(@NotNull List<UUID> uuids) {
        return uuids.stream()
                .map(UUID::toString)
                .reduce((uuid1, uuid2) -> uuid1 + "," + uuid2)
                .orElse("");
    }
}

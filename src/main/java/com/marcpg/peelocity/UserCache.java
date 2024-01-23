package com.marcpg.peelocity;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserCache {
    public static final Path CACHE_PATH = Path.of(Peelocity.DATA_DIRECTORY.toString(), "playercache");
    public static final HashMap<UUID, String> CACHED_USERS = new HashMap<>();

    public static void loadCachedUsers() throws IOException {
        CACHED_USERS.clear();
        Files.readAllLines(CACHE_PATH).parallelStream()
                .map(line -> line.split(","))
                .forEach(user -> CACHED_USERS.put(UUID.fromString(user[0]), user[1]));
        Peelocity.LOG.info("Loaded all entries from the 'usercache' file!");
    }

    public static void saveCachedUsers() throws IOException {
        List<String> lines = CACHED_USERS.entrySet().parallelStream()
                .map(entry -> entry.getKey().toString() + "," + entry.getValue())
                .toList();
        Files.write(CACHE_PATH, lines, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        Peelocity.LOG.info("Saved all cached users to the 'usercache' file!");
    }

    public static @Nullable UUID getUuid(String username) {
        for (Map.Entry<UUID, String> entry : CACHED_USERS.entrySet()) {
            if (username.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}

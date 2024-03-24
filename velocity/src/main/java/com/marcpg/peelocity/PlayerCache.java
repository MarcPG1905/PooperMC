package com.marcpg.peelocity;

import com.marcpg.libpg.color.Ansi;
import com.marcpg.common.Pooper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlayerCache {
    public static final HashMap<UUID, String> PLAYERS = new HashMap<>();
    private static final Path path = Pooper.DATA_DIR.resolve("playercache");

    public static void load() throws IOException {
        Pooper.LOG.info(Ansi.formattedString("Loading Player-Cache...", Ansi.DARK_GRAY));
        if (path.toFile().createNewFile()) return;
        Files.readAllLines(path).parallelStream()
                .map(l -> l.split(", *"))
                .forEach(p -> PLAYERS.put(UUID.fromString(p[0]), p[1]));
    }

    public static void save() throws IOException {
        List<String> lines = PLAYERS.entrySet().parallelStream()
                .map(e -> e.getKey().toString() + "," + e.getValue())
                .toList();
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static @Nullable UUID getUuid(String username) {
        for (Map.Entry<UUID, String> entry : PLAYERS.entrySet()) {
            if (username.equals(entry.getValue()))
                return entry.getKey();
        }
        return null;
    }
}

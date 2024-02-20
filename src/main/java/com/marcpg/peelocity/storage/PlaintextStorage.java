package com.marcpg.peelocity.storage;

import com.marcpg.data.time.Time;
import com.marcpg.peelocity.Peelocity;
import com.marcpg.peelocity.moderation.UserUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlaintextStorage extends RamStorage {
    private final Path path;

    public PlaintextStorage(String name) throws IOException {
        super(name);
        path = Peelocity.DATA_DIRECTORY.resolve("data").resolve(name + ".txt");

        if (!path.toFile().createNewFile()) {
            Files.readAllLines(path).parallelStream()
                    .map(line -> line.split(" {2}\\| {2}"))
                    .map(p -> new UserUtil.Punishment(UUID.fromString(p[0]), Boolean.parseBoolean(p[1]), Instant.ofEpochSecond(Long.parseLong(p[2])), new Time(Long.parseLong(p[3])), p[4]))
                    .forEach(p -> punishments.put(p.player(), p.toMap()));
        }
    }

    @Override
    public void add(Map<String, Object> entry) {
        super.add(entry);
        save();
    }

    @Override
    public void remove(UUID uuid) {
        super.remove(uuid);
        save();
    }

    public void save() {
        try {
            Files.writeString(path, punishments.values().parallelStream()
                    .map(p -> String.join("  |  ", p.values().stream().map(Object::toString).toList()))
                    .collect(Collectors.joining(System.lineSeparator())));
        } catch (IOException e) {
            Peelocity.LOG.error("Couldn't write to data/" + name + ".txt: " + e.getMessage());
        }
    }
}

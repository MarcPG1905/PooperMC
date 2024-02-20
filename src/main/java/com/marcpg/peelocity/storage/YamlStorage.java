package com.marcpg.peelocity.storage;

import com.marcpg.peelocity.Peelocity;
import dev.dejvokep.boostedyaml.YamlDocument;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class YamlStorage extends Storage {
    private final YamlDocument yamlDocument;

    public YamlStorage(String name) throws IOException {
        super(name);
        yamlDocument = YamlDocument.create(new File(Peelocity.DATA_DIRECTORY.toFile(), "/data/" + name + ".yml"));
    }

    @Override
    public boolean contains(@NotNull UUID uuid) {
        return yamlDocument.contains(uuid.toString());
    }

    @Override
    public void add(Map<String, Object> entry) {
        UUID uuid = (UUID) entry.get("uuid");
        entry.forEach((s, o) -> yamlDocument.set(uuid + "." + s, o));
        save();
    }

    @Override
    public void remove(UUID uuid) {
        yamlDocument.remove(uuid.toString());
        save();
    }

    @Override
    public Map<String, Object> get(UUID uuid) {
        return yamlDocument.getSection(uuid.toString()).getStringRouteMappedValues(false);
    }

    @Override
    public Map<UUID, Map<String, Object>> get(Predicate<Map<String, Object>> predicate) {
        return yamlDocument.getRoutesAsStrings(false).parallelStream()
                .map(s -> yamlDocument.getSection(s).getStringRouteMappedValues(false))
                .filter(predicate)
                .collect(Collectors.toMap(o -> (UUID) o.get("uuid"), o -> o));
    }

    public void save() {
        try {
            yamlDocument.save();
        } catch (IOException e) {
            Peelocity.LOG.error("Couldn't save data/" + name + ".yml: " + e.getMessage());
        }
    }
}

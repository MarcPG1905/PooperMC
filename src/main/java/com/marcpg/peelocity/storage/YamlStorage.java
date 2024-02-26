package com.marcpg.peelocity.storage;

import com.marcpg.peelocity.Peelocity;
import dev.dejvokep.boostedyaml.YamlDocument;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class YamlStorage<T> extends Storage<T> {
    private final YamlDocument yamlDocument;

    public YamlStorage(String name, String keyName) throws IOException {
        super(name, keyName);
        yamlDocument = YamlDocument.create(new File(Peelocity.DATA_DIRECTORY.toFile(), "/data/" + name + ".yml"));
    }

    @Override
    public boolean contains(@NotNull T key) {
        return yamlDocument.contains(key.toString());
    }

    @Override
    public void add(Map<String, Object> entry) {
        T key = (T) entry.get(keyName);
        entry.forEach((s, o) -> yamlDocument.set(key + "." + s, o));
        save();
    }

    @Override
    public void remove(T key) {
        yamlDocument.remove(key.toString());
        save();
    }

    @Override
    public Map<String, Object> get(T key) {
        return yamlDocument.getSection(key.toString()).getStringRouteMappedValues(false);
    }

    @Override
    public Map<T, Map<String, Object>> get(Predicate<Map<String, Object>> predicate) {
        return yamlDocument.getRoutesAsStrings(false).parallelStream()
                .map(s -> yamlDocument.getSection(s).getStringRouteMappedValues(false))
                .filter(predicate)
                .collect(Collectors.toMap(o -> (T) o.get(keyName), o -> o));
    }

    public void save() {
        try {
            yamlDocument.save();
        } catch (IOException e) {
            Peelocity.LOG.error("Couldn't save data/" + name + ".yml: " + e.getMessage());
        }
    }
}

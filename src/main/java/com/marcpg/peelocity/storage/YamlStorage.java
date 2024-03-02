package com.marcpg.peelocity.storage;

import com.marcpg.peelocity.Peelocity;
import dev.dejvokep.boostedyaml.YamlDocument;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class YamlStorage<T> extends Storage<T> {
    private final YamlDocument doc;

    public YamlStorage(String name, String primaryKeyName) throws IOException {
        super(name, primaryKeyName);
        this.doc = YamlDocument.create(new File(Peelocity.DATA_DIR.toFile(), "/data/" + name + ".yml"));
    }

    @Override
    public boolean contains(@NotNull T key) {
        return this.doc.contains(key.toString());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void add(@NotNull Map<String, Object> entry) {
        T key = (T) entry.get(this.primaryKeyName);
        entry.remove(this.primaryKeyName);
        entry.forEach((s, o) -> this.doc.set(key + "." + s, o));
    }

    @Override
    public void remove(@NotNull T key) {
        this.doc.remove(key.toString());
    }

    @Override
    public Map<String, Object> get(@NotNull T key) {
        return this.doc.getSection(key.toString()).getStringRouteMappedValues(false);
    }

    @Override
    public List<Map<String, Object>> get(Predicate<Map<String, Object>> predicate) {
        return this.doc.getRoutesAsStrings(false).parallelStream()
                .map(s -> this.doc.getSection(s).getStringRouteMappedValues(false))
                .filter(predicate)
                .toList();
    }
}

package com.marcpg.poopermc.storage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class RamStorage<T> extends Storage<T> {
    private final Map<T, Map<String, Object>> storage = new HashMap<>();

    public RamStorage(String name, String primaryKeyName) {
        super(name, primaryKeyName);
    }

    @Override
    public boolean contains(T key) {
        return this.storage.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void add(Map<String, Object> entries) {
        this.storage.put((T) entries.get(this.primaryKeyName), entries);
    }

    @Override
    public void remove(T key) {
        this.storage.remove(key);
    }

    @Override
    public Map<String, Object> get(T key) {
        return this.storage.get(key);
    }

    public Collection<Map<String, Object>> get(Predicate<Map<String, Object>> predicate) {
        return this.storage.values().stream().filter(predicate).toList();
    }

    @Override
    public Collection<Map<String, Object>> getAll() {
        return this.storage.values();
    }
}

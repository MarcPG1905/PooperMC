package com.marcpg.peelocity.storage;

import java.util.HashMap;
import java.util.List;
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

    @Override
    public List<Map<String, Object>> get(Predicate<Map<String, Object>> predicate) {
        return this.storage.entrySet().parallelStream()
                .filter(e -> predicate.test(e.getValue()))
                .map(Map.Entry::getValue)
                .toList();
    }
}

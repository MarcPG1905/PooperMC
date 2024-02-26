package com.marcpg.peelocity.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RamStorage<T> extends Storage<T> {
    protected final Map<T, Map<String, Object>> entries = new HashMap<>();

    public RamStorage(String name, String keyName) {
        super(name, keyName);
    }

    @Override
    public boolean contains(T key) {
        return entries.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void add(Map<String, Object> entry) {
        entries.put((T) entry.get(keyName), entry);
    }

    @Override
    public void remove(T key) {
        entries.remove(key);
    }

    @Override
    public Map<T, Map<String, Object>> get(Predicate<Map<String, Object>> predicate) {
        return entries.entrySet().parallelStream()
                .filter(e -> predicate.test(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, Object> get(T key) {
        return entries.get(key);
    }
}

package com.marcpg.peelocity.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RamStorage extends Storage {
    protected final Map<UUID, Map<String, Object>> punishments = new HashMap<>();

    public RamStorage(String name) {
        super(name);
    }

    @Override
    public boolean contains(UUID uuid) {
        return punishments.containsKey(uuid);
    }

    @Override
    public void add(Map<String, Object> entry) {
        punishments.put((UUID) entry.get("uuid"), entry);
    }

    @Override
    public void remove(UUID uuid) {
        punishments.remove(uuid);
    }

    @Override
    public Map<UUID, Map<String, Object>> get(Predicate<Map<String, Object>> predicate) {
        return punishments.entrySet().parallelStream()
                .filter(e -> predicate.test(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, Object> get(UUID uuid) {
        return punishments.get(uuid);
    }
}

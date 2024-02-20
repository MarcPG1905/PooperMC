package com.marcpg.peelocity.storage;

import com.marcpg.peelocity.Peelocity;

import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public abstract class Storage {
    public enum StorageType {
        DATABASE, YAML, PLAINTEXT, RAM;

        public Storage getStorage(String name) {
            try {
                return switch (this) {
                    case DATABASE -> new DatabaseStorage(name);
                    case YAML -> new YamlStorage(name);
                    case PLAINTEXT -> new PlaintextStorage(name);
                    case RAM -> new RamStorage(name);
                };
            } catch (Exception e) {
                Peelocity.LOG.info("Couldn't create storage \"" + name + "\": " + e.getMessage());
                return new RamStorage(name);
            }
        }
    }

    public final String name;

    public Storage(String name) {
        this.name = name;
    }

    public abstract boolean contains(UUID uuid);

    public abstract void add(Map<String, Object> entry);

    public abstract void remove(UUID uuid);

    public abstract Map<String, Object> get(UUID uuid);

    public abstract Map<UUID, Map<String, Object>> get(Predicate<Map<String, Object>> predicate);
}

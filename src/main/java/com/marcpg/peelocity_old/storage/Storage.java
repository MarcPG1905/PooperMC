package com.marcpg.peelocity_old.storage;

import com.marcpg.peelocity_old.Peelocity;

import java.util.Map;
import java.util.function.Predicate;

public abstract class Storage<T> {
    public enum StorageType {
        DATABASE, YAML, RAM;

        public <T> Storage<T> getStorage(String name, String keyName) {
            try {
                return switch (this) {
                    case DATABASE -> new DatabaseStorage<>(name, keyName);
                    case YAML -> new YamlStorage<>(name, keyName);
                    case RAM -> new RamStorage<>(name, keyName);
                };
            } catch (Exception e) {
                Peelocity.LOG.info("Couldn't create storage \"" + name + "\": " + e.getMessage());
                return new RamStorage<>(name, keyName);
            }
        }
    }

    public final String name;
    public final String keyName;

    public Storage(String name, String keyName) {
        this.name = name;
        this.keyName = keyName;
    }

    public abstract boolean contains(T key);

    public abstract void add(Map<String, Object> entry);

    public abstract void remove(T key);

    public abstract Map<String, Object> get(T key);

    public abstract Map<T, Map<String, Object>> get(Predicate<Map<String, Object>> predicate);
}

package com.marcpg.peelocity.storage;

import com.marcpg.peelocity.Peelocity;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public abstract class Storage<T> {
    public enum StorageType {
        DATABASE, YAML, RAM;

        public <T> Storage<T> createStorage(String name, String primaryKeyName) {
            try {
                return switch (this) {
                    case DATABASE -> new DatabaseStorage<>(name, primaryKeyName);
                    case YAML -> new YamlStorage<>(name, primaryKeyName);
                    case RAM -> new RamStorage<>(name, primaryKeyName);
                };
            } catch (SQLException | IOException | ClassNotFoundException e) {
                Peelocity.LOG.error("Couldn't create storage \"" + name + "\". Using the default (RAM) now: " + e.getMessage());
                return new RamStorage<>(name, primaryKeyName);
            }
        }
    }

    final String name;
    final String primaryKeyName;

    public Storage(String name, String primaryKeyName) {
        this.name = name;
        this.primaryKeyName = primaryKeyName;
    }

    public abstract boolean contains(T key);

    public abstract void add(Map<String, Object> entry);

    public abstract void remove(T key);

    public abstract Map<String, Object> get(T key);

    public abstract List<Map<String, Object>> get(Predicate<Map<String, Object>> predicate);
}

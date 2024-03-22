package com.marcpg.poopermc.storage;

import com.marcpg.poopermc.Pooper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

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
                Pooper.LOG.error("Couldn't create storage \"" + name + "\". Using the default RAM storage instead now: " + e.getMessage());
                return new RamStorage<>(name, primaryKeyName);
            }
        }
    }

    public static StorageType storageType;
    final String name;
    final String primaryKeyName;

    public Storage(String name, String primaryKeyName) {
        this.name = name;
        this.primaryKeyName = primaryKeyName;
    }

    public abstract boolean contains(T key);

    public abstract void add(Map<String, Object> entries);

    public abstract void remove(T key);

    public abstract Map<String, Object> get(T key);

    public abstract Collection<Map<String, Object>> getAll();
}

package com.marcpg.common.storage;

import com.alessiodp.libby.Library;
import com.alessiodp.libby.LibraryManager;
import com.marcpg.libpg.data.database.sql.AutoCatchingSQLConnection;
import com.marcpg.libpg.data.database.sql.SQLConnection;
import com.marcpg.common.Pooper;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

public class DatabaseStorage<T> extends Storage<T> {
    public static SQLConnection.DatabaseType type;
    public static String address;
    public static int port;
    public static String databaseName;
    public static String username;
    public static String password;

    private final AutoCatchingSQLConnection<T> connection;

    public DatabaseStorage(String name, String primaryKeyName) throws SQLException, ClassNotFoundException {
        super(name, primaryKeyName);
        this.connection = new AutoCatchingSQLConnection<>(type, address, port, databaseName, username, password, name, primaryKeyName, e -> Pooper.LOG.error("Couldn't interact with the " + name + " database: " + e.getMessage()));
        this.createTable(switch (name) {
            case "friendships" -> "uuid UUID PRIMARY KEY, player1 UUID NOT NULL, player2 UUID NOT NULL";
            case "bans" -> "player UUID PRIMARY KEY, permanent BOOLEAN NOT NULL, expires BIGINT NOT NULL, duration BIGINT NOT NULL, reason TEXT NOT NULL";
            case "mutes" -> "player UUID PRIMARY KEY, expires BIGINT NOT NULL, duration BIGINT NOT NULL, reason TEXT NOT NULL";
            case "whitelist" -> "username VARCHAR(20) PRIMARY KEY";
            default -> throw new IllegalStateException("Unexpected table name: " + name);
        });
    }

    @SuppressWarnings("resource")
    private void createTable(String values) throws SQLException {
        String query = (type == SQLConnection.DatabaseType.MS_SQL_SERVER ? "IF OBJECT_ID(N'" + this.name + "', N'U') IS NULL CREATE TABLE " : "CREATE TABLE IF NOT EXISTS ") + this.name + "(" + values + ");";
        this.connection.connection().prepareStatement(query).executeUpdate();
    }

    @Override
    public boolean contains(T key) {
        return this.connection.contains(key);
    }

    @Override
    public void add(@NotNull Map<String, Object> entries) {
        this.connection.add(entries);
    }

    @Override
    public void remove(T key) {
        this.connection.remove(key);
    }

    @Override
    public Map<String, Object> get(T key) {
        return this.connection.getRowMap(key);
    }

    public Collection<Map<String, Object>> get(String predicate, Object... replacements) {
        return this.connection.getRowMapsMatching(predicate, replacements);
    }

    @Override
    public Collection<Map<String, Object>> getAll() {
        return null;
    }

    public static void loadDependency(@NotNull LibraryManager manager) {
        manager.addSonatype();
        String[] info = switch (type) {
            case MYSQL -> new String[]{ "com{}mysql", "mysql-connector-j", "8.3.0" };
            case MARIADB -> new String[]{ "org{}mariadb{}jdbc", "mariadb-java-client", "3.3.2" };
            case MS_SQL_SERVER -> new String[]{ "com{}microsoft{}sqlserver", "mssql-jdbc", "12.6.0.jre11" };
            case ORACLE -> new String[]{ "com{}oracle{}database{}jdbc", "ojdbc10", "19.22.0.0" };
            default -> new String[]{ "org{}postgresql", "postgresql", "42.7.1" };
        };
        manager.loadLibrary(Library.builder().groupId(info[0]).artifactId(info[1]).version(info[2]).build());
    }
}

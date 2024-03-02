package com.marcpg.peelocity.storage;

import com.alessiodp.libby.Library;
import com.alessiodp.libby.VelocityLibraryManager;
import com.marcpg.data.database.sql.AutoCatchingSQLConnection;
import com.marcpg.data.database.sql.SQLConnection;
import com.marcpg.peelocity.Peelocity;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public class DatabaseStorage<T> extends Storage<T> {
    // START - Configuration-Based Values
    public static SQLConnection.DatabaseType TYPE;
    public static String ADDRESS;
    public static int PORT;
    public static String NAME;
    public static String USERNAME;
    public static String PASSWORD;

    // END - Configuration-Based Values

    private final AutoCatchingSQLConnection<T> connection;

    public DatabaseStorage(String name, String primaryKeyName) throws SQLException, ClassNotFoundException {
        super(name, primaryKeyName);
        this.connection = new AutoCatchingSQLConnection<>(TYPE, ADDRESS, PORT, NAME, USERNAME, PASSWORD, name, primaryKeyName, e -> Peelocity.LOG.error("Couldn't interact with the " + name + " database: " + e.getMessage()));
        this.createTable(switch (name) {
            case "friendships" -> "uuid UUID PRIMARY KEY, player1 UUID NOT NULL, player2 UUID NOT NULL";
            case "bans" -> "player UUID PRIMARY KEY, permanent BOOLEAN NOT NULL, expires BIGINT NOT NULL, duration BIGINT NOT NULL, reason TEXT NOT NULL";
            case "mutes" -> "player UUID PRIMARY KEY, expires BIGINT NOT NULL, duration BIGINT NOT NULL, reason TEXT NOT NULL";
            case "whitelist" -> "username VARCHAR(20) PRIMARY KEY";
            default -> throw new IllegalStateException("Unexpected table name: " + name);
        });
    }

    public void createTable(String values) throws SQLException {
        String query = (TYPE == SQLConnection.DatabaseType.MS_SQL_SERVER ? "IF OBJECT_ID(N'" + this.name + "', N'U') IS NULL CREATE TABLE " : "CREATE TABLE IF NOT EXISTS ") + this.name + "(" + values;
        this.connection.connection().prepareStatement(query).executeUpdate();
    }

    @Override
    public boolean contains(T key) {
        return this.connection.contains(key);
    }

    @Override
    public void add(@NotNull Map<String, Object> entry) {
        T primaryKey = (T) entry.get(this.primaryKeyName);
        if (entry.size() == 1) {
            this.connection.add(primaryKey);
        } else {
            entry.remove(this.primaryKeyName);
            this.connection.add(primaryKey, entry.values().toArray(Object[]::new));
        }
    }

    @Override
    public void remove(T key) {
        this.connection.remove(key);
    }

    @Override
    public Map<String, Object> get(T key) {
        return this.connection.getRowMap(key);
    }

    @Override
    public List<Map<String, Object>> get(Predicate<Map<String, Object>> predicate) {
        try (PreparedStatement preparedStatement = this.connection.connection().prepareStatement("SELECT " + this.primaryKeyName + " FROM " + this.name)) {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<Map<String, Object>> list = new ArrayList<>();
                while (resultSet.next()) {
                    Map<String, Object> result = this.get((T) resultSet.getObject(this.primaryKeyName));
                    if (predicate.test(result))
                        list.add(result);
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadDependency() {
        VelocityLibraryManager<Peelocity> libraryManager = new VelocityLibraryManager<>(Peelocity.INSTANCE, Peelocity.LOG, Peelocity.DATA_DIR, Peelocity.SERVER.getPluginManager());
        libraryManager.addSonatype();

        String[] info = switch (TYPE) {
            case MYSQL -> new String[]{ "com{}mysql", "mysql-connector-j", "8.3.0" };
            case MARIADB -> new String[]{ "org{}mariadb{}jdbc", "mariadb-java-client", "3.3.2" };
            case MS_SQL_SERVER -> new String[]{ "com{}microsoft{}sqlserver", "mssql-jdbc", "12.6.0.jre11" };
            case ORACLE -> new String[]{ "com{}oracle{}database{}jdbc", "ojdbc10", "19.22.0.0" };
            default -> new String[]{ "org{}postgresql", "postgresql", "42.7.1" };
        };
        libraryManager.loadLibrary(Library.builder().groupId(info[0]).artifactId(info[1]).version(info[2]).build());
    }
}

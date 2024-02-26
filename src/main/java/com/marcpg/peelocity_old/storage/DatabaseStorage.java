package com.marcpg.peelocity_old.storage;

import com.alessiodp.libby.Library;
import com.alessiodp.libby.VelocityLibraryManager;
import com.marcpg.data.database.sql.AutoCatchingSQLConnection;
import com.marcpg.peelocity_old.Config;
import com.marcpg.peelocity_old.Peelocity;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public class DatabaseStorage<T> extends Storage<T> {
    private final AutoCatchingSQLConnection<T> connection;

    public DatabaseStorage(String name, String keyName) throws SQLException, ClassNotFoundException {
        super(name, keyName);
        connection = new AutoCatchingSQLConnection<>(
                Config.DATABASE_TYPE,
                Config.CONFIG.getString("database.address"),
                Config.CONFIG.getInt("database.port"),
                Config.CONFIG.getString("database.database"),
                Config.CONFIG.getString("database.user"),
                Config.CONFIG.getString("database.passwd"),
                name,
                keyName,
                e -> {
                    e.printStackTrace();
                    Peelocity.LOG.warn("Error while interacting with the " + name + " database: " + e.getMessage());
                });
    }

    @Override
    public boolean contains(T key) {
        return connection.contains(key);
    }

    @Override
    public void add(@NotNull Map<String, Object> entry) {
        connection.add((T) entry.get(keyName), entry.entrySet().stream().filter(e -> !e.getKey().equals(keyName)).map(Map.Entry::getValue).toArray(Object[]::new));
    }

    @Override
    public void remove(T key) {
        connection.remove(key);
    }

    @Override
    public Map<String, Object> get(T key) {
        return connection.getRowMap(key);
    }

    @Override
    public Map<T, Map<String, Object>> get(Predicate<Map<String, Object>> predicate) {
        try (PreparedStatement preparedStatement = connection.connection().prepareStatement("SELECT " + keyName + " FROM " + name)) {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                Map<T, Map<String, Object>> map = new HashMap<>();
                while (resultSet.next()) {
                    Map<String, Object> result = get((T) resultSet.getObject(keyName));
                    if (predicate.test(result))
                        map.put((T) result.get(keyName), result);
                }
                return map;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadDependency() {
        VelocityLibraryManager<Peelocity> libraryManager = new VelocityLibraryManager<>(Peelocity.PLUGIN, Peelocity.LOG, Peelocity.DATA_DIRECTORY, Peelocity.SERVER.getPluginManager());
        libraryManager.addSonatype();

        String[] info = switch (Config.DATABASE_TYPE) {
            case MYSQL -> new String[]{ "com{}mysql", "mysql-connector-j", "8.3.0" };
            case MARIADB -> new String[]{ "org{}mariadb{}jdbc", "mariadb-java-client", "3.3.2" };
            case MS_SQL_SERVER -> new String[]{ "com{}microsoft{}sqlserver", "mssql-jdbc", "12.6.0.jre11" };
            case ORACLE -> new String[]{ "com{}oracle{}database{}jdbc", "ojdbc10", "19.22.0.0" };
            default -> new String[]{ "org{}postgresql", "postgresql", "42.7.1" };
        };
        libraryManager.loadLibrary(Library.builder().groupId(info[0]).artifactId(info[1]).version(info[2]).build());
    }
}

package com.marcpg.peelocity.storage;

import com.alessiodp.libby.Library;
import com.alessiodp.libby.VelocityLibraryManager;
import com.marcpg.data.database.sql.AutoCatchingSQLConnection;
import com.marcpg.peelocity.Config;
import com.marcpg.peelocity.Peelocity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class DatabaseStorage extends Storage {
    private final AutoCatchingSQLConnection connection;

    public DatabaseStorage(String name) throws SQLException, ClassNotFoundException {
        super(name);
        connection = new AutoCatchingSQLConnection(
                Config.DATABASE_TYPE,
                Config.CONFIG.getString("database.address"),
                Config.CONFIG.getInt("database.port"),
                Config.CONFIG.getString("database.database"),
                Config.CONFIG.getString("database.user"),
                Config.CONFIG.getString("database.passwd"),
                name, e -> Peelocity.LOG.warn("Error while interacting with the ban database: " + e.getMessage()));
    }

    @Override
    public boolean contains(UUID uuid) {
        return connection.contains(uuid);
    }

    @Override
    public void add(Map<String, Object> entry) {
        connection.add((UUID) entry.get("uuid"), entry.values().toArray(Object[]::new));
    }

    @Override
    public void remove(UUID uuid) {
        connection.remove(uuid);
    }

    @Override
    public Map<String, Object> get(UUID uuid) {
        return connection.getRowMap(uuid);
    }

    @Override
    public Map<UUID, Map<String, Object>> get(Predicate<Map<String, Object>> predicate) {
        try (PreparedStatement preparedStatement = connection.connection().prepareStatement("SELECT uuid FROM " + name)) {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                Map<UUID, Map<String, Object>> map = new HashMap<>();
                while (resultSet.next()) {
                    Map<String, Object> result = get((UUID) resultSet.getObject("uuid"));
                    if (predicate.test(result))
                        map.put((UUID) result.get("uuid"), result);
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

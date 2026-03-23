package mx.angelDL_.internal.database;

import mx.angelDL_.Main;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private final Main plugin;
    private Connection connection;
    private final File dbFile;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "data.db");
        connect();
        createTables();
    }

    private void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            connection.setAutoCommit(true);
            plugin.getLogger().info("Connected to SQLite database.");
        } catch (Exception e) {
            plugin.getLogger().severe("Could not connect to database: " + e.getMessage());
        }
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS players (" +
                "uuid TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "role TEXT NOT NULL DEFAULT 'Investigador', " +
                "alive INTEGER NOT NULL DEFAULT 1, " +
                "last_steal_time INTEGER NOT NULL DEFAULT 0, " +
                "has_shinigami_deal INTEGER NOT NULL DEFAULT 0" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS identifications (" +
                "owner_uuid TEXT NOT NULL, " +
                "id_name TEXT NOT NULL, " +
                "PRIMARY KEY (owner_uuid, id_name)" +
                ")"
            );
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating database tables: " + e.getMessage());
        }
    }

    public void savePlayer(UUID uuid, String name, String role, boolean alive, long lastStealTime, boolean hasShinigamiDeal) {
        String sql = "INSERT OR REPLACE INTO players (uuid, name, role, alive, last_steal_time, has_shinigami_deal) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, role);
            ps.setInt(4, alive ? 1 : 0);
            ps.setLong(5, lastStealTime);
            ps.setInt(6, hasShinigamiDeal ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving player " + name + ": " + e.getMessage());
        }
    }

    public Map<String, Object> loadPlayer(UUID uuid) {
        String sql = "SELECT * FROM players WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> data = new HashMap<>();
                data.put("name", rs.getString("name"));
                data.put("role", rs.getString("role"));
                data.put("alive", rs.getInt("alive") == 1);
                data.put("last_steal_time", rs.getLong("last_steal_time"));
                data.put("has_shinigami_deal", rs.getInt("has_shinigami_deal") == 1);
                return data;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading player " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    public List<Map<String, Object>> loadAllPlayers() {
        String sql = "SELECT * FROM players";
        List<Map<String, Object>> players = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> data = new HashMap<>();
                data.put("uuid", rs.getString("uuid"));
                data.put("name", rs.getString("name"));
                data.put("role", rs.getString("role"));
                data.put("alive", rs.getInt("alive") == 1);
                data.put("last_steal_time", rs.getLong("last_steal_time"));
                data.put("has_shinigami_deal", rs.getInt("has_shinigami_deal") == 1);
                players.add(data);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading all players: " + e.getMessage());
        }
        return players;
    }

    public void saveIdentifications(UUID uuid, List<String> identifications) {
        String deleteSql = "DELETE FROM identifications WHERE owner_uuid = ?";
        String insertSql = "INSERT OR IGNORE INTO identifications (owner_uuid, id_name) VALUES (?, ?)";
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement deletePs = connection.prepareStatement(deleteSql)) {
                deletePs.setString(1, uuid.toString());
                deletePs.executeUpdate();
            }
            try (PreparedStatement insertPs = connection.prepareStatement(insertSql)) {
                for (String id : identifications) {
                    insertPs.setString(1, uuid.toString());
                    insertPs.setString(2, id);
                    insertPs.addBatch();
                }
                insertPs.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving identifications for " + uuid + ": " + e.getMessage());
            try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    public List<String> loadIdentifications(UUID uuid) {
        String sql = "SELECT id_name FROM identifications WHERE owner_uuid = ?";
        List<String> ids = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ids.add(rs.getString("id_name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading identifications for " + uuid + ": " + e.getMessage());
        }
        return ids;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error closing database: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}

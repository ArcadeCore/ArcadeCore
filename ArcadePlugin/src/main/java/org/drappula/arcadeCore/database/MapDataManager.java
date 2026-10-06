package org.drappula.arcadeCore.database;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.drappula.arcadeCore.ArcadeCore;
import org.drappula.arcadeCore.managers.map.ArcadeMap;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapDataManager {
    public static List<ArcadeMap> loadAll() throws SQLException {
        Map<String, List<Location>> spawnsByMap = new LinkedHashMap<>();
        Map<String, World> worldByMap = new LinkedHashMap<>();
        List<ArcadeMap> maps = new ArrayList<>();

        try (Statement stmt = Database.get().createStatement();
             ResultSet mapRows = stmt.executeQuery("SELECT map_id, world FROM maps")) {
            while (mapRows.next()) {
                String mapId = mapRows.getString("map_id");
                World world = Bukkit.getWorld(mapRows.getString("world"));
                if (world == null) {
                    ArcadeCore.get().getSLF4JLogger().warn("Skipping map {} because its world ({}) isn't loaded", mapId, mapRows.getString("world"));
                    continue;
                }
                worldByMap.put(mapId, world);
                spawnsByMap.put(mapId, new ArrayList<>());
            }
        }

        try (Statement stmt = Database.get().createStatement();
             ResultSet spawnRows = stmt.executeQuery("SELECT map_id, x, y, z, yaw, pitch FROM map_spawns ORDER BY map_id, spawn_index")) {
            while (spawnRows.next()) {
                String mapId = spawnRows.getString("map_id");
                World world = worldByMap.get(mapId);
                if (world == null) continue;
                spawnsByMap.get(mapId).add(new Location(world,
                        spawnRows.getDouble("x"), spawnRows.getDouble("y"), spawnRows.getDouble("z"),
                        spawnRows.getFloat("yaw"), spawnRows.getFloat("pitch")));
            }
        }

        try (PreparedStatement stmt = Database.get().prepareStatement(
                "SELECT map_id, game_id, display_name, world, enabled, in_use FROM maps")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String mapId = rs.getString("map_id");
                    if (!worldByMap.containsKey(mapId)) continue;
                    maps.add(new ArcadeMap(mapId, rs.getString("game_id"), rs.getString("display_name"),
                            rs.getString("world"), rs.getBoolean("enabled"), rs.getBoolean("in_use"),
                            spawnsByMap.get(mapId)));
                }
            }
        }
        Map<String, Map<String, String>> configsByMap = loadConfigs();
        for (ArcadeMap map : maps) {
            Map<String, String> config = configsByMap.get(map.getId());
            if (config != null) map.getConfigInternal().putAll(config);
        }
        return maps;
    }

    public static void create(String mapId, String gameId, String displayName, String world) throws SQLException {
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "INSERT INTO maps (map_id, game_id, display_name, world) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, mapId);
            stmt.setString(2, gameId);
            stmt.setString(3, displayName);
            stmt.setString(4, world);
            stmt.executeUpdate();
        }
    }

    public static void addSpawn(String mapId, int index, Location location) throws SQLException {
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "INSERT INTO map_spawns (map_id, spawn_index, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, mapId);
            stmt.setInt(2, index);
            stmt.setDouble(3, location.getX());
            stmt.setDouble(4, location.getY());
            stmt.setDouble(5, location.getZ());
            stmt.setFloat(6, location.getYaw());
            stmt.setFloat(7, location.getPitch());
            stmt.executeUpdate();
        }
    }

    public static int countSpawns(String mapId) throws SQLException {
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "SELECT COUNT(*) AS count FROM map_spawns WHERE map_id = ?")) {
            stmt.setString(1, mapId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt("count");
            }
        }
    }

    public static void setEnabled(String mapId, boolean enabled) throws SQLException {
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "UPDATE maps SET enabled = ? WHERE map_id = ?")) {
            stmt.setBoolean(1, enabled);
            stmt.setString(2, mapId);
            stmt.executeUpdate();
        }
    }

    public static void setInUse(String mapId, boolean inUse) throws SQLException {
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "UPDATE maps SET in_use = ? WHERE map_id = ?")) {
            stmt.setBoolean(1, inUse);
            stmt.setString(2, mapId);
            stmt.executeUpdate();
        }
    }

    public static void releaseAll() throws SQLException {
        try (Statement stmt = Database.get().createStatement()) {
            stmt.executeUpdate("UPDATE maps SET in_use = 0");
        }
    }

    public static void delete(String mapId) throws SQLException {
        try (PreparedStatement stmt = Database.get().prepareStatement("DELETE FROM maps WHERE map_id = ?")) {
            stmt.setString(1, mapId);
            stmt.executeUpdate();
        }
    }

    public static Map<String, Map<String, String>> loadConfigs() throws SQLException {
        Map<String, Map<String, String>> configsByMap = new LinkedHashMap<>();
        try (Statement stmt = Database.get().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT map_id, key, value FROM map_config")) {
            while (rs.next()) {
                configsByMap.computeIfAbsent(rs.getString("map_id"), id -> new LinkedHashMap<>())
                        .put(rs.getString("key"), rs.getString("value"));
            }
        }
        return configsByMap;
    }

    public static void setConfig(String mapId, String key, String value) throws SQLException {
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "INSERT INTO map_config (map_id, key, value) VALUES (?, ?, ?) " +
                        "ON CONFLICT(map_id, key) DO UPDATE SET value = excluded.value")) {
            stmt.setString(1, mapId);
            stmt.setString(2, key);
            stmt.setString(3, value);
            stmt.executeUpdate();
        }
    }

    public static void deleteConfig(String mapId, String key) throws SQLException {
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "DELETE FROM map_config WHERE map_id = ? AND key = ?")) {
            stmt.setString(1, mapId);
            stmt.setString(2, key);
            stmt.executeUpdate();
        }
    }

    public static void ensureConfig(String mapId, String key, String value) throws SQLException {
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "INSERT INTO map_config (map_id, key, value) VALUES (?, ?, ?) " +
                        "ON CONFLICT(map_id, key) DO NOTHING")) {
            stmt.setString(1, mapId);
            stmt.setString(2, key);
            stmt.setString(3, value);
            stmt.executeUpdate();
        }
    }
}

package org.ezinnovations.ezsurvivalstats.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class DatabaseManager {
    private final JavaPlugin plugin;
    private final File databaseFile;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "EZSurvivalStats-DB");
        thread.setDaemon(true);
        return thread;
    });

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "data.db");
    }

    public void initialize() throws SQLException {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_stats (
                        uuid TEXT PRIMARY KEY,
                        player_name TEXT,
                        playtime_seconds INTEGER NOT NULL DEFAULT 0,
                        joins INTEGER NOT NULL DEFAULT 0,
                        last_seen INTEGER NOT NULL DEFAULT 0,
                        deaths INTEGER NOT NULL DEFAULT 0,
                        total_mob_kills INTEGER NOT NULL DEFAULT 0,
                        total_blocks_mined INTEGER NOT NULL DEFAULT 0,
                        total_items_collected INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_mob_kills (
                        uuid TEXT NOT NULL,
                        entity_type TEXT NOT NULL,
                        kills INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (uuid, entity_type)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_blocks_mined (
                        uuid TEXT NOT NULL,
                        material TEXT NOT NULL,
                        mined INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (uuid, material)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_items_collected (
                        uuid TEXT NOT NULL,
                        material TEXT NOT NULL,
                        collected INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (uuid, material)
                    )
                    """);
        }
    }

    public void executeAsync(Runnable runnable) {
        executorService.submit(runnable);
    }

    public void upsertPlayerStats(PlayerStatsSnapshot snapshot) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                upsertTotals(connection, snapshot);
                upsertBreakdown(connection, snapshot.mobKills(), snapshot.uuid(), "player_mob_kills", "entity_type", "kills");
                upsertBreakdown(connection, snapshot.blocksMined(), snapshot.uuid(), "player_blocks_mined", "material", "mined");
                upsertBreakdown(connection, snapshot.itemsCollected(), snapshot.uuid(), "player_items_collected", "material", "collected");
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public PlayerStatsSnapshot loadPlayer(UUID uuid, String fallbackName) throws SQLException {
        try (Connection connection = getConnection()) {
            String playerName = fallbackName;
            long playtime = 0;
            long joins = 0;
            long lastSeen = 0;
            long deaths = 0;
            long totalKills = 0;
            long totalMined = 0;
            long totalCollected = 0;

            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM player_stats WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        playerName = rs.getString("player_name");
                        playtime = rs.getLong("playtime_seconds");
                        joins = rs.getLong("joins");
                        lastSeen = rs.getLong("last_seen");
                        deaths = rs.getLong("deaths");
                        totalKills = rs.getLong("total_mob_kills");
                        totalMined = rs.getLong("total_blocks_mined");
                        totalCollected = rs.getLong("total_items_collected");
                    }
                }
            }

            Map<String, Long> mobKills = loadBreakdown(connection, uuid, "player_mob_kills", "entity_type", "kills");
            Map<String, Long> blocksMined = loadBreakdown(connection, uuid, "player_blocks_mined", "material", "mined");
            Map<String, Long> itemsCollected = loadBreakdown(connection, uuid, "player_items_collected", "material", "collected");

            return new PlayerStatsSnapshot(uuid, playerName, playtime, joins, lastSeen, deaths, totalKills, totalMined, totalCollected,
                    mobKills, blocksMined, itemsCollected);
        }
    }

    public List<LeaderboardRow> queryTopTotals(String column) throws SQLException {
        List<LeaderboardRow> rows = new ArrayList<>();
        String sql = "SELECT player_name, " + column + " AS value FROM player_stats ORDER BY value DESC LIMIT 10";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(new LeaderboardRow(rs.getString("player_name"), rs.getLong("value")));
            }
        }
        return rows;
    }

    public List<LeaderboardRow> queryTopAggregated(String table, String valueColumn) throws SQLException {
        List<LeaderboardRow> rows = new ArrayList<>();
        String sql = """
                SELECT ps.player_name, COALESCE(SUM(t.%s), 0) AS value
                FROM %s t
                JOIN player_stats ps ON ps.uuid = t.uuid
                GROUP BY t.uuid
                ORDER BY value DESC
                LIMIT 10
                """.formatted(valueColumn, table);
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(new LeaderboardRow(rs.getString("player_name"), rs.getLong("value")));
            }
        }
        return rows;
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void upsertTotals(Connection connection, PlayerStatsSnapshot snapshot) throws SQLException {
        String sql = """
                INSERT INTO player_stats (uuid, player_name, playtime_seconds, joins, last_seen, deaths, total_mob_kills, total_blocks_mined, total_items_collected)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    player_name = excluded.player_name,
                    playtime_seconds = excluded.playtime_seconds,
                    joins = excluded.joins,
                    last_seen = excluded.last_seen,
                    deaths = excluded.deaths,
                    total_mob_kills = excluded.total_mob_kills,
                    total_blocks_mined = excluded.total_blocks_mined,
                    total_items_collected = excluded.total_items_collected
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, snapshot.uuid().toString());
            statement.setString(2, snapshot.playerName());
            statement.setLong(3, snapshot.playtimeSeconds());
            statement.setLong(4, snapshot.joins());
            statement.setLong(5, snapshot.lastSeen());
            statement.setLong(6, snapshot.deaths());
            statement.setLong(7, snapshot.totalMobKills());
            statement.setLong(8, snapshot.totalBlocksMined());
            statement.setLong(9, snapshot.totalItemsCollected());
            statement.executeUpdate();
        }
    }

    private void upsertBreakdown(Connection connection, Map<String, Long> map, UUID uuid, String table, String keyColumn, String valueColumn)
            throws SQLException {
        if (map.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO %s (uuid, %s, %s) VALUES (?, ?, ?) ON CONFLICT(uuid, %s) DO UPDATE SET %s = excluded.%s"
                .formatted(table, keyColumn, valueColumn, keyColumn, valueColumn, valueColumn);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Map.Entry<String, Long> entry : map.entrySet()) {
                statement.setString(1, uuid.toString());
                statement.setString(2, entry.getKey());
                statement.setLong(3, entry.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private Map<String, Long> loadBreakdown(Connection connection, UUID uuid, String table, String keyColumn, String valueColumn)
            throws SQLException {
        Map<String, Long> values = new HashMap<>();
        String sql = "SELECT " + keyColumn + ", " + valueColumn + " FROM " + table + " WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    values.put(rs.getString(keyColumn), rs.getLong(valueColumn));
                }
            }
        }
        return values;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
    }

    public record LeaderboardRow(String playerName, long value) {
    }
}

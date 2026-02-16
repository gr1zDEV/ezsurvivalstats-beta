package org.ezinnovations.ezsurvivalstats.leaderboard;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.ezinnovations.ezsurvivalstats.storage.DatabaseManager;
import org.ezinnovations.ezsurvivalstats.util.ConfigManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class LeaderboardService {
    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final Map<LeaderboardType, List<LeaderboardEntry>> cache = new EnumMap<>(LeaderboardType.class);

    public LeaderboardService(JavaPlugin plugin, DatabaseManager databaseManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.configManager = configManager;
    }

    public void start() {
        long intervalTicks = configManager.getLeaderboardRefreshMinutes() * 60L * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::refreshAsync, 20L, intervalTicks);
    }

    public void refreshAsync() {
        databaseManager.executeAsync(() -> {
            try {
                cache.put(LeaderboardType.PLAYTIME, mapRows(databaseManager.queryTopTotals("playtime_seconds")));
                cache.put(LeaderboardType.TOTAL_MOB_KILLS, mapRows(databaseManager.queryTopTotals("total_mob_kills")));
                cache.put(LeaderboardType.TOTAL_BLOCKS_MINED, mapRows(databaseManager.queryTopTotals("total_blocks_mined")));
                cache.put(LeaderboardType.TOTAL_ITEMS_COLLECTED, mapRows(databaseManager.queryTopTotals("total_items_collected")));
                cache.put(LeaderboardType.DEATHS, mapRows(databaseManager.queryTopTotals("deaths")));
            } catch (SQLException ex) {
                plugin.getLogger().warning("Failed refreshing leaderboards: " + ex.getMessage());
            }
        });
    }

    public List<LeaderboardEntry> get(LeaderboardType type) {
        return cache.getOrDefault(type, List.of());
    }

    private List<LeaderboardEntry> mapRows(List<DatabaseManager.LeaderboardRow> rows) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (DatabaseManager.LeaderboardRow row : rows) {
            entries.add(new LeaderboardEntry(row.playerName() == null ? "Unknown" : row.playerName(), row.value()));
        }
        return entries;
    }
}

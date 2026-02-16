package org.ezinnovations.ezsurvivalstats.tracking;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.ezinnovations.ezsurvivalstats.storage.DatabaseManager;
import org.ezinnovations.ezsurvivalstats.storage.PlayerStats;
import org.ezinnovations.ezsurvivalstats.storage.PlayerStatsSnapshot;
import org.ezinnovations.ezsurvivalstats.util.ConfigManager;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class StatsService {
    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;

    private final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();
    private final Set<UUID> loadedFromDatabase = ConcurrentHashMap.newKeySet();

    public StatsService(JavaPlugin plugin, DatabaseManager databaseManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.configManager = configManager;
    }

    public void startFlushTask() {
        long ticks = configManager.getFlushIntervalSeconds() * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushAllAsync, ticks, ticks);
    }

    public PlayerStats getOrCreate(UUID uuid, String name) {
        return playerStats.computeIfAbsent(uuid, key -> new PlayerStats(uuid, name));
    }

    public void loadPlayerAsync(UUID uuid, String fallbackName) {
        if (loadedFromDatabase.contains(uuid)) {
            return;
        }
        PlayerStats stats = getOrCreate(uuid, fallbackName);
        databaseManager.executeAsync(() -> {
            try {
                PlayerStatsSnapshot snapshot = databaseManager.loadPlayer(uuid, fallbackName);
                synchronized (stats) {
                    if (!loadedFromDatabase.contains(uuid)) {
                        stats.mergeBase(snapshot);
                        loadedFromDatabase.add(uuid);
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().warning("Failed to load stats for " + uuid + ": " + ex.getMessage());
            }
        });
    }

    public void loadSnapshotAsync(UUID uuid, String fallbackName, Consumer<PlayerStatsSnapshot> callback) {
        databaseManager.executeAsync(() -> {
            try {
                PlayerStatsSnapshot snapshot = databaseManager.loadPlayer(uuid, fallbackName);
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(snapshot));
            } catch (SQLException ex) {
                plugin.getLogger().warning("Failed to load snapshot for " + uuid + ": " + ex.getMessage());
            }
        });
    }


    public void cacheLoadedSnapshot(PlayerStatsSnapshot snapshot) {
        PlayerStats stats = getOrCreate(snapshot.uuid(), snapshot.playerName() == null ? "Unknown" : snapshot.playerName());
        synchronized (stats) {
            if (!loadedFromDatabase.contains(snapshot.uuid())) {
                stats.mergeBase(snapshot);
                loadedFromDatabase.add(snapshot.uuid());
            }
        }
    }
    public PlayerStatsSnapshot getSnapshot(UUID uuid, String fallbackName) {
        PlayerStats stats = playerStats.get(uuid);
        if (stats != null) {
            return stats.snapshot();
        }
        return new PlayerStatsSnapshot(uuid, fallbackName, 0, 0, 0, 0, 0, 0, 0, Map.of(), Map.of(), Map.of());
    }

    public void flushPlayer(UUID uuid) {
        PlayerStats stats = playerStats.get(uuid);
        if (stats == null) {
            return;
        }
        PlayerStatsSnapshot snapshot = stats.snapshot();
        databaseManager.executeAsync(() -> persistSnapshot(snapshot));
    }

    public void flushAllAsync() {
        for (PlayerStats stats : playerStats.values()) {
            PlayerStatsSnapshot snapshot = stats.snapshot();
            databaseManager.executeAsync(() -> persistSnapshot(snapshot));
        }
    }

    public void onDisableFlushOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            flushPlayer(player.getUniqueId());
        }
        flushAllAsync();
    }

    private void persistSnapshot(PlayerStatsSnapshot snapshot) {
        try {
            databaseManager.upsertPlayerStats(snapshot);
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to flush stats for " + snapshot.uuid() + ": " + ex.getMessage());
        }
    }
}

package org.ezinnovations.ezsurvivalstats.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigManager {
    private final JavaPlugin plugin;

    private int flushIntervalSeconds;
    private int leaderboardRefreshMinutes;
    private boolean trackCreative;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        this.flushIntervalSeconds = Math.max(5, config.getInt("flush_interval_seconds", 30));
        this.leaderboardRefreshMinutes = Math.max(1, config.getInt("leaderboard_refresh_minutes", 5));
        this.trackCreative = config.getBoolean("track_creative", false);
    }

    public int getFlushIntervalSeconds() {
        return flushIntervalSeconds;
    }

    public int getLeaderboardRefreshMinutes() {
        return leaderboardRefreshMinutes;
    }

    public boolean isTrackCreative() {
        return trackCreative;
    }

    public String getGuiTitle(String key, String fallback) {
        return plugin.getConfig().getString("gui_titles." + key, fallback);
    }
}

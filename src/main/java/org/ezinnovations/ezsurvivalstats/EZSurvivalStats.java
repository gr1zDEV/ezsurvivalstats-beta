package org.ezinnovations.ezsurvivalstats;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.ezinnovations.ezsurvivalstats.commands.EzStatsCommand;
import org.ezinnovations.ezsurvivalstats.commands.StatsCommand;
import org.ezinnovations.ezsurvivalstats.commands.TopCommand;
import org.ezinnovations.ezsurvivalstats.gui.MenuListener;
import org.ezinnovations.ezsurvivalstats.gui.MenuManager;
import org.ezinnovations.ezsurvivalstats.leaderboard.LeaderboardService;
import org.ezinnovations.ezsurvivalstats.placeholder.EZStatsPlaceholderExpansion;
import org.ezinnovations.ezsurvivalstats.storage.DatabaseManager;
import org.ezinnovations.ezsurvivalstats.tracking.PlayerTrackingListener;
import org.ezinnovations.ezsurvivalstats.tracking.StatsService;
import org.ezinnovations.ezsurvivalstats.util.ConfigManager;
import org.ezinnovations.ezsurvivalstats.util.MessageManager;

import java.sql.SQLException;
import java.util.Objects;

public final class EZSurvivalStats extends JavaPlugin {
    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private StatsService statsService;
    private LeaderboardService leaderboardService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        this.configManager = new ConfigManager(this);
        this.configManager.reload();

        this.messageManager = new MessageManager(this);
        this.messageManager.reload();

        this.databaseManager = new DatabaseManager(this);
        try {
            databaseManager.initialize();
        } catch (SQLException ex) {
            getLogger().severe("Could not initialize SQLite database: " + ex.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.statsService = new StatsService(this, databaseManager, configManager);
        this.leaderboardService = new LeaderboardService(this, databaseManager, configManager);

        MenuManager menuManager = new MenuManager(statsService, leaderboardService, configManager, messageManager);
        registerCommands(menuManager);
        Bukkit.getPluginManager().registerEvents(new PlayerTrackingListener(statsService, configManager), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(menuManager), this);

        statsService.startFlushTask();
        leaderboardService.start();
        leaderboardService.refreshAsync();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new EZStatsPlaceholderExpansion(statsService).register();
            getLogger().info("Hooked into PlaceholderAPI.");
        }

        getLogger().info("EZSurvivalStats enabled.");
    }

    @Override
    public void onDisable() {
        if (statsService != null) {
            statsService.onDisableFlushOnlinePlayers();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        configManager.reload();
        messageManager.reload();
        leaderboardService.refreshAsync();
    }

    private void registerCommands(MenuManager menuManager) {
        StatsCommand statsCommand = new StatsCommand(menuManager, statsService, messageManager);
        TopCommand topCommand = new TopCommand(menuManager, messageManager);
        EzStatsCommand ezStatsCommand = new EzStatsCommand(this, messageManager);

        PluginCommand stats = Objects.requireNonNull(getCommand("stats"));
        stats.setExecutor(statsCommand);
        stats.setTabCompleter(statsCommand);

        PluginCommand top = Objects.requireNonNull(getCommand("top"));
        top.setExecutor(topCommand);

        PluginCommand ezstats = Objects.requireNonNull(getCommand("ezstats"));
        ezstats.setExecutor(ezStatsCommand);
        ezstats.setTabCompleter(ezStatsCommand);
    }
}

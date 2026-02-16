package org.ezinnovations.ezsurvivalstats.tracking;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.ezinnovations.ezsurvivalstats.storage.PlayerStats;
import org.ezinnovations.ezsurvivalstats.util.ConfigManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerTrackingListener implements Listener {
    private final StatsService statsService;
    private final ConfigManager configManager;
    private final Map<UUID, Long> sessionStart = new ConcurrentHashMap<>();

    public PlayerTrackingListener(StatsService statsService, ConfigManager configManager) {
        this.statsService = statsService;
        this.configManager = configManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        sessionStart.put(player.getUniqueId(), System.currentTimeMillis());
        PlayerStats stats = statsService.getOrCreate(player.getUniqueId(), player.getName());
        stats.setPlayerName(player.getName());
        stats.incrementJoins();
        stats.setLastSeen(System.currentTimeMillis());
        statsService.loadPlayerAsync(player.getUniqueId(), player.getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Long joinedAt = sessionStart.remove(player.getUniqueId());
        PlayerStats stats = statsService.getOrCreate(player.getUniqueId(), player.getName());
        if (joinedAt != null) {
            long deltaSeconds = Math.max(0, (System.currentTimeMillis() - joinedAt) / 1000L);
            stats.addPlaytimeSeconds(deltaSeconds);
        }
        stats.setLastSeen(System.currentTimeMillis());
        statsService.flushPlayer(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (skipCreative(player)) {
            return;
        }
        statsService.getOrCreate(player.getUniqueId(), player.getName()).incrementDeaths();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || skipCreative(killer)) {
            return;
        }
        statsService.getOrCreate(killer.getUniqueId(), killer.getName()).incrementMobKill(event.getEntityType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (skipCreative(player)) {
            return;
        }
        statsService.getOrCreate(player.getUniqueId(), player.getName())
                .incrementBlockMined(event.getBlock().getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (skipCreative(player)) {
            return;
        }
        statsService.getOrCreate(player.getUniqueId(), player.getName())
                .incrementItemCollected(event.getItem().getItemStack().getType().name(), event.getItem().getItemStack().getAmount());
    }

    private boolean skipCreative(Player player) {
        return !configManager.isTrackCreative() && player.getGameMode() == GameMode.CREATIVE;
    }
}

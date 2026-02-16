package org.ezinnovations.ezsurvivalstats.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.ezinnovations.ezsurvivalstats.storage.PlayerStatsSnapshot;
import org.ezinnovations.ezsurvivalstats.tracking.StatsService;
import org.jetbrains.annotations.NotNull;

public final class EZStatsPlaceholderExpansion extends PlaceholderExpansion {
    private final StatsService statsService;

    public EZStatsPlaceholderExpansion(StatsService statsService) {
        this.statsService = statsService;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ezstats";
    }

    @Override
    public @NotNull String getAuthor() {
        return "EZInnovations";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || player.getUniqueId() == null) {
            return "0";
        }
        PlayerStatsSnapshot snapshot = statsService.getSnapshot(player.getUniqueId(), player.getName());
        return switch (params.toLowerCase()) {
            case "playtime" -> String.valueOf(snapshot.playtimeSeconds());
            case "total_kills" -> String.valueOf(snapshot.totalMobKills());
            case "total_mined" -> String.valueOf(snapshot.totalBlocksMined());
            case "total_collected" -> String.valueOf(snapshot.totalItemsCollected());
            case "deaths" -> String.valueOf(snapshot.deaths());
            default -> null;
        };
    }
}

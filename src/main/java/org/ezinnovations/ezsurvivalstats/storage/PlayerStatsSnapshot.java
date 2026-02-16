package org.ezinnovations.ezsurvivalstats.storage;

import java.util.Map;
import java.util.UUID;

public record PlayerStatsSnapshot(
        UUID uuid,
        String playerName,
        long playtimeSeconds,
        long joins,
        long lastSeen,
        long deaths,
        long totalMobKills,
        long totalBlocksMined,
        long totalItemsCollected,
        Map<String, Long> mobKills,
        Map<String, Long> blocksMined,
        Map<String, Long> itemsCollected
) {
}

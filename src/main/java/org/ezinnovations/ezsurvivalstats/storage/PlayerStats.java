package org.ezinnovations.ezsurvivalstats.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerStats {
    private final UUID uuid;
    private String playerName;

    private long playtimeSeconds;
    private long joins;
    private long lastSeen;
    private long deaths;
    private long totalMobKills;
    private long totalBlocksMined;
    private long totalItemsCollected;

    private final Map<String, Long> mobKills = new HashMap<>();
    private final Map<String, Long> blocksMined = new HashMap<>();
    private final Map<String, Long> itemsCollected = new HashMap<>();

    public PlayerStats(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
    }

    public synchronized void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public synchronized void addPlaytimeSeconds(long seconds) {
        this.playtimeSeconds += Math.max(0, seconds);
    }

    public synchronized void incrementJoins() {
        this.joins++;
    }

    public synchronized void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public synchronized void incrementDeaths() {
        this.deaths++;
    }

    public synchronized void incrementMobKill(String entityType) {
        this.totalMobKills++;
        mobKills.merge(entityType, 1L, Long::sum);
    }

    public synchronized void incrementBlockMined(String material) {
        this.totalBlocksMined++;
        blocksMined.merge(material, 1L, Long::sum);
    }

    public synchronized void incrementItemCollected(String material, long amount) {
        long safeAmount = Math.max(1, amount);
        this.totalItemsCollected += safeAmount;
        itemsCollected.merge(material, safeAmount, Long::sum);
    }

    public synchronized void mergeBase(PlayerStatsSnapshot base) {
        this.playerName = base.playerName() != null ? base.playerName() : playerName;
        this.playtimeSeconds += base.playtimeSeconds();
        this.joins += base.joins();
        this.lastSeen = Math.max(this.lastSeen, base.lastSeen());
        this.deaths += base.deaths();
        this.totalMobKills += base.totalMobKills();
        this.totalBlocksMined += base.totalBlocksMined();
        this.totalItemsCollected += base.totalItemsCollected();
        base.mobKills().forEach((k, v) -> mobKills.merge(k, v, Long::sum));
        base.blocksMined().forEach((k, v) -> blocksMined.merge(k, v, Long::sum));
        base.itemsCollected().forEach((k, v) -> itemsCollected.merge(k, v, Long::sum));
    }

    public synchronized PlayerStatsSnapshot snapshot() {
        return new PlayerStatsSnapshot(
                uuid,
                playerName,
                playtimeSeconds,
                joins,
                lastSeen,
                deaths,
                totalMobKills,
                totalBlocksMined,
                totalItemsCollected,
                Map.copyOf(mobKills),
                Map.copyOf(blocksMined),
                Map.copyOf(itemsCollected)
        );
    }
}

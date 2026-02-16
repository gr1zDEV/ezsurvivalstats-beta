package org.ezinnovations.ezsurvivalstats.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.ezinnovations.ezsurvivalstats.leaderboard.LeaderboardEntry;
import org.ezinnovations.ezsurvivalstats.leaderboard.LeaderboardService;
import org.ezinnovations.ezsurvivalstats.leaderboard.LeaderboardType;
import org.ezinnovations.ezsurvivalstats.storage.PlayerStatsSnapshot;
import org.ezinnovations.ezsurvivalstats.tracking.StatsService;
import org.ezinnovations.ezsurvivalstats.util.ConfigManager;
import org.ezinnovations.ezsurvivalstats.util.ItemBuilder;
import org.ezinnovations.ezsurvivalstats.util.MessageManager;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MenuManager {
    private static final int[] CONTENT_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

    private final StatsService statsService;
    private final LeaderboardService leaderboardService;
    private final ConfigManager configManager;
    private final MessageManager messages;

    public MenuManager(StatsService statsService, LeaderboardService leaderboardService, ConfigManager configManager, MessageManager messages) {
        this.statsService = statsService;
        this.leaderboardService = leaderboardService;
        this.configManager = configManager;
        this.messages = messages;
    }

    public void openMain(Player viewer, UUID target, String targetName) {
        PlayerStatsSnapshot snapshot = statsService.getSnapshot(target, targetName);
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.MAIN, target, targetName, 0), 54,
                messages.colorize(configManager.getGuiTitle("main", "&aStatistics")));

        inv.setItem(13, head(target, "&a" + targetName,
                List.of(
                        "&7Playtime: &f" + snapshot.playtimeSeconds() + "s",
                        "&7Joins: &f" + snapshot.joins(),
                        "&7Deaths: &f" + snapshot.deaths()
                )));

        inv.setItem(20, new ItemBuilder(Material.CLOCK).name(messages.colorize("&eGeneral")).build());
        inv.setItem(22, new ItemBuilder(Material.IRON_SWORD).name(messages.colorize("&cCombat")).build());
        inv.setItem(24, new ItemBuilder(Material.CHEST).name(messages.colorize("&bCollections")).build());
        inv.setItem(31, new ItemBuilder(Material.GOLD_INGOT).name(messages.colorize("&6Leaderboards")).build());
        viewer.openInventory(inv);
    }

    public void openGeneral(Player viewer, UUID target, String targetName) {
        PlayerStatsSnapshot snapshot = statsService.getSnapshot(target, targetName);
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.GENERAL, target, targetName, 0), 54,
                messages.colorize(configManager.getGuiTitle("general", "&eGeneral Stats")));

        String lastSeen = snapshot.lastSeen() <= 0 ? "Never" :
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
                        .format(Instant.ofEpochMilli(snapshot.lastSeen()));

        inv.setItem(20, new ItemBuilder(Material.CLOCK).name(messages.colorize("&ePlaytime")).lore(List.of("&f" + snapshot.playtimeSeconds() + "s")).build());
        inv.setItem(22, new ItemBuilder(Material.OAK_DOOR).name(messages.colorize("&eJoins")).lore(List.of("&f" + snapshot.joins())).build());
        inv.setItem(24, new ItemBuilder(Material.SKELETON_SKULL).name(messages.colorize("&eDeaths")).lore(List.of("&f" + snapshot.deaths())).build());
        inv.setItem(31, new ItemBuilder(Material.PAPER).name(messages.colorize("&eLast Seen")).lore(List.of("&f" + lastSeen)).build());
        inv.setItem(49, new ItemBuilder(Material.BARRIER).name(messages.colorize("&cBack")).build());
        viewer.openInventory(inv);
    }

    public void openCombat(Player viewer, UUID target, String targetName, int page) {
        PlayerStatsSnapshot snapshot = statsService.getSnapshot(target, targetName);
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.COMBAT, target, targetName, page), 54,
                messages.colorize(configManager.getGuiTitle("combat", "&cCombat Stats")));
        pagedMaterialMap(inv, snapshot.mobKills(), page, "&c", Material.ZOMBIE_HEAD);
        viewer.openInventory(inv);
    }

    public void openCollections(Player viewer, UUID target, String targetName) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.COLLECTIONS, target, targetName, 0), 54,
                messages.colorize(configManager.getGuiTitle("collections", "&bCollections")));
        inv.setItem(21, new ItemBuilder(Material.DIAMOND_PICKAXE).name(messages.colorize("&bBlocks Mined")).build());
        inv.setItem(23, new ItemBuilder(Material.HOPPER).name(messages.colorize("&bItems Collected")).build());
        inv.setItem(49, new ItemBuilder(Material.BARRIER).name(messages.colorize("&cBack")).build());
        viewer.openInventory(inv);
    }

    public void openBlocks(Player viewer, UUID target, String targetName, int page) {
        PlayerStatsSnapshot snapshot = statsService.getSnapshot(target, targetName);
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.BLOCKS, target, targetName, page), 54,
                messages.colorize(configManager.getGuiTitle("blocks_mined", "&bBlocks Mined")));
        pagedMaterialMap(inv, snapshot.blocksMined(), page, "&b", Material.STONE);
        viewer.openInventory(inv);
    }

    public void openItems(Player viewer, UUID target, String targetName, int page) {
        PlayerStatsSnapshot snapshot = statsService.getSnapshot(target, targetName);
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.ITEMS, target, targetName, page), 54,
                messages.colorize(configManager.getGuiTitle("items_collected", "&bItems Collected")));
        pagedMaterialMap(inv, snapshot.itemsCollected(), page, "&b", Material.CHEST);
        viewer.openInventory(inv);
    }

    public void openLeaderboards(Player viewer) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.LEADERBOARDS, viewer.getUniqueId(), viewer.getName(), 0), 54,
                messages.colorize(configManager.getGuiTitle("leaderboards", "&6Leaderboards")));
        inv.setItem(10, leaderboardItem(Material.CLOCK, "&6Playtime", leaderboardService.get(LeaderboardType.PLAYTIME)));
        inv.setItem(12, leaderboardItem(Material.IRON_SWORD, "&6Total Mob Kills", leaderboardService.get(LeaderboardType.TOTAL_MOB_KILLS)));
        inv.setItem(14, leaderboardItem(Material.DIAMOND_PICKAXE, "&6Total Blocks Mined", leaderboardService.get(LeaderboardType.TOTAL_BLOCKS_MINED)));
        inv.setItem(16, leaderboardItem(Material.CHEST, "&6Total Items Collected", leaderboardService.get(LeaderboardType.TOTAL_ITEMS_COLLECTED)));
        inv.setItem(31, leaderboardItem(Material.SKELETON_SKULL, "&6Deaths", leaderboardService.get(LeaderboardType.DEATHS)));
        viewer.openInventory(inv);
    }

    private void pagedMaterialMap(Inventory inv, Map<String, Long> map, int page, String color, Material fallback) {
        List<Map.Entry<String, Long>> entries = new ArrayList<>(map.entrySet());
        entries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        int perPage = CONTENT_SLOTS.length;
        int maxPage = Math.max(0, (entries.size() - 1) / perPage);
        int safePage = Math.max(0, Math.min(maxPage, page));
        int start = safePage * perPage;

        for (int i = 0; i < perPage && start + i < entries.size(); i++) {
            Map.Entry<String, Long> entry = entries.get(start + i);
            Material material = Material.matchMaterial(entry.getKey());
            if (material == null) {
                material = fallback;
            }
            inv.setItem(CONTENT_SLOTS[i], new ItemBuilder(material)
                    .name(messages.colorize(color + entry.getKey()))
                    .lore(List.of(messages.colorize("&f" + entry.getValue())))
                    .build());
        }

        if (safePage > 0) {
            inv.setItem(45, new ItemBuilder(Material.ARROW).name(messages.colorize("&ePrevious Page")).build());
        }
        if (safePage < maxPage) {
            inv.setItem(53, new ItemBuilder(Material.ARROW).name(messages.colorize("&eNext Page")).build());
        }
        inv.setItem(49, new ItemBuilder(Material.BARRIER).name(messages.colorize("&cBack")).build());
    }

    private ItemStack head(UUID uuid, String name, List<String> lore) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        if (meta != null) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            meta.setOwningPlayer(offlinePlayer);
            meta.setDisplayName(messages.colorize(name));
            meta.setLore(lore.stream().map(messages::colorize).toList());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack leaderboardItem(Material icon, String title, List<LeaderboardEntry> entries) {
        List<String> lore = new ArrayList<>();
        if (entries.isEmpty()) {
            lore.add("&7Leaderboard cache is warming up...");
        } else {
            for (int i = 0; i < entries.size(); i++) {
                LeaderboardEntry entry = entries.get(i);
                lore.add("&e" + (i + 1) + ". &f" + entry.playerName() + " &7- &a" + entry.value());
            }
        }
        return new ItemBuilder(icon).name(messages.colorize(title)).lore(lore.stream().map(messages::colorize).toList()).build();
    }
}

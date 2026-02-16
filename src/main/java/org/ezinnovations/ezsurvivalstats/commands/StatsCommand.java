package org.ezinnovations.ezsurvivalstats.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ezinnovations.ezsurvivalstats.gui.MenuManager;
import org.ezinnovations.ezsurvivalstats.tracking.StatsService;
import org.ezinnovations.ezsurvivalstats.util.MessageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StatsCommand implements CommandExecutor, TabCompleter {
    private final MenuManager menuManager;
    private final StatsService statsService;
    private final MessageManager messages;

    public StatsCommand(MenuManager menuManager, StatsService statsService, MessageManager messages) {
        this.menuManager = menuManager;
        this.statsService = statsService;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("errors.players_only"));
            return true;
        }

        if (args.length == 0) {
            menuManager.openMain(player, player.getUniqueId(), player.getName());
            return true;
        }

        if (!player.hasPermission("ezsurvivalstats.view.others")) {
            messages.send(player, "errors.no_permission");
            return true;
        }

        String targetName = args[0];
        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            menuManager.openMain(player, online.getUniqueId(), online.getName());
            return true;
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(targetName);
        if (offline == null || offline.getUniqueId() == null) {
            messages.send(player, "errors.player_not_found");
            return true;
        }

        player.sendMessage(messages.getRaw("loading", "&7Loading stats..."));
        statsService.loadSnapshotAsync(offline.getUniqueId(), targetName, snapshot -> {
            statsService.cacheLoadedSnapshot(snapshot);
            menuManager.openMain(player, snapshot.uuid(), snapshot.playerName() == null ? targetName : snapshot.playerName());
        });
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) {
            return List.of();
        }

        List<String> options = new ArrayList<>();
        String input = args[0].toLowerCase(Locale.ROOT);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase(Locale.ROOT).startsWith(input)) {
                options.add(player.getName());
            }
        }
        return options;
    }
}

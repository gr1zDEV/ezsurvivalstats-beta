package org.ezinnovations.ezsurvivalstats.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.ezinnovations.ezsurvivalstats.gui.MenuManager;
import org.ezinnovations.ezsurvivalstats.util.MessageManager;

public final class TopCommand implements CommandExecutor {
    private final MenuManager menuManager;
    private final MessageManager messages;

    public TopCommand(MenuManager menuManager, MessageManager messages) {
        this.menuManager = menuManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("errors.players_only"));
            return true;
        }
        menuManager.openLeaderboards(player);
        return true;
    }
}

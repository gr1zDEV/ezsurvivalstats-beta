package org.ezinnovations.ezsurvivalstats.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ezinnovations.ezsurvivalstats.EZSurvivalStats;
import org.ezinnovations.ezsurvivalstats.util.MessageManager;

import java.util.List;

public final class EzStatsCommand implements CommandExecutor, TabCompleter {
    private final EZSurvivalStats plugin;
    private final MessageManager messages;

    public EzStatsCommand(EZSurvivalStats plugin, MessageManager messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("ezsurvivalstats.admin")) {
                messages.send(sender, "errors.no_permission");
                return true;
            }
            plugin.reloadPlugin();
            messages.send(sender, "commands.reload_success");
            return true;
        }
        sender.sendMessage(messages.getRaw("commands.reload_usage", "&cUsage: /ezstats reload"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reload");
        }
        return List.of();
    }
}

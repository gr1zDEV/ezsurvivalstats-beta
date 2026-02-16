package org.ezinnovations.ezsurvivalstats.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class MessageManager {
    private final JavaPlugin plugin;
    private FileConfiguration messages;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public String get(String path) {
        String prefix = colorize(messages.getString("prefix", "&7[&aEZStats&7] "));
        String raw = messages.getString(path, path);
        return colorize(prefix + raw);
    }

    public String getRaw(String path, String fallback) {
        return colorize(messages.getString(path, fallback));
    }

    public void send(CommandSender sender, String path) {
        sender.sendMessage(get(path));
    }

    public String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}

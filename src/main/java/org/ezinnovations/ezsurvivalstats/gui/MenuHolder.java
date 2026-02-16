package org.ezinnovations.ezsurvivalstats.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public record MenuHolder(MenuType type, UUID target, String targetName, int page) implements InventoryHolder {
    @Override
    public Inventory getInventory() {
        return null;
    }
}

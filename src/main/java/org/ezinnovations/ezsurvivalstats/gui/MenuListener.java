package org.ezinnovations.ezsurvivalstats.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class MenuListener implements Listener {
    private final MenuManager menuManager;

    public MenuListener(MenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);

        int slot = event.getRawSlot();
        switch (holder.type()) {
            case MAIN -> {
                if (slot == 20) menuManager.openGeneral(player, holder.target(), holder.targetName());
                if (slot == 22) menuManager.openCombat(player, holder.target(), holder.targetName(), 0);
                if (slot == 24) menuManager.openCollections(player, holder.target(), holder.targetName());
                if (slot == 31) menuManager.openLeaderboards(player);
            }
            case GENERAL, LEADERBOARDS -> {
                if (slot == 49) menuManager.openMain(player, holder.target(), holder.targetName());
            }
            case COLLECTIONS -> {
                if (slot == 21) menuManager.openBlocks(player, holder.target(), holder.targetName(), 0);
                if (slot == 23) menuManager.openItems(player, holder.target(), holder.targetName(), 0);
                if (slot == 49) menuManager.openMain(player, holder.target(), holder.targetName());
            }
            case COMBAT -> handlePaged(player, holder, slot, MenuType.COMBAT);
            case BLOCKS -> handlePaged(player, holder, slot, MenuType.BLOCKS);
            case ITEMS -> handlePaged(player, holder, slot, MenuType.ITEMS);
        }
    }

    private void handlePaged(Player player, MenuHolder holder, int slot, MenuType type) {
        if (slot == 45) {
            openByType(player, holder.target(), holder.targetName(), type, holder.page() - 1);
        } else if (slot == 53) {
            openByType(player, holder.target(), holder.targetName(), type, holder.page() + 1);
        } else if (slot == 49) {
            if (type == MenuType.COMBAT) {
                menuManager.openMain(player, holder.target(), holder.targetName());
            } else {
                menuManager.openCollections(player, holder.target(), holder.targetName());
            }
        }
    }

    private void openByType(Player player, java.util.UUID target, String targetName, MenuType type, int page) {
        switch (type) {
            case COMBAT -> menuManager.openCombat(player, target, targetName, page);
            case BLOCKS -> menuManager.openBlocks(player, target, targetName, page);
            case ITEMS -> menuManager.openItems(player, target, targetName, page);
            default -> {
            }
        }
    }
}

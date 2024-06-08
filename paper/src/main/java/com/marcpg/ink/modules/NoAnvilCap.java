package com.marcpg.ink.modules;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class NoAnvilCap implements Listener {
    private static final int VANILLA_CAP = 39;
    private final Map<AnvilInventory, Integer> realMaxRepairCosts = new HashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onPrepareAnvil(@NotNull PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();

        if (!realMaxRepairCosts.containsKey(inv))
            realMaxRepairCosts.put(inv, inv.getMaximumRepairCost());

        if (inv.getRepairCost() > VANILLA_CAP) {
            int scaledCost = (int) (Math.log(inv.getRepairCost() - VANILLA_CAP + 1) * 10) + VANILLA_CAP; // Example scaling factor
            inv.setRepairCost(scaledCost);
        }

        inv.setMaximumRepairCost(Integer.MAX_VALUE);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (event.getInventory() instanceof AnvilInventory inv)
            realMaxRepairCosts.remove(inv);
    }
}

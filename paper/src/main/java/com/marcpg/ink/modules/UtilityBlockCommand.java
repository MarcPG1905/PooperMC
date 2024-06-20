package com.marcpg.ink.modules;

import com.marcpg.libpg.lang.Translation;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public final class UtilityBlockCommand implements TabExecutor, Listener {
    public static final Map<UUID, Inventory> TRASH_INVENTORIES = new HashMap<>();

    public static final UtilityBlockCommand ANVIL = new UtilityBlockCommand(p -> p.openAnvil(p.getLocation(), true));
    public static final UtilityBlockCommand CARTOGRAPHY_TABLE = new UtilityBlockCommand(p -> p.openCartographyTable(p.getLocation(), true));
    public static final UtilityBlockCommand WORKBENCH = new UtilityBlockCommand(p -> p.openWorkbench(p.getLocation(), true));
    public static final UtilityBlockCommand GRINDSTONE = new UtilityBlockCommand(p -> p.openGrindstone(p.getLocation(), true));
    public static final UtilityBlockCommand LOOM = new UtilityBlockCommand(p -> p.openLoom(p.getLocation(), true));
    public static final UtilityBlockCommand SMITHING_TABLE = new UtilityBlockCommand(p -> p.openSmithingTable(p.getLocation(), true));
    public static final UtilityBlockCommand STONECUTTER = new UtilityBlockCommand(p -> p.openStonecutter(p.getLocation(), true));
    public static final UtilityBlockCommand ENCHANTING = new UtilityBlockCommand(p -> p.openEnchanting(p.getLocation(), true));
    public static final UtilityBlockCommand ENDER_CHEST = new UtilityBlockCommand(p -> p.openInventory(p.getEnderChest()));
    public static final UtilityBlockCommand TRASH = new UtilityBlockCommand(p -> {
        Inventory inv = Bukkit.createInventory(p, 27, Translation.component(p.locale(), "module.utility-block-commands.trash"));
        p.openInventory(inv);
        TRASH_INVENTORIES.put(p.getUniqueId(), inv);
    });


    private final Consumer<Player> openAction;

    protected UtilityBlockCommand(Consumer<Player> openAction) {
        this.openAction = openAction;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            openAction.accept(player);
        } else {
            sender.sendMessage(Translation.component(Locale.getDefault(), "cmd.only_players"));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return List.of();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (TRASH_INVENTORIES.containsKey(player.getUniqueId())) {
            int items = 0;
            for (ItemStack item : TRASH_INVENTORIES.get(player.getUniqueId()).getContents()) {
                items += item != null ? item.getAmount() : 0;
            }
            player.sendMessage(Translation.component(player.locale(), "modules.utility-block-commands.trash.confirm", items));
        }
    }
}

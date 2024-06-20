package com.marcpg.ink.modules;

import com.marcpg.ink.Ink;
import com.marcpg.ink.InkPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class VeinMining implements Listener {
    public static final List<Material> veinBlocks = new ArrayList<>();
    public static boolean requireProperTool;
    public static boolean animated;
    public static int maximumDistance;

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        if (!event.getPlayer().isSneaking() && veinBlocks.contains(event.getBlock().getType())) {
            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
            if (requireProperTool && event.getBlock().getDrops(item).isEmpty()) return;
            mineVein(event.getBlock().getType(), item, event.getPlayer(), new HashSet<>(Set.of(event.getBlock())), new AtomicInteger());
        }
    }

    private void mineVein(Material ore, @NotNull ItemStack item, Player player, Set<Block> lastBlocks, AtomicInteger steps) {
        if (item.getType() == Material.AIR || item.getAmount() <= 0) return;
        if (steps.getAndIncrement() >= maximumDistance) return;

        Set<Block> blocks = new HashSet<>();
        for (Block last : lastBlocks) {
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        Block block = last.getRelative(x, y, z);
                        if (block.getType() != ore) continue;

                        block.breakNaturally(item, true);
                        if (damageItem(item, player)) return;

                        blocks.add(block);
                    }
                }
            }
        }
        if (animated) {
            Bukkit.getScheduler().runTaskLater((InkPlugin) Ink.PLUGIN, () -> mineVein(ore, item, player, blocks, steps), 3);
        } else {
            mineVein(ore, item, player, blocks, steps);
        }
    }

    private boolean damageItem(@NotNull ItemStack item, Player player) {
        if (item.getType() == Material.AIR || item.getAmount() == 0) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            damageable.damage(1, player);
            item.setItemMeta(meta);
            return damageable.getHealth() <= 0;
        }
        return false;
    }
}

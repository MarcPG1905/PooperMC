package com.marcpg.ink.features;

import com.marcpg.libpg.lang.Translation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class AntiBookBan implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onEntityPickupItem(@NotNull EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        long itemSize = getItemSize(event.getItem().getItemStack());
        long addedInvSize = itemSize + Arrays.stream(player.getInventory().getContents()).mapToLong(AntiBookBan::getItemSize).sum();

        if (itemSize > 9269 || addedInvSize > 27448) {
            event.setCancelled(true);
            player.sendMessage(Translation.component(player.locale(), "anti_book_ban.pickup").color(NamedTextColor.YELLOW));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerEditBook(@NotNull PlayerEditBookEvent event) {
        String text = PlainComponentSerializer.plain().serializeOr(Component.join(JoinConfiguration.noSeparators(), event.getNewBookMeta().pages()), "");
        if (text.getBytes(StandardCharsets.UTF_8).length > 8000) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Translation.component(event.getPlayer().locale(), "anti_book_ban.edit_book").color(NamedTextColor.RED));
        }
    }

    private static long getItemSize(ItemStack item) {
        if (item == null || item.getAmount() == 0 || item.getType() == Material.AIR) return 0;
        long size = 0;

        if (item.getItemMeta() instanceof BlockStateMeta bsm && bsm.getBlockState() instanceof ShulkerBox shulker) {
            size += Arrays.stream(shulker.getInventory().getContents()).mapToLong(AntiBookBan::getItemSize).sum();
        }
        size += item.serialize().toString().getBytes(StandardCharsets.UTF_8).length;

        return size;
    }
}

package com.marcpg.ink.modules;

import com.marcpg.common.Pooper;
import com.marcpg.libpg.lang.Translation;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class GlobalEnderChest implements TabExecutor {
    private static final Inventory ENDER_CHEST = Bukkit.createInventory(null, 27, Component.translatable("container.enderchest"));
    private static final File FILE = Pooper.DATA_DIR.resolve("data/global-ec.yml").toFile();

    public void open(@NotNull Player player) {
        player.openInventory(ENDER_CHEST);
        player.playSound(player.getLocation(), "minecraft:block.ender_chest.open", SoundCategory.BLOCKS, 0.5f, 1f);
    }

    public static void load() throws IOException {
        if (FILE.createNewFile()) Pooper.LOG.info("Created /data/global-ec.yml, as it didn't exist before!");
        try (BukkitObjectInputStream data = new BukkitObjectInputStream(new FileInputStream(FILE))) {
            for (int i = 0; i < 27; i++) {
                ENDER_CHEST.setItem(i, (ItemStack) data.readObject());
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void save() {
        try (BukkitObjectOutputStream data = new BukkitObjectOutputStream(new FileOutputStream(FILE))) {
            for (int i = 0; i < 27; i++) {
                data.writeObject(ENDER_CHEST.getItem(i));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            open(player);
        } else {
            sender.sendMessage(Translation.component(Locale.getDefault(), "cmd.only_players"));
        }
        return true;
    }

    @Override
    public @Unmodifiable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return List.of();
    }
}

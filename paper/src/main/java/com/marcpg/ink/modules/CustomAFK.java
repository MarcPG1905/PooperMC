package com.marcpg.ink.modules;

import com.marcpg.libpg.lang.Translation;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class CustomAFK implements Listener, TabExecutor {
    public static final List<Player> AFK_PLAYERS = new ArrayList<>();

    public void enable(Player player) {
        AFK_PLAYERS.add(player);
        player.setCustomName(Objects.requireNonNullElse(player.getCustomName(), player.getName()) + " [AFK]");
        player.addScoreboardTag("afk");
        player.sendMessage(Translation.component(player.locale(), "module.custom_afk.confirm"));
    }

    public void disable(Player player) {
        AFK_PLAYERS.remove(player);
        player.setCustomName(Objects.requireNonNullElse(player.getCustomName(), player.getName()).replace(" [AFK]", ""));
        player.removeScoreboardTag("afk");
        player.sendMessage(Translation.component(player.locale(), "module.custom_afk.disable"));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        if (AFK_PLAYERS.contains(event.getPlayer()))
            disable(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        if (event.hasChangedPosition() && AFK_PLAYERS.contains(event.getPlayer()))
            disable(event.getPlayer());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (AFK_PLAYERS.contains(player)) {
                player.sendMessage(Translation.component(player.locale(), "module.custom_afk.already"));
            } else {
                enable(player);
            }
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

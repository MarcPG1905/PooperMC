package com.marcpg.ink.moderation;

import com.marcpg.ink.common.PaperPlayer;
import com.marcpg.libpg.data.time.Time;
import com.marcpg.libpg.lang.Translation;
import com.marcpg.libpg.text.Completer;
import com.marcpg.common.entity.OfflinePlayer;
import com.marcpg.common.moderation.Banning;
import com.marcpg.common.moderation.Muting;
import com.marcpg.common.util.InvalidCommandArgsException;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;

@SuppressWarnings("deprecation") // Bukkit#getOfflinePlayer(String)
public class PaperMuting implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onAsyncChat(@NotNull AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Locale l = player.locale();

        if (!Muting.STORAGE.contains(event.getPlayer().getUniqueId())) return;

        Map<String, Object> mute = Muting.STORAGE.get(uuid);

        if (player.hasPermission("poo.mute") || player.hasPermission("poo.admin")) {
            Muting.STORAGE.remove(uuid);
        } else if ((System.currentTimeMillis() * 0.001) > (Long) mute.get("expires")) {
            Muting.STORAGE.remove(uuid);
            player.sendMessage(Translation.component(l, "moderation.mute.expired.msg").color(NamedTextColor.GREEN));
        } else {
            event.message(Component.empty());
            event.setCancelled(true);
            player.sendMessage(Translation.component(l, "moderation.mute.warning").color(NamedTextColor.RED));
            player.sendMessage(Translation.component(l, "moderation.expiration", Time.preciselyFormat((Long) mute.get("expires") - Instant.now().getEpochSecond())).color(NamedTextColor.GOLD));
            player.sendMessage(Translation.component(l, "moderation.reason", mute.get("reason")).color(NamedTextColor.GOLD));
        }
    }

    public static class MuteCommand implements TabExecutor {
        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length < 3) return false;

            Locale l = sender instanceof Player player ? player.locale() : Locale.getDefault();
            String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            Time time = Time.parse(args[1]);

            try {
                if (target.isOnline()) {
                    Muting.mute(sender instanceof Player player ? player.getName() : "Console",
                            new PaperPlayer(target.getPlayer()), time, reason);
                } else {
                    Muting.mute(sender instanceof Player player ? player.getName() : "Console",
                            new OfflinePlayer(target.getName(), target.getUniqueId()), time, reason);
                }
                sender.sendMessage(Translation.component(l, "moderation.mute.confirm", args[0], time.getPreciselyFormatted(), reason).color(NamedTextColor.YELLOW));
            } catch (InvalidCommandArgsException e) {
                sender.sendMessage(e.translatable(l));
            }
            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length == 1) {
                List<UUID> excluded = new ArrayList<>(Muting.STORAGE.getAll().stream().map(m -> (UUID) m.get("player")).toList());
                if (sender instanceof Player player)
                    excluded.add(player.getUniqueId());

                return Completer.semiSmartComplete(args[0], Arrays.stream(Bukkit.getOfflinePlayers())
                        .filter(offlinePlayer -> !excluded.contains(offlinePlayer.getUniqueId()))
                        .map(org.bukkit.OfflinePlayer::getName)
                        .toList()
                );
            } else if (args.length == 2) {
                return Muting.TIME_UNITS.stream()
                        .map(unit -> args[1].replaceAll("[^-\\d.]+", "") + unit)
                        .toList();
            }
            return List.of();
        }
    }

    public static class UnmuteCommand implements TabExecutor {
        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length != 1) return false;

            Locale l = sender instanceof Player player ? player.locale() : Locale.getDefault();
            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

            try {
                Banning.pardon(sender instanceof Player player ? player.getName() : "Console", new OfflinePlayer(target.getName(), target.getUniqueId()));
                sender.sendMessage(Translation.component(l, "moderation.unmute.confirm", args[0]).color(NamedTextColor.YELLOW));
            } catch (InvalidCommandArgsException e) {
                sender.sendMessage(e.translatable(l));
            }
            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length == 1) {
                return Completer.semiSmartComplete(args[0], Banning.STORAGE.getAll().stream()
                        .map(m -> (UUID) m.get("player"))
                        .map(Bukkit::getOfflinePlayer)
                        .map(org.bukkit.OfflinePlayer::getName)
                        .toList());
            }
            return List.of();
        }
    }
}

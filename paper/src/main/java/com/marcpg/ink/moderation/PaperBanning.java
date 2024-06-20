package com.marcpg.ink.moderation;

import com.marcpg.common.entity.OfflinePlayer;
import com.marcpg.common.moderation.Banning;
import com.marcpg.common.util.InvalidCommandArgsException;
import com.marcpg.ink.common.PaperPlayer;
import com.marcpg.libpg.data.time.Time;
import com.marcpg.libpg.lang.Translation;
import com.marcpg.libpg.text.Completer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;

@SuppressWarnings("deprecation") // Bukkit#getOfflinePlayer(String)
public final class PaperBanning implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onPlayerLogin(@NotNull PlayerLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Locale l = player.locale();

        if (!Banning.STORAGE.contains(uuid)) return;

        Map<String, Object> ban = Banning.STORAGE.get(uuid);

        if (player.hasPermission("poo.ban") || player.hasPermission("poo.admin")) {
            Banning.STORAGE.remove(uuid);
        } else if ((Boolean) ban.get("permanent") && (System.currentTimeMillis() * 0.001) > (Long) ban.get("expires")) {
            Banning.STORAGE.remove(uuid);
            player.sendMessage(Translation.component(l, "moderation.ban.expired.msg").color(NamedTextColor.GREEN));
        } else {
            Component reason = LegacyComponentSerializer.legacySection().deserialize((String) ban.get("reason"));
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, Translation.component(l, "moderation.ban.join.title").color(NamedTextColor.RED)
                    .appendNewline().appendNewline()
                    .append(Translation.component(l, "moderation.expiration", "").color(NamedTextColor.GRAY)
                            .append((Boolean) ban.get("permanent") ? Translation.component(l, "moderation.time.permanent").color(NamedTextColor.RED) :
                                    Component.text(Time.preciselyFormat((Long) ban.get("expires") - Instant.now().getEpochSecond()), NamedTextColor.BLUE)))
                    .appendNewline()
                    .append(Translation.component(l, "moderation.reason", "").color(NamedTextColor.GRAY).append(reason.hasStyling() ? reason : reason.color(NamedTextColor.BLUE)))
            );
        }
    }

    public static class BanCommand implements TabExecutor {
        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length < 3) return false;

            Locale l = sender instanceof Player player ? player.locale() : Locale.getDefault();
            String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            boolean permanent = args[1].equalsIgnoreCase("permanent");
            Time time = permanent ? new Time(0) : Time.parse(args[1]);

            try {
                if (target.isOnline()) {
                    Banning.ban(sender instanceof Player player ? player.getName() : "Console",
                            new PaperPlayer(target.getPlayer()), permanent, time, Component.text(reason));
                } else {
                    Banning.ban(sender instanceof Player player ? player.getName() : "Console",
                            new OfflinePlayer(target.getName(), target.getUniqueId()), permanent, time, reason);
                }
                sender.sendMessage(Translation.component(l, "moderation.ban.confirm", args[0], permanent ? Translation.string(l, "moderation.time.permanent") : time.getPreciselyFormatted(), reason).color(NamedTextColor.YELLOW));
            } catch (InvalidCommandArgsException e) {
                sender.sendMessage(e.translatable(l));
            }
            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length == 1) {
                List<UUID> excluded = new ArrayList<>(Banning.STORAGE.getAll().stream().map(m -> (UUID) m.get("player")).toList());
                if (sender instanceof Player player)
                    excluded.add(player.getUniqueId());

                return Completer.semiSmartComplete(args[0], Arrays.stream(Bukkit.getOfflinePlayers())
                        .filter(offlinePlayer -> !excluded.contains(offlinePlayer.getUniqueId()))
                        .map(org.bukkit.OfflinePlayer::getName)
                        .toList()
                );
            } else if (args.length == 2) {
                List<String> suggestions = new ArrayList<>();
                Banning.TIME_UNITS.forEach(unit -> suggestions.add(args[1].replaceAll("[^-\\d.]+", "") + unit));
                suggestions.add("permanent");
                return suggestions;
            }
            return List.of();
        }
    }

    public static class PardonCommand implements TabExecutor {
        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length != 1) return false;

            Locale l = sender instanceof Player player ? player.locale() : Locale.getDefault();
            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

            try {
                Banning.pardon(sender instanceof Player player ? player.getName() : "Console", new OfflinePlayer(target.getName(), target.getUniqueId()));
                sender.sendMessage(Translation.component(l, "moderation.pardon.confirm", args[0]).color(NamedTextColor.YELLOW));
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

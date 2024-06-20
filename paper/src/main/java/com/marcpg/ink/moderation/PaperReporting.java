package com.marcpg.ink.moderation;

import com.marcpg.libpg.lang.Translation;
import com.marcpg.libpg.text.Completer;
import com.marcpg.ink.common.PaperPlayer;
import com.marcpg.common.moderation.Reporting;
import com.marcpg.common.util.InvalidCommandArgsException;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("deprecation") // Bukkit#getOfflinePlayer(String)
public final class PaperReporting implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 3) return false;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Translation.component(Locale.getDefault(), "cmd.only_players").color(NamedTextColor.RED));
            return false;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        if (!target.hasPlayedBefore()) {
            player.sendMessage(Translation.component(player.locale(), "cmd.player_not_found", args[0]).color(NamedTextColor.RED));
            return true;
        }

        try {
            Reporting.report(
                    new PaperPlayer(player),
                    new com.marcpg.common.entity.OfflinePlayer(args[0], target.getUniqueId()),
                    args[1],
                    String.join(" ", Arrays.copyOfRange(args, 2, args.length))
            );
            player.sendMessage(Translation.component(player.locale(), "report.confirm").color(NamedTextColor.YELLOW));
        } catch (InvalidCommandArgsException e) {
            player.sendMessage(e.translatable(player.locale()));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 1) {
                String sourceName = player.getName();
                return Completer.semiSmartComplete(args[0], Arrays.stream(Bukkit.getOfflinePlayers())
                        .map(OfflinePlayer::getName)
                        .filter(p -> !sourceName.equals(p))
                        .toList());
            } else if (args.length == 2) {
                return Completer.semiSmartComplete(args[1], Reporting.REASONS);
            }
        }
        return List.of();
    }
}

package com.marcpg.ink.moderation;

import com.marcpg.libpg.lang.Translation;
import com.marcpg.libpg.text.Completer;
import com.marcpg.ink.common.PaperPlayer;
import com.marcpg.common.moderation.Kicking;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PaperKicking implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) return false;

        Locale l = sender instanceof Player player ? player.locale() : Locale.getDefault();
        Player target = Bukkit.getPlayer(args[0]);
        if (target != null) {
            String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            Kicking.kick(sender instanceof Player player ? player.getName() : "Console", new PaperPlayer(target), reason);
            sender.sendMessage(Translation.component(l, "moderation.kick.confirm", args[0], reason).color(NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Translation.component(l, "cmd.player_not_found", args[0]).color(NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Completer.semiSmartComplete(args[0], Bukkit.getOnlinePlayers().parallelStream().filter(p -> p != sender).map(Player::getName).toList());
        }
        return List.of();
    }
}

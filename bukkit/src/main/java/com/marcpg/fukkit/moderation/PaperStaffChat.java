package com.marcpg.fukkit.moderation;

import com.marcpg.common.features.MessageLogging;
import com.marcpg.fukkit.common.PaperPlayer;
import com.marcpg.libpg.lang.Translation;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Locale;

public class PaperStaffChat implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) return false;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Translation.component(Locale.getDefault(), "cmd.only_players").color(NamedTextColor.RED));
            return false;
        }

        PaperPlayer paperPlayer = PaperPlayer.ofPlayer(player);
        String message = String.join(" ", args);

        MessageLogging.saveMessage(paperPlayer, new MessageLogging.MessageData(new Date(), message, MessageLogging.MessageData.Type.STAFF, null));
        Bukkit.getOnlinePlayers().parallelStream()
                .filter(p -> p.hasPermission("poo.staff"))
                .forEach(p -> p.sendMessage(Translation.component(p.locale(), "staff_chat.message", paperPlayer.name(), message).color(NamedTextColor.BLUE)));

        return true;
    }
}

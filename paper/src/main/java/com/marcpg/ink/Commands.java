package com.marcpg.ink;

import com.marcpg.libpg.lang.Translation;
import com.marcpg.libpg.text.Completer;
import com.marcpg.common.features.MessageLogging;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Commands {
    public static class MsgHistCommand implements TabExecutor {
        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length == 1 && sender.hasPermission("poo.msg-hist")) {
                Locale l = sender instanceof Player player ? player.locale() : Locale.getDefault();

                String target = args[0];
                OfflinePlayer targetPlayer = Bukkit.getOfflinePlayerIfCached(target);
                if (targetPlayer == null) {
                    sender.sendMessage(Translation.component(l, "cmd.player_not_found", target).color(NamedTextColor.RED));
                    return true;
                }
                if (MessageLogging.noHistory(targetPlayer.getUniqueId())) {
                    sender.sendMessage(Translation.component(l, "moderation.chat_history.no_history", target).color(NamedTextColor.RED));
                    return true;
                }

                sender.sendMessage(Translation.component(l, "moderation.chat_history.title", target).color(NamedTextColor.DARK_GREEN));
                MessageLogging.getHistory(targetPlayer.getUniqueId()).forEach(msg -> {
                    String time = "[" + DateTimeFormatter.ofPattern("MMMM d, HH:mm").format(ZonedDateTime.ofInstant(msg.time().toInstant(), ZoneId.of("UTC"))) + " UTC] ";
                    String additional = switch (msg.type()) {
                        case NORMAL -> "";
                        case STAFF -> Translation.string(l, "moderation.chat_history.staff") + " ";
                        case PRIVATE -> Translation.string(l, "moderation.chat_history.private", msg.receiver()) + " ";
                        case PARTY -> Translation.string(l, "moderation.chat_history.party") + " ";
                    };
                    sender.sendMessage(Component.text("| " + time + additional, NamedTextColor.GRAY).append(Component.text(msg.content().strip(), NamedTextColor.WHITE)));
                });
                sender.sendMessage(Component.text("=========================").color(NamedTextColor.DARK_GREEN));

                return true;
            }
            return false;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            return args.length != 1 ? List.of() : Completer.semiSmartComplete(args[0], Arrays.stream(Bukkit.getOfflinePlayers())
                    .filter(sender::equals)
                    .map(OfflinePlayer::getName)
                    .toList());
        }
    }
}

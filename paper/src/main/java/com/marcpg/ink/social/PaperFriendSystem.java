package com.marcpg.ink.social;

import com.marcpg.common.entity.IdentifiablePlayer;
import com.marcpg.common.entity.OfflinePlayer;
import com.marcpg.common.social.FriendSystem;
import com.marcpg.common.util.InvalidCommandArgsException;
import com.marcpg.common.util.ThrowingBiConsumer;
import com.marcpg.ink.common.PaperPlayer;
import com.marcpg.libpg.lang.Translation;
import com.marcpg.libpg.text.Completer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.kyori.adventure.text.format.NamedTextColor.*;

public class PaperFriendSystem implements TabExecutor {
    private static final List<String> SUBCOMMANDS = List.of("add", "remove", "accept", "deny", "list");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || !SUBCOMMANDS.contains(args[0])) return false;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Translation.component(Locale.getDefault(), "cmd.only_players").color(RED));
            return false;
        }

        if (args[0].equals("list")) {
            UUID playerUuid = player.getUniqueId();

            List<Map<String, Object>> friendships = FriendSystem.getFriendships(playerUuid);

            if (friendships.isEmpty()) {
                player.sendMessage(Translation.component(player.locale(), "friend.list.none").color(YELLOW));
            } else {
                player.sendMessage(Translation.component(player.locale(), "friend.list.list", friendships.size()).color(GREEN));
                friendships.forEach(m -> {
                    UUID friend = (UUID) (m.get("player1").equals(playerUuid) ? m.get("player2") : m.get("player1"));
                    player.sendMessage(Component.text("- " + Bukkit.getOfflinePlayer(friend).getName()));
                });
            }
        } else {
            if (args.length != 2) return false;
            switch (args[0]) {
                case "add" -> handle(player, args[1], args[0], FriendSystem::add);
                case "remove" -> handle(player, args[1], args[0], FriendSystem::remove);
                case "accept" -> handle(player, args[1], args[0], FriendSystem::accept);
                case "deny" -> handle(player, args[1], args[0], FriendSystem::deny);
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Completer.startComplete(args[0], SUBCOMMANDS);
        } else if (args.length == 2) {
            String playerName = sender.getName();
            UUID playerUuid = ((Player) sender).getUniqueId();
            return switch (args[1]) {
                case "add" -> Arrays.stream(Bukkit.getOfflinePlayers()).parallel()
                        .map(org.bukkit.OfflinePlayer::getName)
                        .filter(p -> !playerName.equals(p))
                        .toList();
                case "remove" -> FriendSystem.getFriendships(((Player) sender).getUniqueId()).stream()
                        .map(m -> Bukkit.getOfflinePlayer((UUID) (m.get("player1").equals(playerUuid) ? m.get("player2") : m.get("player1"))).getName())
                        .toList();
                case "accept", "deny" -> {
                    if (!FriendSystem.REQUESTS.containsKey(playerUuid)) {
                        yield List.of();
                    } else {
                        yield FriendSystem.REQUESTS.get(playerUuid).stream()
                                .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                                .toList();
                    }
                }
                default -> List.of();
            };
        }
        return List.of();
    }

    public static void handle(Player player, String target, String operationName, ThrowingBiConsumer<PaperPlayer, IdentifiablePlayer, InvalidCommandArgsException> operation) {
        UUID targetUuid = Bukkit.getPlayerUniqueId(target);
        if (targetUuid == null) {
            player.sendMessage(Translation.component(player.locale(), "cmd.player_not_found", target).color(RED));
        } else {
            try {
                Player p = Bukkit.getPlayer(targetUuid);
                operation.accept(PaperPlayer.ofPlayer(player), p != null ? new PaperPlayer(p) : new OfflinePlayer(target, targetUuid));
                player.sendMessage(Translation.component(player.locale(), "friend." + operationName + ".confirm", target).color(YELLOW));
            } catch (InvalidCommandArgsException e) {
                player.sendMessage(e.translatable(player.locale()));
            }
        }
    }
}

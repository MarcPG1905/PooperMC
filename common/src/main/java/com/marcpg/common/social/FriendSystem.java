package com.marcpg.common.social;

import com.marcpg.common.entity.IdentifiablePlayer;
import com.marcpg.common.entity.OnlinePlayer;
import com.marcpg.common.storage.DatabaseStorage;
import com.marcpg.common.storage.RamStorage;
import com.marcpg.common.storage.Storage;
import com.marcpg.common.storage.YamlStorage;
import com.marcpg.common.util.InvalidCommandArgsException;
import com.marcpg.libpg.lang.Translation;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FriendSystem {
    public static final HashMap<UUID, HashSet<UUID>> REQUESTS = new HashMap<>();
    public static final Storage<UUID> STORAGE = Storage.storageType.createStorage("friendships", "uuid");

    public static void add(@NotNull OnlinePlayer<?> player, @NotNull IdentifiablePlayer target) throws InvalidCommandArgsException {
        if (getFriendship(player.uuid(), target.uuid()) != null)
            throw new InvalidCommandArgsException("friend.already_friends", target.name());

        if ((REQUESTS.containsKey(player.uuid()) && REQUESTS.get(player.uuid()).contains(target.uuid())) ||
                (REQUESTS.containsKey(target.uuid()) && REQUESTS.get(target.uuid()).contains(player.uuid())))
            throw new InvalidCommandArgsException("friend.add.already_requested", target.name());

        if (REQUESTS.containsKey(target.uuid())) {
            REQUESTS.get(target.uuid()).add(player.uuid());
        } else {
            REQUESTS.put(target.uuid(), new HashSet<>(Set.of(player.uuid())));
        }

        player.sendMessage(Translation.component(player.locale(), "friend.add.confirm", target.name()).color(NamedTextColor.GREEN));

        if (target instanceof OnlinePlayer<?> t) {
            t.sendMessage(Translation.component(t.locale(), "friend.add.msg.1", player.name()).color(NamedTextColor.GREEN).appendSpace()
                    .append(Translation.component(t.locale(), "friend.add.msg.2").color(NamedTextColor.YELLOW)
                            .hoverEvent(HoverEvent.showText(Translation.component(t.locale(), "friend.add.msg.2.tooltip")))
                            .clickEvent(ClickEvent.runCommand("/friend accept " + player.name()))
                    ).appendSpace()
                    .append(Translation.component(t.locale(), "friend.add.msg.3").color(NamedTextColor.GREEN))
            );
        }
    }

    public static void remove(@NotNull OnlinePlayer<?> player, @NotNull IdentifiablePlayer target) throws InvalidCommandArgsException {
        UUID friendship = getFriendship(player.uuid(), target.uuid());
        if (friendship == null)
            throw new InvalidCommandArgsException("friend.not_friends", target.name());
        STORAGE.remove(friendship);

        if (target instanceof OnlinePlayer<?> t)
            t.sendMessage(Translation.component(t.locale(), "friend.remove.confirm", player.name()).color(NamedTextColor.YELLOW));
    }

    public static void accept(@NotNull OnlinePlayer<?> player, IdentifiablePlayer target) throws InvalidCommandArgsException {
        handleRequests(REQUESTS.get(player.uuid()), player.uuid(), target);
        STORAGE.add(Map.of("uuid", UUID.randomUUID(),  "player1", player.uuid(),  "player2", player.uuid()));
        if (target instanceof OnlinePlayer<?> t)
            t.sendMessage(Translation.component(t.locale(), "friend.accept.confirm", player.name()).color(NamedTextColor.YELLOW));
    }

    public static void deny(@NotNull OnlinePlayer<?> player, IdentifiablePlayer target) throws InvalidCommandArgsException {
        handleRequests(REQUESTS.get(player.uuid()), player.uuid(), target);
        player.sendMessage(Translation.component(player.locale(), "friend.deny.confirm", target.name()).color(NamedTextColor.YELLOW));
        if (target instanceof OnlinePlayer<?> t)
            t.sendMessage(Translation.component(t.locale(), "friend.deny.msg", player.name()).color(NamedTextColor.YELLOW));
    }

    private static void handleRequests(HashSet<UUID> requests, UUID player, @NotNull IdentifiablePlayer target) throws InvalidCommandArgsException {
        if (requests == null || !requests.contains(target.uuid()))
            throw new InvalidCommandArgsException("friend.accept_deny.not_requested", target.name());

        requests.remove(target.uuid());
        if (requests.isEmpty()) REQUESTS.remove(player);

        if (getFriendship(player, target.uuid()) != null)
            throw new InvalidCommandArgsException("friend.already_friends", target.name());
    }

    public static List<Map<String, Object>> getFriendships(UUID player) {
        if (STORAGE instanceof DatabaseStorage<UUID> databaseStorage) {
            return List.copyOf(databaseStorage.get("player1 = ? OR player2 = ?", player, player));
        } else if (STORAGE instanceof RamStorage<UUID> ramStorage) {
            return List.copyOf(ramStorage.get(m -> m.get("player1").equals(player) || m.get("player2").equals(player)));
        } else if (STORAGE instanceof YamlStorage<UUID> yamlStorage) {
            return List.copyOf(yamlStorage.get(m -> m.get("player1").equals(player) || m.get("player2").equals(player)));
        } else {
            return List.of();
        }
    }

    private static @Nullable UUID getFriendship(UUID player1, UUID player2) {
        List<Map<String, Object>> maps = List.of();

        if (STORAGE instanceof DatabaseStorage<UUID> databaseStorage) {
            maps = List.copyOf(databaseStorage.get("player1 = ? AND player2 = ? OR player2 = ? AND player1 = ?", player1, player2, player1, player2));
        } else if (STORAGE instanceof RamStorage<UUID> ramStorage) {
            maps = List.copyOf(ramStorage.get(m -> (m.get("player1").equals(player1) && m.get("player2").equals(player2)) || (m.get("player2").equals(player1) && m.get("player1").equals(player2))));
        } else if (STORAGE instanceof YamlStorage<UUID> yamlStorage) {
            maps = List.copyOf(yamlStorage.get(m -> (m.get("player1").equals(player1) && m.get("player2").equals(player2)) || (m.get("player2").equals(player1) && m.get("player1").equals(player2))));
        }

        return maps.isEmpty() ? null : (UUID) maps.get(0).get("uuid");
    }
}

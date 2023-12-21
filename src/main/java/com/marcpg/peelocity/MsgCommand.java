package com.marcpg.peelocity;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MsgCommand implements SimpleCommand {
    public static final Map<UUID, String> PENDING_MESSAGES = new HashMap<>();
    public static final Map<UUID, UUID> LAST_SEND_RECEIVERS = new HashMap<>();

    @Override
    public void execute(@NotNull Invocation invocation) {
        String[] args = invocation.arguments();

        if (!(invocation.source() instanceof Player sender)) {
            invocation.source().sendMessage(Component.text("Only players can use this command!", TextColor.color(255, 0, 0)));
        } else {
            Optional<Player> receiverOptional = Peelocity.SERVER.getPlayer(args[0]);
            if (receiverOptional.isPresent()) {
                args[0] = "";
                receiverOptional.get().sendMessage(Component.text(String.format("[FROM: %s] %s", sender.getUsername(), String.join(" ", args))));
                LAST_SEND_RECEIVERS.put(sender.getUniqueId(), receiverOptional.get().getUniqueId());
            } else {
                if (LAST_SEND_RECEIVERS.containsKey(sender.getUniqueId())) {
                    Optional<Player> receiver = Peelocity.SERVER.getPlayer(LAST_SEND_RECEIVERS.get(sender.getUniqueId()));
                    if (receiver.isPresent()) {
                        receiver.get().sendMessage(Component.text(String.format("[FROM: %s] %s", sender.getUsername(), String.join(" ", args))));
                    } else {
                        sender.sendMessage(Component.text("The player '" + args[0] + "' could not be found!", TextColor.color(255, 0, 0)));
                    }
                } else {
                    sender.sendMessage(Component.text("The player '" + args[0] + "' could not be found!", TextColor.color(255, 0, 0)));
                }
            }
        }
    }

    @Override
    public List<String> suggest(@NotNull Invocation invocation) {
        if (invocation.arguments().length == 1) {
            return Peelocity.SERVER.getAllPlayers().stream().map(Player::getUsername).toList();
        }
        return List.of();
    }
}

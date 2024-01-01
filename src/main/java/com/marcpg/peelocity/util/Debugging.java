package com.marcpg.peelocity.util;

import com.marcpg.peelocity.Peelocity;
import com.marcpg.peelocity.PlayerEvents;
import com.marcpg.peelocity.chat.MessageLogging;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Debugging implements SimpleCommand {
    @Override
    public void execute(@NotNull Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args[0].equals("history") && args.length == 2) {
            Optional<Player> target = Peelocity.SERVER.getPlayer(args[1]);
            if (target.isPresent()) {
                List<MessageLogging.MessageData> history = MessageLogging.getHistory(target.get());
                if (history == null || history.isEmpty()) {
                    source.sendMessage(Component.text("The player '" + args[1] + "' does not have any chat history!", TextColor.color(255, 127, 0)));
                } else {
                    source.sendMessage(Component.text("⸺⸺⸺⸺⸺⸺⸺⸺⸺⸺", TextColor.color(80, 133, 80)));
                    source.sendMessage(Component.text(args[1] + "'s message history:", TextColor.color(0, 180, 0)));
                    for (MessageLogging.MessageData data : history) {
                        String additional = data.type() == MessageLogging.MessageData.Type.NORMAL ? "" : (data.type() == MessageLogging.MessageData.Type.PARTY ? "(Party Chat)" : (data.type() == MessageLogging.MessageData.Type.STAFF ? "(Staff Chat)" : "(Private -> " + data.receiver() + ")"));
                        source.sendMessage(Component.text(MessageLogging.DATE_FORMAT.format(data.time()) + additional + ": " + data.content().replace(" \\==|==\\==|== ", " || "), TextColor.color(180, 255, 180)));
                    }
                    source.sendMessage(Component.text("⸺⸺⸺⸺⸺⸺⸺⸺⸺⸺", TextColor.color(80, 133, 80)));
                }
            } else {
                source.sendMessage(Component.text("The player '" + args[1] + "' was not found!", TextColor.color(255, 64, 0)));
            }
        } else if (args[0].equals("allow-player") && args.length == 2) {
            Optional<Player> target = Peelocity.SERVER.getPlayer(args[1]);
            if (target.isPresent()) {
                PlayerEvents.ALLOWED_USERS.add(target.get().getUsername());
            } else {
                source.sendMessage(Component.text("The player '" + args[1] + "' was not found!", TextColor.color(255, 64, 0)));
            }
        } else {
            source.sendMessage(Component.text("WRONG YOU STUPID NIGGER!!!", TextColor.color(255, 0, 0)));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) {
            return Stream.of("history", "allow-player")
                    .filter(string -> string.startsWith(args[0]))
                    .toList();
        } else if (args.length == 2 && (args[0].equals("history") || args[0].equals("allow-player"))) {
            return Peelocity.SERVER.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(string -> string.startsWith(args[1]))
                    .toList();
        }

        return List.of();
    }
}

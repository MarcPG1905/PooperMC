package com.marcpg.peelocity.moderation;

import com.marcpg.peelocity.Peelocity;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

public class Kicks {
    public static @NotNull BrigadierCommand createKickBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("kick")
                .requires(source -> source.hasPermission("pee.kick"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Peelocity.SERVER.getAllPlayers().stream()
                                    .filter(player -> !player.hasPermission("pee.kick") && player != context.getSource())
                                    .map(Player::getUsername)
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("reason", StringArgumentType.greedyString())
                                .executes(context -> {
                                    CommandSource source = context.getSource();
                                    Peelocity.SERVER.getPlayer(context.getArgument("player", String.class)).ifPresentOrElse(
                                            target -> {
                                                String reason = context.getArgument("reason", String.class);

                                                target.disconnect(Component.text("You were kicked off the server!", NamedTextColor.GOLD)
                                                        .appendNewline().appendNewline()
                                                        .append(Component.text("Reason: ", NamedTextColor.GRAY).append(Component.text(reason, NamedTextColor.BLUE))));

                                                source.sendMessage(Component.text("Successfully kicked " + target.getUsername() + " for \"" + reason + "\"", NamedTextColor.YELLOW));
                                            },
                                            () -> source.sendMessage(Component.text("The player " + context.getArgument("player", String.class) + " could not be found!"))
                                    );
                                    return 1;
                                })
                        )
                )
                .build();

        return new BrigadierCommand(node);
    }
}

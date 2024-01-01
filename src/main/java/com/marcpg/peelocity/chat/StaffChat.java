package com.marcpg.peelocity.chat;

import com.mojang.brigadier.Command;
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

import java.util.Date;

public class StaffChat {
    public static @NotNull BrigadierCommand createStaffChatBrigadier(ProxyServer proxy) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("staff")
                .requires(commandSource -> commandSource.hasPermission("pee.staff"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            if (context.getSource() instanceof Player sender) {
                                String message = context.getArgument("message", String.class);
                                MessageLogging.saveMessage(sender, new MessageLogging.MessageData(new Date(), message, MessageLogging.MessageData.Type.STAFF, null));
                                for (Player player : proxy.getAllPlayers()) {
                                    if (player.hasPermission("pee.staff")) {
                                        player.sendMessage(Component.text("[STAFF] <" + sender.getUsername() + "> " + message, NamedTextColor.BLUE));
                                    }
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }
}

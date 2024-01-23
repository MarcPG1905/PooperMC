package com.marcpg.peelocity.chat;

import com.marcpg.peelocity.Peelocity;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.hectus.lang.Translation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class StaffChat {
    public static @NotNull BrigadierCommand createStaffBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("staff")
                .requires(commandSource -> commandSource.hasPermission("pee.staff"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            String message = context.getArgument("message", String.class);
                            MessageLogging.saveMessage((Player) context.getSource(), new MessageLogging.MessageData(new Date(), message, MessageLogging.MessageData.Type.STAFF, null));
                            for (Player player : Peelocity.SERVER.getAllPlayers()) {
                                if (player.hasPermission("pee.staff")) {
                                    player.sendMessage(Translation.component(player.getEffectiveLocale(), "staff_chat.message", message).color(NamedTextColor.BLUE));
                                }
                            }
                            return 1;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .executes(context -> {
                            context.getSource().sendMessage(Component.text("""
                                    §l§nHelp:§r §l/staff§r
                                    The command /staff will send a message into the private staff chat.
                                    
                                    §l§nArguments:§r
                                    - §lmessage§r: The content of the message to send.
                                    """));
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }
}

package com.marcpg.peelocity.moderation;

import com.marcpg.lang.Translation;
import com.marcpg.peelocity.Peelocity;
import com.marcpg.peelocity.features.MessageHistory;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public final class StaffChat {
    @Contract(" -> new")
    public static @NotNull BrigadierCommand command() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("staff")
                .requires(source -> source instanceof Player && source.hasPermission("pee.staff"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            Player player = (Player) context.getSource();
                            String message = context.getArgument("message", String.class);
                            MessageHistory.saveMessage(player, new MessageHistory.MessageData(new Date(), message, MessageHistory.MessageData.Type.STAFF, null));

                            Peelocity.SERVER.getAllPlayers().parallelStream()
                                    .filter(p -> p.hasPermission("pee.staff"))
                                    .forEach(p -> p.sendMessage(Translation.component(p.getEffectiveLocale(), "staff_chat.message", player.getUsername(), message).color(NamedTextColor.BLUE)));
                            return 1;
                        })
                )
                .build()
        );
    }
}

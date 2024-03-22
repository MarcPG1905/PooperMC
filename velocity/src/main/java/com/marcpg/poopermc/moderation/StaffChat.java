package com.marcpg.poopermc.moderation;

import com.marcpg.libpg.lang.Translation;
import com.marcpg.poopermc.VelocityPooper;
import com.marcpg.poopermc.common.VelocityPlayer;
import com.marcpg.poopermc.entity.OnlinePlayer;
import com.marcpg.poopermc.features.MessageLogging;
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
                .requires(source -> source instanceof Player && source.hasPermission("poo.staff"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            OnlinePlayer<Player> player = VelocityPlayer.ofPlayer((Player) context.getSource());
                            String message = context.getArgument("message", String.class);
                            MessageLogging.saveMessage(player, new MessageLogging.MessageData(new Date(), message, MessageLogging.MessageData.Type.STAFF, null));

                            VelocityPooper.SERVER.getAllPlayers().parallelStream()
                                    .filter(p -> p.hasPermission("poo.staff"))
                                    .forEach(p -> p.sendMessage(Translation.component(p.getEffectiveLocale(), "staff_chat.message", player.name(), message).color(NamedTextColor.BLUE)));
                            return 1;
                        })
                )
                .build()
        );
    }
}

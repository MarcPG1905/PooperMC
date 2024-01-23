package com.marcpg.peelocity.moderation;

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

import java.util.Locale;

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
                                    Player source = (Player) context.getSource();
                                    Peelocity.SERVER.getPlayer(context.getArgument("player", String.class)).ifPresentOrElse(
                                            target -> {
                                                String reason = context.getArgument("reason", String.class);

                                                Locale tl = target.getEffectiveLocale();
                                                target.disconnect(Translation.component(tl, "moderation.kick.msg.title").color(NamedTextColor.GOLD)
                                                        .appendNewline().appendNewline()
                                                        .append(Translation.component(tl, "moderation.reason").color(NamedTextColor.GRAY).append(Component.text(reason, NamedTextColor.BLUE))));

                                                source.sendMessage(Translation.component(source.getEffectiveLocale(), "moderation.kick.confirm", target.getUsername(), reason).color(NamedTextColor.YELLOW));
                                                Peelocity.LOG.info(source.getUsername() + " kicked " + target.getUsername() + " with the reason: \"" + reason + "\"");
                                            },
                                            () -> source.sendMessage(Translation.component(source.getEffectiveLocale(), "cmd.player_not_found", context.getArgument("player", String.class)).color(NamedTextColor.RED))
                                    );
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .executes(context -> {
                            context.getSource().sendMessage(Component.text("""
                                    §l§nHelp:§r §l/kick§r
                                    The command /kick will kick a player off the server, as a very light punishment.
                                    
                                    §l§nArguments:§r
                                    - §lplayer§r: The player to kick off the server.
                                    - §lreason§r: A good reason for kicking the player.
                                    """));
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }
}

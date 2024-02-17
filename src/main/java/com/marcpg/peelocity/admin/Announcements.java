package com.marcpg.peelocity.admin;

import com.marcpg.lang.Translation;
import com.marcpg.peelocity.Peelocity;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

public class Announcements {
    public static final List<String> GROUPS = List.of("everyone", "staff", "moderator", "lobby");
    public static BrigadierCommand createAnnounceBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("announce")
                .requires(source -> source.hasPermission("pee.announcements"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("to", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            GROUPS.forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, Boolean>argument("important", BoolArgumentType.bool())
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("content", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            boolean important = context.getArgument("important", Boolean.class);
                                            String content = context.getArgument("content", String.class);
                                            Peelocity.SERVER.getAllPlayers().parallelStream()
                                                    .filter(player -> switch (context.getArgument("to", String.class)) {
                                                        case "everyone" -> true;
                                                        case "staff" -> player.hasPermission("pee.staff");
                                                        case "moderator" -> player.hasPermission("pee.mod");
                                                        case "lobby" -> player.getCurrentServer().map(serverConnection -> serverConnection.getServerInfo().getName().startsWith("lobby")).orElse(false);
                                                        default -> false;
                                                    })
                                                    .forEach(player -> {
                                                        if (important) player.sendMessage(Component.text("\n\n\n\n\n"));
                                                        player.sendMessage(Translation.component(player.getEffectiveLocale(), "announcement.message", content.strip())
                                                                .color(important ? NamedTextColor.RED : NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
                                                        if (important) player.playSound(Sound.sound(Key.key("minecraft:entity.player.levelup"), Sound.Source.PLAYER, 1.0f, 1.0f));
                                                    });
                                            return 1;
                                        })
                                )
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .executes(context -> {
                            context.getSource().sendMessage(Component.text("""
                                    §l§nHelp:§r §l/announce§r
                                    The command /announce will send an announcement globally on the server.
                                    
                                    §l§nArguments:§r
                                    - §lto§r: To which audience the announcement should be sent.
                                      - everyone: Every single player who's currently online.
                                      - staff: Every member with the pee.staff permission.
                                      - moderator: Every member with the pee.mod permission.
                                      - lobby: Every member who's currently in a lobby and not in some match/game.
                                    - §limportant§r: If the announcement should be displayed more noticeably, to assure that really everyone reads it.
                                    - §lcontent§r: The content of the message. Can contain minecraft formatting using the § symbol.
                                    
                                    §l§nAdditional Info:§r
                                    - Announcements will only be sent to players who are currently online and meet the specified requirements.
                                    """));
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }
}

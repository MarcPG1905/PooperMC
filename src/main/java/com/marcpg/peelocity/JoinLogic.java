package com.marcpg.peelocity;

import com.marcpg.peelocity.social.PartySystem;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.hectus.lang.Translation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JoinLogic {
    public static BrigadierCommand createJoinBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("join")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("gamemode", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Config.GAMEMODES.keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            Player player = (Player) context.getSource();
                            String gamemode = context.getArgument("gamemode", String.class);

                            RegisteredServer server = null;
                            List<Player> players = new ArrayList<>();

                            player.sendMessage(Translation.component(player.getEffectiveLocale(), "cmd.play.search"));

                            if (PartySystem.PLAYER_PARTIES.containsKey(player.getUniqueId())) {
                                Map<UUID, Boolean> party = PartySystem.PARTIES.get(PartySystem.PLAYER_PARTIES.get(player.getUniqueId()));
                                if (party.get(player.getUniqueId())) {
                                    server = findServer(gamemode, Config.GAMEMODES.get(gamemode), party.size());
                                    party.keySet().stream().map(uuid -> Peelocity.SERVER.getPlayer(uuid)).forEach(optionalPartyMember -> optionalPartyMember.ifPresent(players::add));
                                } else {
                                    player.sendMessage(Translation.component(player.getEffectiveLocale(), "cmd.play.not_leader"));
                                }
                            } else {
                                server = findServer(gamemode, Config.GAMEMODES.get(gamemode), 1);
                                players.add(player);
                            }

                            if (server != null) {
                                join(server, players.toArray(Player[]::new));
                            } else {
                                player.sendMessage(Translation.component(player.getEffectiveLocale(), "cmd.play.failure"));
                            }
                            return 1;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .executes(context -> {
                            context.getSource().sendMessage(Component.text("""
                                    §l§nHelp:§r §l/join§r
                                    The command /join will join a specific game mode.
                                    
                                    §l§nArguments:§r
                                    - §lgamemode§r: What game mode to join.
                                    
                                    §l§nAdditional Info:§r
                                    - Will take all party members with you, if you're the leader of a party.
                                    """));
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }

    public static @Nullable RegisteredServer findServer(String serverNamespace, int playerLimit, int players) {
        for (RegisteredServer server : Peelocity.SERVER.getAllServers()) {
            if (server.getServerInfo().getName().startsWith(serverNamespace) && server.getPlayersConnected().size() < (playerLimit - players)) {
                return server;
            }
        }
        return null;
    }

    public static void join(RegisteredServer server, Player @NotNull ... players) {
        for (Player target : players) {
            target.sendMessage(Translation.component(target.getEffectiveLocale(), "cmd.play.success.connect").color(NamedTextColor.YELLOW));
            target.createConnectionRequest(server).fireAndForget();
            target.sendMessage(Translation.component(target.getEffectiveLocale(), "cmd.play.success.finish").color(NamedTextColor.GREEN));
        }
    }
}

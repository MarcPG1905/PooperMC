package com.marcpg.peelocity;

import com.google.common.io.ByteStreams;
import com.marcpg.common.Configuration;
import com.marcpg.libpg.lang.Translation;
import com.marcpg.libpg.storing.Pair;
import com.marcpg.peelocity.social.VelocityPartySystem;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Joining {
    public static final MinecraftChannelIdentifier PLUGIN_MESSAGE_IDENTIFIER = MinecraftChannelIdentifier.from("poopermc:joining");

    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void onPluginMessage(@NotNull PluginMessageEvent event) {
        if (event.getSource() instanceof ServerConnection connection && event.getIdentifier() == PLUGIN_MESSAGE_IDENTIFIER) {
            String gamemode = ByteStreams.newDataInput(event.getData()).readUTF();
            runLogic(connection.getPlayer(), Pair.of(gamemode, Configuration.gamemodes.get(gamemode)));
        }
    }

    public static @NotNull BrigadierCommand joinCommand() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("join")
                .requires(source -> source instanceof Player)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("gamemode", StringArgumentType.greedyString())
                        .suggests((context, builder) -> {
                            Configuration.gamemodes.keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            String gamemode = context.getArgument("gamemode", String.class);
                            runLogic((Player) context.getSource(), Pair.of(gamemode, Configuration.gamemodes.get(gamemode)));
                            return 1;
                        })
                )
                .build()
        );
    }

    public static @NotNull BrigadierCommand hubCommand() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("hub")
                .requires(source -> source instanceof Player)
                .executes(context -> {
                    join(findServer("lobby", 500, 1), (Player) context.getSource());
                    return 1;
                })
                .build()
        );
    }

    // Chaos Code, don't touch, it works:
    private static void runLogic(@NotNull Player player, Pair<String, Integer> gamemode) {
        player.sendMessage(Translation.component(player.getEffectiveLocale(), "cmd.play.search").color(NamedTextColor.GRAY));

        RegisteredServer targetServer;
        List<Player> players = new ArrayList<>();

        if (VelocityPartySystem.PLAYER_PARTIES.containsKey(player.getUniqueId())) {
            Map<UUID, Boolean> party = VelocityPartySystem.PARTIES.get(VelocityPartySystem.PLAYER_PARTIES.get(player.getUniqueId()));

            if (!party.get(player.getUniqueId())) {
                player.sendMessage(Translation.component(player.getEffectiveLocale(), "cmd.play.not_leader").color(NamedTextColor.RED));
                return;
            }
            targetServer = findServer(gamemode.left(), gamemode.right(), party.size());
            party.keySet().stream()
                    .map(uuid -> PeelocityPlugin.SERVER.getPlayer(uuid))
                    .forEach(m -> m.ifPresent(players::add));
        } else {
            targetServer = findServer(gamemode.left(), gamemode.right(), 1);
            players.add(player);
        }

        if (targetServer != null) {
            join(targetServer, players.toArray(Player[]::new));
        } else {
            player.sendMessage(Translation.component(player.getEffectiveLocale(), "cmd.play.failure").color(NamedTextColor.RED));
        }
    }
    // The following code is readable :D

    private static @Nullable RegisteredServer findServer(String serverNamespace, int playerLimit, int players) {
        for (RegisteredServer server : PeelocityPlugin.SERVER.matchServer(serverNamespace)) {
            if (server.getPlayersConnected().size() < (playerLimit - players))
                return server;
        }
        return null;
    }

    private static void join(RegisteredServer server, @NotNull Player... players) {
        for (Player target : players) {
            target.createConnectionRequest(server).fireAndForget();
            PeelocityPlugin.SERVER.getScheduler().buildTask(PeelocityPlugin.INSTANCE, () ->
                    target.sendMessage(Translation.component(target.getEffectiveLocale(), "cmd.play.success.finish").color(NamedTextColor.GREEN))).delay(Duration.ofSeconds(2)).schedule();
        }
    }
}

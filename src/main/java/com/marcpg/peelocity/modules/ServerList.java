package com.marcpg.peelocity.modules;

import com.marcpg.peelocity.Config;
import com.marcpg.util.Randomizer;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import org.jetbrains.annotations.NotNull;

public class ServerList {
    @Subscribe
    public EventTask onProxyPing(ProxyPingEvent event) {
        return EventTask.async(() -> format(event));
    }

    private void format(@NotNull ProxyPingEvent e) {
        ServerPing.Builder ping = e.getPing().asBuilder();
        try {
            // Custom/Random MotDs
            if (Config.SL_MOTD_ENABLED)
                ping.description(Randomizer.fromArray(Config.SL_MOTDS));

            // Custom/Random Favicons (Server Icons)
            if (Config.SL_FAVICON_ENABLED)
                ping.favicon(Randomizer.fromArray(Config.SL_FAVICONS));

            // Fake/Custom Player-Count
            if (Config.SL_SHOW_CURRENT_PLAYERS >= 0)
                ping.onlinePlayers(Config.SL_SHOW_CURRENT_PLAYERS);
            else if (Config.SL_SHOW_CURRENT_PLAYERS == -2)
                ping.nullPlayers();

            // Custom Maximum Players
            if (Config.SL_SHOW_MAX_PLAYERS >= 0)
                ping.maximumPlayers(Config.SL_SHOW_MAX_PLAYERS);
            else if (Config.SL_SHOW_MAX_PLAYERS == -2)
                ping.maximumPlayers(ping.getOnlinePlayers() + 1);
        } finally {
            e.setPing(ping.build());
        }
    }
}

package com.marcpg.peelocity.features;

import com.marcpg.common.Configuration;
import com.marcpg.libpg.util.Randomizer;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import org.jetbrains.annotations.NotNull;

public final class VelocityServerList {
    @Subscribe(order = PostOrder.LATE)
    public @NotNull EventTask onProxyPing(ProxyPingEvent event) {
        return EventTask.async(() -> this.format(event));
    }

    private void format(@NotNull ProxyPingEvent e) {
        ServerPing.Builder ping = e.getPing().asBuilder();
        try {
            // Custom/Random MotDs
            if (Configuration.serverListMotd && !Configuration.serverListMotdList.isEmpty())
                ping.description(Randomizer.fromCollection(Configuration.serverListMotdList));

            // Custom/Random Favicons (Server Icons)
            if (Configuration.serverListFavicon && Configuration.serverListFavicons.hasValues())
                ping.favicon((Favicon) Configuration.serverListFavicons.randomIcon());

            // Fake/Custom Player-Count
            if (Configuration.serverListShowCurrentPlayers >= 0)
                ping.onlinePlayers(Configuration.serverListShowCurrentPlayers);
            else if (Configuration.serverListShowCurrentPlayers == -2)
                ping.nullPlayers();

            // Custom Maximum Players
            if (Configuration.serverListShowMaxPlayers >= 0)
                ping.maximumPlayers(Configuration.serverListShowMaxPlayers);
            else if (Configuration.serverListShowMaxPlayers == -2)
                ping.maximumPlayers(ping.getOnlinePlayers() + 1);
        } finally {
            e.setPing(ping.build());
        }
    }
}

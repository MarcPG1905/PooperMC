package com.marcpg.peelocity.features;

import com.marcpg.libpg.util.Randomizer;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class VelocityServerList {
    public static boolean motd;
    public static List<Component> motdList;
    public static boolean favicon;
    public static List<Favicon> faviconList;
    public static int showCurrentPlayers;
    public static int showMaxPlayers;

    @Subscribe(order = PostOrder.LATE)
    public @NotNull EventTask onProxyPing(ProxyPingEvent event) {
        return EventTask.async(() -> this.format(event));
    }

    private void format(@NotNull ProxyPingEvent e) {
        ServerPing.Builder ping = e.getPing().asBuilder();
        try {
            // Custom/Random MotDs
            if (motd && !motdList.isEmpty())
                ping.description(Randomizer.fromCollection(motdList));

            // Custom/Random Favicons (Server Icons)
            if (favicon && !faviconList.isEmpty())
                ping.favicon(Randomizer.fromCollection(faviconList));

            // Fake/Custom Player-Count
            if (showCurrentPlayers >= 0)
                ping.onlinePlayers(showCurrentPlayers);
            else if (showCurrentPlayers == -2)
                ping.nullPlayers();

            // Custom Maximum Players
            if (showMaxPlayers >= 0)
                ping.maximumPlayers(showMaxPlayers);
            else if (showMaxPlayers == -2)
                ping.maximumPlayers(ping.getOnlinePlayers() + 1);
        } finally {
            e.setPing(ping.build());
        }
    }
}

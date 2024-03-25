package com.marcpg.ink.features;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.marcpg.libpg.util.Randomizer;
import com.marcpg.common.Pooper;
import com.marcpg.common.util.Limiter;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;

import java.util.List;

public class PaperServerList implements Listener {
    public static final Limiter limiter = new Limiter(5);

    public static boolean motd;
    public static List<Component> motdList;
    public static boolean favicon;
    public static List<CachedServerIcon> faviconList;
    public static int showCurrentPlayers;
    public static int showMaxPlayers;

    @EventHandler
    public void onPaperServerListPing(PaperServerListPingEvent event) {
        // Custom/Random MotDs
        if (motd && !motdList.isEmpty())
            event.motd(Randomizer.fromCollection(motdList));

        // Custom/Random Favicons (Server Icons)
        if (favicon && !faviconList.isEmpty())
            event.setServerIcon(Randomizer.fromCollection(faviconList));

        // Fake/Custom Player-Count
        if (showCurrentPlayers >= 0)
            event.setNumPlayers(showCurrentPlayers);
        else if (showCurrentPlayers == -2)
            event.setHidePlayers(true);

        // Custom Maximum Players
        if (showMaxPlayers >= 0)
            event.setMaxPlayers(showMaxPlayers);
        else if (showMaxPlayers == -2)
            event.setMaxPlayers(event.getNumPlayers() + 1);
    }

    @EventHandler
    public void onBukkitServerListPing(ServerListPingEvent event) {
        // Custom/Random MotDs
        if (motd && !motdList.isEmpty())
            event.motd(Randomizer.fromCollection(motdList));

        // Custom/Random Favicons (Server Icons)
        if (favicon && !faviconList.isEmpty())
            event.setServerIcon(Randomizer.fromCollection(faviconList));

        // Fake/Custom Player-Count - The advanced PaperServerListPing even isn't available on Bukkit/Spigot, which
        // means this will only work on server software with native Paper API support, such as Paper itself or Purpur.
        if (showCurrentPlayers != -1 && limiter.incrementAndGet())
            Pooper.LOG.warn("Custom current player counts are not available on Bukkit/Spigot. Use Paper or a fork of Paper to enable this feature!");

        // Custom Maximum Players
        if (showMaxPlayers >= 0)
            event.setMaxPlayers(showMaxPlayers);
        else if (showMaxPlayers == -2)
            event.setMaxPlayers(event.getNumPlayers() + 1);
    }
}

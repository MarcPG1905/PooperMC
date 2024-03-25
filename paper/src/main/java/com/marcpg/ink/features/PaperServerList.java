package com.marcpg.ink.features;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.marcpg.common.Configuration;
import com.marcpg.common.Pooper;
import com.marcpg.common.util.Limiter;
import com.marcpg.libpg.util.Randomizer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;

public class PaperServerList implements Listener {
    public static final Limiter limiter = new Limiter(5);

    @EventHandler
    public void onPaperServerListPing(PaperServerListPingEvent event) {
        // Custom/Random MotDs
        if (Configuration.serverListMotd && !Configuration.serverListMotdList.isEmpty())
            event.motd(Randomizer.fromCollection(Configuration.serverListMotdList));

        // Custom/Random Favicons (Server Icons)
        if (Configuration.serverListFavicon && Configuration.serverListFavicons.hasValues())
            event.setServerIcon((CachedServerIcon) Configuration.serverListFavicons.randomIcon());

        // Fake/Custom Player-Count
        if (Configuration.serverListShowCurrentPlayers >= 0)
            event.setNumPlayers(Configuration.serverListShowCurrentPlayers);
        else if (Configuration.serverListShowCurrentPlayers == -2)
            event.setHidePlayers(true);

        // Custom Maximum Players
        if (Configuration.serverListShowMaxPlayers >= 0)
            event.setMaxPlayers(Configuration.serverListShowMaxPlayers);
        else if (Configuration.serverListShowMaxPlayers == -2)
            event.setMaxPlayers(event.getNumPlayers() + 1);
    }

    @EventHandler
    public void onBukkitServerListPing(ServerListPingEvent event) {
        // Custom/Random MotDs
        if (Configuration.serverListMotd && !Configuration.serverListMotdList.isEmpty())
            event.motd(Randomizer.fromCollection(Configuration.serverListMotdList));

        // Custom/Random Favicons (Server Icons)
        if (Configuration.serverListFavicon && Configuration.serverListFavicons.hasValues())
            event.setServerIcon((CachedServerIcon) Configuration.serverListFavicons.randomIcon());

        // Fake/Custom Player-Count - The advanced PaperServerListPing even isn't available on Bukkit/Spigot, which
        // means this will only work on server software with native Paper API support, such as Paper itself or Purpur.
        if (Configuration.serverListShowCurrentPlayers != -1 && limiter.incrementAndGet())
            Pooper.LOG.warn("Custom current player counts are not available on Bukkit/Spigot. Use Paper or a fork of Paper to enable this feature!");

        // Custom Maximum Players
        if (Configuration.serverListShowMaxPlayers >= 0)
            event.setMaxPlayers(Configuration.serverListShowMaxPlayers);
        else if (Configuration.serverListShowMaxPlayers == -2)
            event.setMaxPlayers(event.getNumPlayers() + 1);
    }
}

package com.marcpg.ink.features;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.marcpg.common.Configuration;
import com.marcpg.libpg.util.Randomizer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.CachedServerIcon;

public final class PaperServerList implements Listener {
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
}

package com.marcpg.ink.common;

import com.marcpg.common.platform.EventManager;
import com.marcpg.ink.InkPlugin;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class PaperEventManager extends EventManager<Listener, InkPlugin> {
    @Override
    public void register(InkPlugin plugin, Listener event) {
        super.register(plugin, event);
        plugin.getServer().getPluginManager().registerEvents(event, plugin);
    }

    @Override
    public void unregister(InkPlugin plugin, Listener event) {
        super.unregister(plugin, event);
        HandlerList.unregisterAll(event);
    }
}

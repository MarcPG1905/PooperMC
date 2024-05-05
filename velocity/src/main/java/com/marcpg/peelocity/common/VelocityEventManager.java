package com.marcpg.peelocity.common;

import com.marcpg.common.platform.EventManager;
import com.marcpg.peelocity.PeelocityPlugin;

public class VelocityEventManager extends EventManager<Object, PeelocityPlugin> {
    @Override
    public void register(PeelocityPlugin plugin, Object event) {
        super.register(plugin, event);
        PeelocityPlugin.SERVER.getEventManager().register(plugin, event);
    }

    @Override
    public void unregister(PeelocityPlugin plugin, Object event) {
        super.unregister(plugin, event);
        PeelocityPlugin.SERVER.getEventManager().unregisterListener(plugin, event);
    }
}

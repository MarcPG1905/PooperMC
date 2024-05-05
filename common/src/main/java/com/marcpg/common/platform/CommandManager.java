package com.marcpg.common.platform;

import java.util.HashMap;
import java.util.Map;

public abstract class CommandManager<C, P> {
    private final Map<String, C> commands = new HashMap<>();

    public void register(P plugin, String name, C command, String... aliases) {
        commands.put(name, command);
    }

    public void unregister(P plugin, String name) {
        commands.remove(name);
    }

    public final void reset(P plugin) {
        commands.keySet().forEach(c -> unregister(plugin, c));
    }
}

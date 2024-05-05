package com.marcpg.common.platform;

import java.util.ArrayList;
import java.util.List;

public abstract class EventManager<E, P> {
    private final List<E> events = new ArrayList<>();

    public void register(P plugin, E event) {
        events.add(event);
    }

    public void unregister(P plugin, E event) {
        events.remove(event);
    }

    public final void reset(P plugin) {
        events.forEach(e -> unregister(plugin, e));
    }
}

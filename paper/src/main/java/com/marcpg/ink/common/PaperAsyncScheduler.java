package com.marcpg.ink.common;

import com.marcpg.common.platform.AsyncScheduler;
import com.marcpg.libpg.data.time.Time;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

public class PaperAsyncScheduler implements AsyncScheduler {
    private final Plugin plugin;
    private final BukkitScheduler scheduler;

    public PaperAsyncScheduler(Plugin plugin, BukkitScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public void schedule(Runnable runnable) {
        scheduler.runTaskAsynchronously(plugin, runnable);
    }

    @Override
    public void delayed(Runnable runnable, @NotNull Time delay) {
        scheduler.runTaskLaterAsynchronously(plugin, runnable, delay.get() * 20);
    }

    @Override
    public void repeating(Runnable runnable, @NotNull Time interval) {
        scheduler.runTaskTimerAsynchronously(plugin, runnable, 0, interval.get() * 20);
    }

    @Override
    public void delayedRepeating(Runnable runnable, @NotNull Time interval, @NotNull Time delay) {
        scheduler.runTaskTimerAsynchronously(plugin, runnable, delay.get() * 20, interval.get() * 20);
    }
}

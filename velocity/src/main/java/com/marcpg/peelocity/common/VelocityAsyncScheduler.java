package com.marcpg.peelocity.common;

import com.marcpg.common.platform.AsyncScheduler;
import com.marcpg.libpg.data.time.Time;
import com.velocitypowered.api.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class VelocityAsyncScheduler implements AsyncScheduler {
    private final Object plugin;
    private final Scheduler scheduler;

    public VelocityAsyncScheduler(Object plugin, Scheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public void schedule(Runnable runnable) {
        scheduler.buildTask(plugin, runnable).schedule();
    }

    @Override
    public void delayed(Runnable runnable, @NotNull Time delay) {
        scheduler.buildTask(plugin, runnable)
                .delay(delay.get(), TimeUnit.SECONDS)
                .schedule();
    }

    @Override
    public void repeating(Runnable runnable, @NotNull Time interval) {
        scheduler.buildTask(plugin, runnable)
                .repeat(interval.get(), TimeUnit.SECONDS)
                .schedule();
    }

    @Override
    public void delayedRepeating(Runnable runnable, @NotNull Time interval, @NotNull Time delay) {
        scheduler.buildTask(plugin, runnable)
                .delay(delay.get(), TimeUnit.SECONDS)
                .repeat(interval.get(), TimeUnit.SECONDS)
                .schedule();
    }
}

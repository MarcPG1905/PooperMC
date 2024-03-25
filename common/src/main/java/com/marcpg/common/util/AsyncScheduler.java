package com.marcpg.common.util;

import com.marcpg.libpg.data.time.Time;

public interface AsyncScheduler {
    void schedule(Runnable runnable);
    void delayed(Runnable runnable, Time delay);
    void repeating(Runnable runnable, Time interval);
    void delayedRepeating(Runnable runnable, Time interval, Time delay);
}

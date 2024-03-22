package com.marcpg.poopermc.util;

/**
 * Just a little object to limit something.
 * Useful for things like warnings in the logs, that should only appear x amount of times.
 */
public class Limiter {
    private final int limit;
    private int counter = 0;

    /**
     * Creates a new {@link Limiter}.
     * @param limit The limit. Can't be changed later on.
     */
    public Limiter(int limit) {
        this.limit = limit;
    }

    /** Adds one to the counter. If it already reached the limit, it will not be incremented any further. */
    public void increment() {
        if (counter < limit) counter++;
    }

    /**
     * First increments the counter, if it's not at the limit already and then gets, whether it's still under the limit or not.
     * @return true if the counter is still under the limit. <br>
     *         false if it already reached the limit.
     * @see #increment()
     * @see #get()
     */
    public boolean incrementAndGet() {
        increment();
        return get();
    }

    /**
     * Gets whether the counter is still under the set limit or not.
     * @return true if the counter is still under the limit. <br>
     *         false if it already reached the limit.
     */
    public boolean get() {
        return counter >= limit;
    }

    /**
     * Gets the current counter, which will never go over the set limit.
     * @return The current counter.
     */
    public int getCounter() {
        return counter;
    }
}

package com.marcpg.common.logger;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import org.jetbrains.annotations.NotNull;

/**
 * A simple abstraction class for different logger implementations, such as
 * {@link java.util.logging.Logger} or {@link org.slf4j.Logger SLF4J}.
 * @param <T> The native logger's type.
 */
public abstract class Logger<T> implements Audience {
    protected final T nativeLogger;

    /**
     * Creates a new logger.
     * @param nativeLogger The native logger to forward all logs to.
     */
    protected Logger(T nativeLogger) {
        this.nativeLogger = nativeLogger;
    }

    /**
     * Gets the native logger, which can be various objects depending on the implementation,
     * such as {@link java.util.logging.Logger JUL} or {@link org.slf4j.Logger SLF4J}.
     * @return The implementation's native logger.
     */
    public T getNativeLogger() { return nativeLogger; }

    /**
     * Logs a message at info level.
     * @param msg The message to log.
     */
    public abstract void info(String msg);

    /**
     * Logs a message at warning level.
     * @param msg The message to log.
     */
    public abstract void warn(String msg);

    /**
     * Logs a message at error/severe level.
     * @param msg The message to log.
     */
    public abstract void error(String msg);

    /**
     * Sends a {@link ComponentLike} message to the logger. This is sent at the {@link #info(String)} info} level.
     * @param message The message to log.
     */
    @Override
    public void sendMessage(@NotNull ComponentLike message) {
        sendMessage(message.asComponent());
    }

    /**
     * Sends a {@link Component} message to the logger. This is sent at the {@link #info(String)} info} level.
     * @param message The message to log.
     */
    @Override
    public void sendMessage(@NotNull Component message) {
        info(ANSIComponentSerializer.ansi().serialize(message));
    }
}

package com.marcpg.common.logger;

public abstract class Logger<T> {
    protected final T nativeLogger;

    public Logger(T nativeLogger) {
        this.nativeLogger = nativeLogger;
    }

    public T getNativeLogger() { return nativeLogger; }

    public abstract void info(String msg);
    public abstract void warn(String msg);
    public abstract void error(String msg);
}

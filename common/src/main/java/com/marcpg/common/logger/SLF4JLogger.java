package com.marcpg.common.logger;

public class SLF4JLogger extends Logger<org.slf4j.Logger> {
    public SLF4JLogger(org.slf4j.Logger logger) { super(logger); }

    @Override public void info(String msg) { nativeLogger.info(msg); }
    @Override public void warn(String msg) { nativeLogger.warn(msg); }
    @Override public void error(String msg) { nativeLogger.error(msg); }
}

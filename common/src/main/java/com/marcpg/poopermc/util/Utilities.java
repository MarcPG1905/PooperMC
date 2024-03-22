package com.marcpg.poopermc.util;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Marker;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Utilities {
    public static @NotNull org.slf4j.Logger toSlf4j(final Logger logger) {
        return new org.slf4j.Logger() {
            @Override
            public String getName() {
                return logger.getName();
            }

            @Override
            public boolean isTraceEnabled() {
                return logger.isLoggable(Level.FINEST);
            }

            @Override
            public void trace(final String msg) {
                logger.finest(msg);
            }

            @Override
            public void trace(final String format, final Object arg) {
                logger.log(Level.FINEST, format, arg);
            }

            @Override
            public void trace(final String format, final Object arg1, final Object arg2) {
                logger.log(Level.FINEST, format, new Object[] { arg1, arg2 });
            }

            @Override
            public void trace(final String format, final Object... arguments) {
                logger.log(Level.FINEST, format, arguments);
            }

            @Override
            public void trace(final String msg, final Throwable t) {
                logger.log(Level.FINEST, msg, t);
            }

            @Override
            public boolean isTraceEnabled(final Marker marker) {
                return isTraceEnabled();
            }

            @Override
            public void trace(final Marker marker, final String msg) {
                trace(msg);
            }

            @Override
            public void trace(final Marker marker, final String format, final Object arg) {
                trace(format, arg);
            }

            @Override
            public void trace(final Marker marker, final String format, final Object arg1, final Object arg2) {
                trace(format, arg1, arg2);
            }

            @Override
            public void trace(final Marker marker, final String format, final Object... argArray) {
                trace(format, argArray);
            }

            @Override
            public void trace(final Marker marker, final String msg, final Throwable t) {
                trace(msg, t);
            }

            @Override
            public boolean isDebugEnabled() {
                return logger.isLoggable(Level.FINE);
            }

            @Override
            public void debug(final String msg) {
                logger.finest(msg);
            }

            @Override
            public void debug(final String format, final Object arg) {
                logger.log(Level.FINE, format, arg);
            }

            @Override
            public void debug(final String format, final Object arg1, final Object arg2) {
                logger.log(Level.FINE, format, new Object[] { arg1, arg2 });
            }

            @Override
            public void debug(final String format, final Object... arguments) {
                logger.log(Level.FINE, format, arguments);
            }

            @Override
            public void debug(final String msg, final Throwable t) {
                logger.log(Level.FINE, msg, t);
            }

            @Override
            public boolean isDebugEnabled(final Marker marker) {
                return isDebugEnabled();
            }

            @Override
            public void debug(final Marker marker, final String msg) {
                debug(msg);
            }

            @Override
            public void debug(final Marker marker, final String format, final Object arg) {
                debug(format, arg);
            }

            @Override
            public void debug(final Marker marker, final String format, final Object arg1, final Object arg2) {
                debug(format, arg1, arg2);
            }

            @Override
            public void debug(final Marker marker, final String format, final Object... argArray) {
                debug(format, argArray);
            }

            @Override
            public void debug(final Marker marker, final String msg, final Throwable t) {
                debug(msg, t);
            }

            @Override
            public boolean isInfoEnabled() {
                return logger.isLoggable(Level.INFO);
            }

            @Override
            public void info(final String msg) {
                logger.finest(msg);
            }

            @Override
            public void info(final String format, final Object arg) {
                logger.log(Level.INFO, format, arg);
            }

            @Override
            public void info(final String format, final Object arg1, final Object arg2) {
                logger.log(Level.INFO, format, new Object[] { arg1, arg2 });
            }

            @Override
            public void info(final String format, final Object... arguments) {
                logger.log(Level.INFO, format, arguments);
            }

            @Override
            public void info(final String msg, final Throwable t) {
                logger.log(Level.INFO, msg, t);
            }

            @Override
            public boolean isInfoEnabled(final Marker marker) {
                return isInfoEnabled();
            }

            @Override
            public void info(final Marker marker, final String msg) {
                info(msg);
            }

            @Override
            public void info(final Marker marker, final String format, final Object arg) {
                info(format, arg);
            }

            @Override
            public void info(final Marker marker, final String format, final Object arg1, final Object arg2) {
                info(format, arg1, arg2);
            }

            @Override
            public void info(final Marker marker, final String format, final Object... argArray) {
                info(format, argArray);
            }

            @Override
            public void info(final Marker marker, final String msg, final Throwable t) {
                info(msg, t);
            }

            @Override
            public boolean isWarnEnabled() {
                return logger.isLoggable(Level.WARNING);
            }

            @Override
            public void warn(final String msg) {
                logger.finest(msg);
            }

            @Override
            public void warn(final String format, final Object arg) {
                logger.log(Level.WARNING, format, arg);
            }

            @Override
            public void warn(final String format, final Object arg1, final Object arg2) {
                logger.log(Level.WARNING, format, new Object[] { arg1, arg2 });
            }

            @Override
            public void warn(final String format, final Object... arguments) {
                logger.log(Level.WARNING, format, arguments);
            }

            @Override
            public void warn(final String msg, final Throwable t) {
                logger.log(Level.WARNING, msg, t);
            }

            @Override
            public boolean isWarnEnabled(final Marker marker) {
                return isWarnEnabled();
            }

            @Override
            public void warn(final Marker marker, final String msg) {
                warn(msg);
            }

            @Override
            public void warn(final Marker marker, final String format, final Object arg) {
                warn(format, arg);
            }

            @Override
            public void warn(final Marker marker, final String format, final Object arg1, final Object arg2) {
                warn(format, arg1, arg2);
            }

            @Override
            public void warn(final Marker marker, final String format, final Object... argArray) {
                warn(format, argArray);
            }

            @Override
            public void warn(final Marker marker, final String msg, final Throwable t) {
                warn(msg, t);
            }

            @Override
            public boolean isErrorEnabled() {
                return logger.isLoggable(Level.SEVERE);
            }

            @Override
            public void error(final String msg) {
                logger.finest(msg);
            }

            @Override
            public void error(final String format, final Object arg) {
                logger.log(Level.SEVERE, format, arg);
            }

            @Override
            public void error(final String format, final Object arg1, final Object arg2) {
                logger.log(Level.SEVERE, format, new Object[] { arg1, arg2 });
            }

            @Override
            public void error(final String format, final Object... arguments) {
                logger.log(Level.FINEST, format, arguments);
            }

            @Override
            public void error(final String msg, final Throwable t) {
                logger.log(Level.SEVERE, msg, t);
            }

            @Override
            public boolean isErrorEnabled(final Marker marker) {
                return isErrorEnabled();
            }

            @Override
            public void error(final Marker marker, final String msg) {
                error(msg);
            }

            @Override
            public void error(final Marker marker, final String format, final Object arg) {
                error(format, arg);
            }

            @Override
            public void error(final Marker marker, final String format, final Object arg1, final Object arg2) {
                error(format, arg1, arg2);
            }

            @Override
            public void error(final Marker marker, final String format, final Object... argArray) {
                error(format, argArray);
            }

            @Override
            public void error(final Marker marker, final String msg, final Throwable t) {
                error(msg, t);
            }
        };
    }
}

package com.marcpg.common.util;

import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.util.Random;

public interface FaviconHandler<T> {
    Random RANDOM = new Random();

    void addIcon(BufferedImage image) throws InvalidSizeException;
    T randomIcon();

    final class InvalidSizeException extends Exception {
        public InvalidSizeException(@NotNull BufferedImage image) {
            super("Found " + image.getWidth() + "x" + image.getHeight() + ". Favicons should be 64x64!");
        }
    }
}

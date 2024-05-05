package com.marcpg.common.platform;

import com.marcpg.libpg.util.Randomizer;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public abstract class FaviconHandler<T> {
    protected final List<T> favicons = new ArrayList<>();

    public abstract void addIcon(BufferedImage image) throws InvalidSizeException;

    public final T randomIcon() {
        return Randomizer.fromCollection(favicons);
    }

    public final boolean hasValues() {
        return !favicons.isEmpty();
    }

    public static final class InvalidSizeException extends Exception {
        public InvalidSizeException(@NotNull BufferedImage image) {
            super("Found " + image.getWidth() + "x" + image.getHeight() + ". Favicons should be 64x64!");
        }
    }
}

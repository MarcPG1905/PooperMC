package com.marcpg.ink.common;

import com.marcpg.common.platform.FaviconHandler;
import org.bukkit.Bukkit;
import org.bukkit.util.CachedServerIcon;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;

public class PaperFaviconHandler extends FaviconHandler<CachedServerIcon> {
    @Override
    public void addIcon(@NotNull BufferedImage image) throws InvalidSizeException {
        try {
            favicons.add(Bukkit.loadServerIcon(image));
        } catch (Exception e) {
            throw new InvalidSizeException(image);
        }
    }
}

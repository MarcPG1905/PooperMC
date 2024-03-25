package com.marcpg.peelocity.common;

import com.marcpg.common.util.FaviconHandler;
import com.velocitypowered.api.util.Favicon;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class VelocityFaviconHandler implements FaviconHandler<Favicon> {
    private final List<Favicon> favicons = new ArrayList<>();

    @Override
    public void addIcon(BufferedImage image) throws InvalidSizeException {
        try {
            favicons.add(Favicon.create(image));
        } catch (IllegalArgumentException e) {
            throw new InvalidSizeException(image);
        }
    }

    @Override
    public Favicon randomIcon() {
        return favicons.get(RANDOM.nextInt(favicons.size()));
    }

    @Override
    public boolean hasValues() {
        return !favicons.isEmpty();
    }
}

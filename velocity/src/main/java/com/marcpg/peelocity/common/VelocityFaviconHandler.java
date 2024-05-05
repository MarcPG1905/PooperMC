package com.marcpg.peelocity.common;

import com.marcpg.common.platform.FaviconHandler;
import com.velocitypowered.api.util.Favicon;

import java.awt.image.BufferedImage;

public class VelocityFaviconHandler extends FaviconHandler<Favicon> {
    @Override
    public void addIcon(BufferedImage image) throws InvalidSizeException {
        try {
            favicons.add(Favicon.create(image));
        } catch (IllegalArgumentException e) {
            throw new InvalidSizeException(image);
        }
    }
}

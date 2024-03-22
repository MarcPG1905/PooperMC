package com.marcpg.poopermc.util;

import com.marcpg.libpg.lang.Translation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Locale;

public class InvalidCommandArgsException extends Exception {
    private final Object[] args;

    /**
     * Creates a new InvalidCommandArgsException with the description's translation key.
     * @param translationKey The description's translation key.
     */
    public InvalidCommandArgsException(String translationKey, String... args) {
        super(translationKey);
        this.args = args;
    }

    /**
     * Converts this exception to a {@link Component}, which is colored red.
     * Requires the {@link Translation}s to be loaded in!
     * @return The converted {@link Component}.
     */
    public Component translatable(Locale locale) {
        return Translation.component(locale, getMessage(), args).color(NamedTextColor.RED);
    }
}

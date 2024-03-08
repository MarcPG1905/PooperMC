package com.marcpg.peelocity;

import com.marcpg.color.Ansi;
import com.marcpg.web.Downloads;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UpdateChecker {
    private static final Path PATH = Peelocity.DATA_DIR.resolve(".latest_version");

    public static void checkUpdates() {
        Peelocity.LOG.info(Ansi.formattedString("Checking for the latest version of Peelocity...", Ansi.DARK_GRAY));

        Version latest = getLatestVersion();
        if (latest.updateIdentifier() > Peelocity.CURRENT_VERSION.updateIdentifier()) {
            Peelocity.LOG.warn("You're " + (latest.updateIdentifier() - Peelocity.CURRENT_VERSION.updateIdentifier()) + " build(s) behind!");
            Peelocity.LOG.warn("Latest version is " + latest.fullName() + ". Update at " + latest.link());
        } else if (latest.updateIdentifier() == Peelocity.CURRENT_VERSION.updateIdentifier()) {
            Peelocity.LOG.info(Ansi.formattedString("You're on the latest Peelocity version!"));
        } else {
            Peelocity.LOG.error("The version you're running is later than the newest version. Please report this bug to a developer!");
        }
    }

    private static @NotNull Version getLatestVersion() {
        try {
            Downloads.simpleDownload(new URI("https://marcpg.com/peelocity/latest_version").toURL(), PATH.toFile());
            Files.setAttribute(PATH, "dos:hidden", true);
            return Version.parseVersion(Files.readString(PATH));
        } catch (IOException | URISyntaxException e) {
            Peelocity.LOG.error("There was an issue while checking for the newest version of Peelocity!");
            return Peelocity.CURRENT_VERSION;
        }
    }

    public record Version(int updateIdentifier, String fullName, String modrinthVersionId) {
        @Contract("_ -> new")
        public static @NotNull Version parseVersion(@NotNull String commaSeparated) {
            String[] parts = commaSeparated.split(", ");
            return new Version(Integer.parseInt(parts[0]), parts[1], parts[2].strip());
        }

        @Contract(pure = true)
        public @NotNull String link() {
            return "https://modrinth.com/plugin/peelocity/version/" + modrinthVersionId;
        }
    }
}

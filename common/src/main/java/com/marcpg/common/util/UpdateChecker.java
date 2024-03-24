package com.marcpg.common.util;

import com.marcpg.libpg.color.Ansi;
import com.marcpg.libpg.web.Downloads;
import com.marcpg.common.Pooper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UpdateChecker {
    private static final Path PATH = Pooper.DATA_DIR.resolve(".latest_version");

    public static void checkUpdates() {
        Pooper.LOG.info(Ansi.gray("Checking for the latest version of PooperMC..."));

        Version latest = getLatestVersion();
        if (latest.updateIdentifier() > Pooper.CURRENT_VERSION.updateIdentifier()) {
            Pooper.LOG.warn("You're " + (latest.updateIdentifier() - Pooper.CURRENT_VERSION.updateIdentifier()) + " build(s) behind!");
            Pooper.LOG.warn("Latest version is " + latest.fullName() + ". Update at " + latest.link());
        } else if (latest.updateIdentifier() == Pooper.CURRENT_VERSION.updateIdentifier()) {
            Pooper.LOG.info(Ansi.formattedString("You're on the latest PooperMC version!"));
        } else {
            Pooper.LOG.error("The version you're running is later than the newest version. Please report this bug to a developer!");
        }
    }

    private static @NotNull Version getLatestVersion() {
        try {
            Downloads.simpleDownload(new URI("https://marcpg.com/pooper/latest_version").toURL(), PATH.toFile());
            Files.setAttribute(PATH, "dos:hidden", true);
            return Version.parseVersion(Files.readString(PATH));
        } catch (IOException | URISyntaxException e) {
            Pooper.LOG.error("There was an issue while checking for the newest version of PooperMC!");
            return Pooper.CURRENT_VERSION;
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
            return "https://modrinth.com/plugin/pooper/version/" + modrinthVersionId;
        }
    }
}

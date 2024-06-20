package com.marcpg.common.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.marcpg.common.Pooper;
import com.marcpg.libpg.color.Ansi;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public final class UpdateChecker {
    public static void checkUpdates() {
        Pooper.LOG.info(Ansi.gray("Checking for the latest version of PooperMC..."));

        List<Version> versions = getVersions();
        Version latest = versions.get(versions.size() - 1);
        if (Pooper.CURRENT_VERSION.equals(latest)) {
            Pooper.LOG.info(Ansi.formattedString("You're on the latest PooperMC version!"));
        } else {
            Pooper.LOG.warn("You're " + (versions.indexOf(latest) - versions.indexOf(Pooper.CURRENT_VERSION)) + " build(s) behind!");
            Pooper.LOG.warn("Latest version is \"" + latest.fullName() + "\". Update at " + latest.link());
        }
    }

    private static @NotNull List<Version> getVersions() {
        try {
            HttpRequest request = HttpRequest.newBuilder(new URI("https://marcpg.com/pooper/ver/all")).GET().build();
            String response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
            return new Gson().fromJson(response, new TypeToken<List<Version>>(){}.getType());
        } catch (Exception e) {
            Pooper.LOG.error("There was an issue while checking for the newest version of PooperMC!");
            return List.of(Pooper.CURRENT_VERSION);
        }
    }

    public record Version(String version, String fullName, String modrinthVersionId) {
        @Contract(pure = true)
        public @NotNull String link() {
            return "https://modrinth.com/plugin/pooper/version/" + modrinthVersionId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            return obj instanceof Version ver && fullName.equals(ver.fullName);
        }
    }
}

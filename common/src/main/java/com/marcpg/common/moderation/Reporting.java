package com.marcpg.common.moderation;

import com.marcpg.libpg.text.Formatter;
import com.marcpg.libpg.web.discord.Embed;
import com.marcpg.libpg.web.discord.Webhook;
import com.marcpg.common.Pooper;
import com.marcpg.common.entity.OfflinePlayer;
import com.marcpg.common.entity.OnlinePlayer;
import com.marcpg.common.util.InvalidCommandArgsException;

import java.awt.*;
import java.io.IOException;
import java.util.List;

public class Reporting {
    public static final List<String> REASONS = List.of("cheats", "spam", "swearing", "exploiting", "other");

    public static void report(OnlinePlayer<?> reporter, OfflinePlayer player, String reason, String info) throws InvalidCommandArgsException {
        if (!REASONS.contains(reason))
            throw new InvalidCommandArgsException("report.invalid_reason", reason);

        if (Pooper.MOD_WEBHOOK != null) {
            try {
                Pooper.MOD_WEBHOOK.post(new Embed("New Report!", null, Color.decode("#FF5555"), List.of(
                        new Embed.Field("Reported User", player.name(), true),
                        new Embed.Field("Who Reported?", reporter.name(), true),
                        new Embed.Field("Reason", Formatter.toPascalCase(reason), true),
                        new Embed.Field("Additional Info", Webhook.escapeJson(info).trim(), false)
                )));
            } catch (IOException e) {
                Pooper.LOG.warn("Couldn't send Discord webhook to " + Pooper.MOD_WEBHOOK.getUrl() +"!");
                throw new InvalidCommandArgsException("report.error");
            }
        } else {
            Pooper.LOG.warn("A player tried using `/report`, which isn't available as there's no valid webhook in the configuration.");
            throw new InvalidCommandArgsException("report.no_webhook");
        }
    }
}

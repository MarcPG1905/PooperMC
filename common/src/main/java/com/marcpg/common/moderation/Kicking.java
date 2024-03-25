package com.marcpg.common.moderation;

import com.marcpg.common.Configuration;
import com.marcpg.libpg.lang.Translation;
import com.marcpg.libpg.web.discord.Embed;
import com.marcpg.libpg.web.discord.Webhook;
import com.marcpg.common.Pooper;
import com.marcpg.common.entity.OnlinePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class Kicking {
    public static void kick(String sourceName, @NotNull OnlinePlayer<?> player, String reason) {
        Locale l = player.locale();
        player.disconnect(Translation.component(l, "moderation.kick.msg.title").color(NamedTextColor.GOLD)
                .appendNewline().appendNewline()
                .append(Translation.component(l, "moderation.reason", "").color(NamedTextColor.GRAY))
                .append(Component.text(reason, NamedTextColor.BLUE)));

        if (Configuration.modWebhook != null) {
            try {
                Configuration.modWebhook.post(new Embed("Minecraft Kick", player.name() + " got kicked by " + sourceName + "!", Color.GREEN, List.of(
                        new Embed.Field("Kicked", player.name(), true),
                        new Embed.Field("Moderator", sourceName, true),
                        new Embed.Field("Reason", Webhook.escapeJson(reason).trim(), false)
                )));
            } catch (IOException e) {
                Pooper.LOG.warn("Couldn't send Discord webhook to " + Configuration.modWebhook.getUrl() +"!");
            }
        }

        Pooper.LOG.info(sourceName + " kicked " + player.name() + " with the reason: \"" + reason + "\"!");
    }
}

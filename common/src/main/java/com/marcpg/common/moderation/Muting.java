package com.marcpg.common.moderation;

import com.marcpg.common.Configuration;
import com.marcpg.common.Pooper;
import com.marcpg.common.entity.OfflinePlayer;
import com.marcpg.common.entity.OnlinePlayer;
import com.marcpg.common.storage.Storage;
import com.marcpg.common.util.InvalidCommandArgsException;
import com.marcpg.libpg.data.time.Time;
import com.marcpg.libpg.lang.Translation;
import com.marcpg.libpg.web.discord.Embed;
import com.marcpg.libpg.web.discord.Webhook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class Muting {
    public static final List<String> TIME_UNITS = List.of("s", "min", "h", "d", "wk", "mo");
    public static final Storage<UUID> STORAGE = Storage.storageType.createStorage("mutes", "player");
    public static final Time MAX_TIME = new Time(1, Time.Unit.YEARS);

    public static void mute(String sourceName, @NotNull OnlinePlayer<?> player, @NotNull Time time, String reason) throws InvalidCommandArgsException {
        mute(sourceName, OfflinePlayer.of(player), time, reason);

        Locale l = player.locale();
        player.sendMessage(Translation.component(l, "moderation.mute.msg.title").color(NamedTextColor.RED)
                .appendNewline()
                .append(Translation.component(l, "moderation.expiration", "").color(NamedTextColor.GRAY)).append(Component.text(time.getOneUnitFormatted(), NamedTextColor.BLUE))
                .appendNewline()
                .append(Translation.component(l, "moderation.reason", "").color(NamedTextColor.GRAY)).append(Component.text(reason, NamedTextColor.BLUE))
        );
    }

    public static void mute(String sourceName, OfflinePlayer player, @NotNull Time time, String reason) throws InvalidCommandArgsException {
        if (time.get() <= 0)
            throw new InvalidCommandArgsException("moderation.time.invalid", time.getPreciselyFormatted());
        if (time.get() > MAX_TIME.get())
            throw new InvalidCommandArgsException("moderation.time.limit", time.getPreciselyFormatted(), MAX_TIME.getOneUnitFormatted());

        if (STORAGE.contains(player.uuid()))
            throw new InvalidCommandArgsException("moderation.mute.already_muted", player.name());

        STORAGE.add(Map.of(
                "player", player.uuid(),
                "expires", System.currentTimeMillis() / 2 + time.get(),
                "duration", time.get(),
                "reason", reason
        ));

        if (Configuration.modWebhook != null) {
            try {
                Configuration.modWebhook.post(new Embed("Minecraft Mute", player.name() + " got muted by " + sourceName + "!", Color.YELLOW, List.of(
                        new Embed.Field("Muted", player.name(), true),
                        new Embed.Field("Moderator", sourceName, true),
                        new Embed.Field("Time", time.getPreciselyFormatted(), true),
                        new Embed.Field("Reason", Webhook.escapeJson(reason).trim(), false)
                )));
            } catch (IOException e) {
                Pooper.LOG.warn("Couldn't send Discord webhook to " + Configuration.modWebhook.getUrl() +"!");
            }
        }

        Pooper.LOG.info(sourceName + " muted " + player.name() + " for " + time.getPreciselyFormatted() + " with the reason: \"" + reason + "\"!");
    }

    public static void unmute(String sourceName, @NotNull OfflinePlayer player) throws InvalidCommandArgsException {
        if (!STORAGE.contains(player.uuid()))
            throw new InvalidCommandArgsException("moderation.unmute.not_muted", player.name());

        STORAGE.remove(player.uuid());

        if (Configuration.modWebhook != null) {
            try {
                Configuration.modWebhook.post(new Embed("Minecraft **Un**mute", player.name() + "'s mute got removed by " + sourceName + ".", Color.YELLOW, List.of(
                        new Embed.Field("Unmuted", player.name(), true),
                        new Embed.Field("Moderator", sourceName, true)
                )));
            } catch (IOException e) {
                Pooper.LOG.warn("Couldn't send Discord webhook to " + Configuration.modWebhook.getUrl() +"!");
            }
        }

        Pooper.LOG.info(sourceName + " unmuted " + player.name() + "!");
    }
}

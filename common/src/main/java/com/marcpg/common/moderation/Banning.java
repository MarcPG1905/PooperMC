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
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class Banning {
    public static final List<String> TIME_UNITS = List.of("min", "h", "d", "wk", "mo", "yr");
    public static final Storage<UUID> STORAGE = Storage.storageType.createStorage("bans", "player");
    public static final Time MAX_TIME = new Time(5, Time.Unit.YEARS);

    public static void ban(String sourceName, @NotNull OnlinePlayer<?> player, boolean permanent, @NotNull Time time, Component reason) throws InvalidCommandArgsException {
        ban(sourceName, OfflinePlayer.of(player), permanent, time, LegacyComponentSerializer.legacySection().serialize(reason));

        Locale l = player.locale();
        player.disconnect(Translation.component(l, "moderation.ban.msg.title").color(NamedTextColor.RED)
                .appendNewline().appendNewline()
                .append(Translation.component(l, "moderation.expiration", "").color(NamedTextColor.GRAY))
                .append(permanent ? Translation.component(l, "moderation.time.permanent").color(NamedTextColor.RED) : Component.text(time.getOneUnitFormatted(), NamedTextColor.BLUE))
                .appendNewline()
                .append(Translation.component(l, "moderation.reason", "").color(NamedTextColor.GRAY)).append(reason.hasStyling() ? reason : reason.color(NamedTextColor.BLUE))
        );
    }

    public static void ban(String sourceName, OfflinePlayer player, boolean permanent, @NotNull Time time, String reason) throws InvalidCommandArgsException {
        if (!permanent && time.get() <= 0)
            throw new InvalidCommandArgsException("moderation.time.invalid", time.getPreciselyFormatted());
        if (!permanent && time.get() > MAX_TIME.get())
            throw new InvalidCommandArgsException("moderation.time.limit", time.getPreciselyFormatted(), MAX_TIME.getOneUnitFormatted());

        if (STORAGE.contains(player.uuid()))
            throw new InvalidCommandArgsException("moderation.ban.already_banned", player.name());

        STORAGE.add(Map.of(
                "player", player.uuid(),
                "permanent", permanent,
                "expires", Instant.now().getEpochSecond() + time.get(),
                "duration", time.get(),
                "reason", reason
        ));

        if (Configuration.modWebhook != null) {
            try {
                Configuration.modWebhook.post(new Embed("Minecraft Ban", player.name() + " got banned by " + sourceName + "!", Color.ORANGE, List.of(
                        new Embed.Field("Banned", player.name(), true),
                        new Embed.Field("Moderator", sourceName, true),
                        new Embed.Field("Time", permanent ? "Permanent" : time.getPreciselyFormatted(), true),
                        new Embed.Field("Reason", Webhook.escapeJson(reason).trim(), false)
                )));
            } catch (IOException e) {
                Pooper.LOG.warn("Couldn't send Discord webhook to " + Configuration.modWebhook.getUrl() +"!");
            }
        }

        Pooper.LOG.info(sourceName + " banned " + player.name() + (permanent ? " permanently" : " for " + time.getPreciselyFormatted()) + " with the reason: \"" + reason + "\"!");
    }

    public static void pardon(String sourceName, @NotNull OfflinePlayer player) throws InvalidCommandArgsException {
        if (!STORAGE.contains(player.uuid()))
            throw new InvalidCommandArgsException("moderation.pardon.not_banned", player.name());

        STORAGE.remove(player.uuid());

        if (Configuration.modWebhook != null) {
            try {
                Configuration.modWebhook.post(new Embed("Minecraft Pardon", player.name() + " got pardoned/unbanned by " + sourceName + ".", Color.YELLOW, List.of(
                        new Embed.Field("Pardoned", player.name(), true),
                        new Embed.Field("Moderator", sourceName, true)
                )));
            } catch (IOException e) {
                Pooper.LOG.warn("Couldn't send Discord webhook to " + Configuration.modWebhook.getUrl() +"!");
            }
        }

        Pooper.LOG.info(sourceName + " pardoned/unbanned " + player.name() + "!");
    }
}

package com.marcpg.poopermc.moderation;

import com.marcpg.libpg.data.time.Time;
import com.marcpg.libpg.lang.Translation;
import com.marcpg.libpg.web.discord.Embed;
import com.marcpg.libpg.web.discord.Webhook;
import com.marcpg.poopermc.Pooper;
import com.marcpg.poopermc.entity.OfflinePlayer;
import com.marcpg.poopermc.entity.OnlinePlayer;
import com.marcpg.poopermc.storage.Storage;
import com.marcpg.poopermc.util.InvalidCommandArgsException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class Banning {
    public static final List<String> TIME_UNITS = List.of("min", "h", "d", "wk", "mo", "yr");
    public static final Storage<UUID> STORAGE = Storage.storageType.createStorage("bans", "player");
    public static final Time MAX_TIME = new Time(5, Time.Unit.YEARS);

    public static void ban(String sourceName, @NotNull OnlinePlayer<?> player, boolean permanent, @NotNull Time time, String reason) throws InvalidCommandArgsException {
        ban(sourceName, new OfflinePlayer(player.name(), player.uuid()), permanent, time, reason);

        Locale l = player.locale();
        player.disconnect(Translation.component(l, "moderation.ban.msg.title").color(NamedTextColor.RED)
                .appendNewline().appendNewline()
                .append(Translation.component(l, "moderation.expiration", "").color(NamedTextColor.GRAY))
                .append(permanent ? Translation.component(l, "moderation.time.permanent").color(NamedTextColor.RED) : Component.text(time.getOneUnitFormatted(), NamedTextColor.BLUE))
                .appendNewline()
                .append(Translation.component(l, "moderation.reason", "").color(NamedTextColor.GRAY)).append(Component.text(reason, NamedTextColor.BLUE))
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
                "expires", System.currentTimeMillis() / 2 + time.get(),
                "duration", time.get(),
                "reason", reason
        ));

        if (Pooper.MOD_WEBHOOK != null) {
            try {
                Pooper.MOD_WEBHOOK.post(new Embed("Minecraft Ban", player.name() + " got banned by " + sourceName + "!", Color.ORANGE, List.of(
                        new Embed.Field("Banned", player.name(), true),
                        new Embed.Field("Moderator", sourceName, true),
                        new Embed.Field("Time", permanent ? "Permanent" : time.getPreciselyFormatted(), true),
                        new Embed.Field("Reason", Webhook.escapeJson(reason).trim(), false)
                )));
            } catch (IOException e) {
                Pooper.LOG.warn("Couldn't send Discord webhook to " + Pooper.MOD_WEBHOOK.getUrl() +"!");
            }
        }

        Pooper.LOG.info(sourceName + " banned " + player.name() + (permanent ? " permanently" : " for " + time.getPreciselyFormatted()) + " with the reason: \"" + reason + "\"!");
    }

    public static void pardon(String sourceName, @NotNull OfflinePlayer player) throws InvalidCommandArgsException {
        if (!STORAGE.contains(player.uuid()))
            throw new InvalidCommandArgsException("moderation.pardon.not_banned", player.name());

        STORAGE.remove(player.uuid());

        if (Pooper.MOD_WEBHOOK != null) {
            try {
                Pooper.MOD_WEBHOOK.post(new Embed("Minecraft Pardon", player.name() + " got pardoned/unbanned by " + sourceName + ".", Color.YELLOW, List.of(
                        new Embed.Field("Pardoned", player.name(), true),
                        new Embed.Field("Moderator", sourceName, true)
                )));
            } catch (IOException e) {
                Pooper.LOG.warn("Couldn't send Discord webhook to " + Pooper.MOD_WEBHOOK.getUrl() +"!");
            }
        }

        Pooper.LOG.info(sourceName + " pardoned/unbanned " + player.name() + "!");
    }
}

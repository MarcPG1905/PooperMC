package com.marcpg.ink.features;

import com.marcpg.common.Configuration;
import com.marcpg.libpg.lang.Translation;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PaperChatUtilities implements Listener {
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    @EventHandler(ignoreCancelled = true)
    public void onAsyncChat(@NotNull AsyncChatEvent event) {
        Player player = event.getPlayer();
        Component message = event.message();

        if (canUse(player, "mentions")) {
            Matcher matcher = MENTION_PATTERN.matcher(PlainComponentSerializer.plain().serialize(message));
            while (matcher.find()) {
                String mentioned = matcher.group(1);

                if (mentioned.equals("everyone") && canUse(player, "mentions.everyone")) {
                    Bukkit.getOnlinePlayers().forEach(p -> {
                        p.showTitle(Title.title(Translation.component(p.locale(), "chat.mentions.title").color(NamedTextColor.BLUE),
                                Translation.component(p.locale(), "chat.mentions.subtitle.everyone", player.getName()).color(NamedTextColor.DARK_GREEN)));
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
                    });
                } else {
                    Player p = Bukkit.getPlayer(mentioned);
                    if (p != null) {
                        p.showTitle(Title.title(Translation.component(p.locale(), "chat.mentions.title").color(NamedTextColor.BLUE),
                                Translation.component(p.locale(), "chat.mentions.subtitle", player.getName()).color(NamedTextColor.DARK_GREEN)));
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
                    } else {
                        player.sendMessage(Translation.component(player.locale(), "cmd.player_not_found", mentioned).color(NamedTextColor.GRAY));
                    }
                }
            }
        }

        if (canUse(player, "colors"))
            event.message(colorize(PlainComponentSerializer.plain().serialize(message)));
    }


    private static boolean canUse(Player player, String chatUtil) {
        return Configuration.chatUtilities.getBoolean(chatUtil + ".enabled") &&
                !Configuration.chatUtilities.getBoolean(chatUtil + ".permission") ||
                player.hasPermission("poo.chat." + chatUtil);
    }

    private static final TagResolver COLORS = TagResolver.resolver(StandardTags.reset(), StandardTags.color());
    private static final TagResolver STYLES = TagResolver.resolver(StandardTags.reset(), StandardTags.color(), StandardTags.decorations());

    private static @NotNull Component colorize(String original) {
        return MiniMessage.builder().tags(Configuration.chatUtilities.getBoolean("colors.styles") ? STYLES : COLORS).build().deserialize(original);
    }
}

package com.marcpg.peelocity.modules;

import com.marcpg.lang.Translation;
import com.marcpg.peelocity.Config;
import com.marcpg.peelocity.Peelocity;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatUtilities {
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChat(@NotNull PlayerChatEvent event) {
        if (!event.getResult().isAllowed()) return;

        Player player = event.getPlayer();
        String message = event.getMessage();

        if (canUse(player, "mentions")) {
            Matcher matcher = MENTION_PATTERN.matcher(message);
            while (matcher.find()) {
                String target = matcher.group(1);
                Optional<ServerConnection> connection = player.getCurrentServer();
                if (target.equals("everyone") && canUse(player, "mentions.everyone")) {
                    Peelocity.SERVER.getAllPlayers().forEach(p -> {
                        if (Config.GLOBAL_CHAT || connection.equals(p.getCurrentServer())) {
                            Locale l = p.getEffectiveLocale();
                            p.showTitle(Title.title(Translation.component(l, "chat.mentions.title").color(NamedTextColor.BLUE), Translation.component(l, "chat.mentions.subtitle.everyone", player.getUsername()).color(NamedTextColor.GREEN)));
                            p.playSound(Sound.sound(Key.key("minecraft:entity.player.levelup"), Sound.Source.PLAYER, 1.0f, 1.0f));
                        }
                    });
                } else {
                    Peelocity.SERVER.getPlayer(target).ifPresentOrElse(p -> {
                        if (Config.GLOBAL_CHAT || connection.equals(p.getCurrentServer())) {
                            Locale l = p.getEffectiveLocale();
                            p.showTitle(Title.title(Translation.component(l, "chat.mentions.title").color(NamedTextColor.BLUE), Translation.component(l, "chat.mentions.subtitle", player.getUsername()).color(NamedTextColor.GREEN)));
                            p.playSound(Sound.sound(Key.key("minecraft:entity.player.levelup"), Sound.Source.PLAYER, 1.0f, 1.0f));
                        } else
                            player.sendMessage(Translation.component(player.getEffectiveLocale(), "cmd.player_not_found", target).color(NamedTextColor.GRAY));
                    }, () -> player.sendMessage(Translation.component(player.getEffectiveLocale(), "cmd.player_not_found", target).color(NamedTextColor.GRAY)));
                }
            }
        }

        event.setResult(PlayerChatEvent.ChatResult.denied());
        Component finalMessage = canUse(player, "colors") ? colorize(message) : Component.text(message);
        if (Config.GLOBAL_CHAT) {
            Peelocity.SERVER.sendMessage(Component.text("<" + player.getUsername() + "> ").append(finalMessage));
        } else {
            player.getCurrentServer().ifPresent(connection -> connection.getServer().sendMessage(Component.text("<" + player.getUsername() + "> " + finalMessage)));
        }
    }

    public static boolean canUse(Player player, String chatUtil) {
        return Config.CHATUTILITY_BOOLEANS.getBoolean(chatUtil + ".enabled") && !Config.CHATUTILITY_BOOLEANS.getBoolean(chatUtil + ".permission") || player.hasPermission("pee.chat." + chatUtil);
    }

    public static final TagResolver COLORS = TagResolver.resolver(StandardTags.reset(), StandardTags.color());
    public static final TagResolver STYLES = TagResolver.resolver(StandardTags.reset(), StandardTags.color(), StandardTags.decorations());

    public static @NotNull Component colorize(String original) {
        return MiniMessage.miniMessage().deserialize(original, Config.CHATUTILITY_BOOLEANS.getBoolean("colors.styles") ? STYLES : COLORS);
    }
}

package com.marcpg.peelocity.modules;

import com.marcpg.lang.Translation;
import com.marcpg.peelocity.Config;
import com.marcpg.peelocity.Peelocity;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatUtilities {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\[(\\w+)=?(\\S+)?]");
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChat(@NotNull PlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // TODO: Replace all placeholders in the message.

        // if (canUse(player, "colors")) {
        //     event.setResult(PlayerChatEvent.ChatResult.denied());
        //     player.getCurrentServer().ifPresent(server -> {
        //         server.getServer().sendMessage(MiniMessage.miniMessage().deserialize(message));
        //         MessageLogging.saveMessage(player, new MessageLogging.MessageData(new Date(), message, MessageLogging.MessageData.Type.NORMAL, null));
        //     });
        // }

        if (canUse(player, "mentions")) {
            Matcher matcher = MENTION_PATTERN.matcher(message);
            while (matcher.find()) {
                String target = matcher.group(1);

                if (target.equals("everyone") && canUse(player, "mentions.everyone")) {
                    Peelocity.SERVER.getAllPlayers().forEach(p -> {
                        Locale l = p.getEffectiveLocale();
                        p.showTitle(Title.title(Translation.component(l, "chat.mentions.title").color(NamedTextColor.BLUE), Translation.component(l, "chat.mentions.subtitle.everyone", player.getUsername()).color(NamedTextColor.GREEN)));
                        p.playSound(Sound.sound(Key.key("minecraft:entity.player.levelup"), Sound.Source.PLAYER, 1.0f, 1.0f));
                    });
                } else {
                    Peelocity.SERVER.getPlayer(target).ifPresentOrElse(
                            p -> {
                                Locale l = p.getEffectiveLocale();
                                p.showTitle(Title.title(Translation.component(l, "chat.mentions.title").color(NamedTextColor.BLUE), Translation.component(l, "chat.mentions.subtitle", player.getUsername()).color(NamedTextColor.GREEN)));
                                p.playSound(Sound.sound(Key.key("minecraft:entity.player.levelup"), Sound.Source.PLAYER, 1.0f, 1.0f));
                            },
                            () -> player.sendMessage(Translation.component(player.getEffectiveLocale(), "cmd.player_not_found", target).color(NamedTextColor.GRAY))
                    );
                }
            }
        }
    }

    public static boolean canUse(Player player, String chatUtil) {
        return Config.CHATUTILITY_BOOLEANS.getBoolean(chatUtil + ".enabled") && !Config.CHATUTILITY_BOOLEANS.getBoolean(chatUtil + ".permission") || player.hasPermission("pee.chat." + chatUtil);
    }
}

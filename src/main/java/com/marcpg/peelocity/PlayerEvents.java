package com.marcpg.peelocity;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.hectus.lang.Translation;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

public class PlayerEvents {
    @Subscribe(order = PostOrder.EARLY)
    public void onLogin(@NotNull LoginEvent event) {
        Player player = event.getPlayer();

        if (Config.WHITELIST && !(Config.WHITELISTED_NAMES.contains(player.getUsername()))) {
            event.setResult(ResultedEvent.ComponentResult.denied(Translation.component(player.getEffectiveLocale(), "server.whitelist").color(NamedTextColor.GOLD)));
            Peelocity.LOG.info("Whitelist: " + player.getUsername() + " kicked, because he isn't whitelisted!");
            return;
        }

        PlayerCache.CACHED_USERS.put(player.getUniqueId(), player.getUsername());
    }
}

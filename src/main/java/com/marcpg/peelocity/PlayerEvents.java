package com.marcpg.peelocity;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.hectus.Translation;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PlayerEvents {
    public static final List<String> ALLOWED_USERS = new ArrayList<>();

    @Subscribe(order = PostOrder.NORMAL)
    public void onLogin(@NotNull LoginEvent event) {
        Player player = event.getPlayer();

        List<UUID> opUuids = Arrays.stream(Peelocity.CONFIG.getProperty("op-uuids").split(", |,")).map(UUID::fromString).toList();

        if (Boolean.parseBoolean(Peelocity.CONFIG.getProperty("closed-testing")) && !(ALLOWED_USERS.contains(player.getUsername()) || opUuids.contains(player.getUniqueId()))) {
            event.setResult(ResultedEvent.ComponentResult.denied(Translation.component(event.getPlayer().getEffectiveLocale(), "server.closed").color(NamedTextColor.GOLD)));
            return;
        }

        if (Boolean.parseBoolean(Peelocity.CONFIG.getProperty("whitelist")) && !(List.of(Peelocity.CONFIG.getProperty("whitelisted-names").split(", |,")).contains(player.getUsername()) || opUuids.contains(player.getUniqueId()))) {
            event.setResult(ResultedEvent.ComponentResult.denied(Translation.component(event.getPlayer().getEffectiveLocale(), "server.whitelist").color(NamedTextColor.GOLD)));
        }
    }
}

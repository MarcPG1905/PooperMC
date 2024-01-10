package com.marcpg.peelocity;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.marcpg.peelocity.Peelocity.CONFIG;

public class PlayerEvents {
    public static final List<String> ALLOWED_USERS = new ArrayList<>();

    @Subscribe(order = PostOrder.NORMAL)
    public void onLogin(@NotNull LoginEvent event) {
        Player player = event.getPlayer();

        List<UUID> opUuids = Arrays.stream(CONFIG.getProperty("op-uuids").split(", |,")).map(UUID::fromString).toList();

        if (Boolean.parseBoolean(CONFIG.getProperty("closed-testing")) && !(ALLOWED_USERS.contains(player.getUsername()) || opUuids.contains(player.getUniqueId()))) {
            event.setResult(ResultedEvent.ComponentResult.denied(Component.text("""
                    This server is currently not available.
                    This is due to closed testing, private-beta or general maintenance.
                    Thanks for your understanding!
                    """, TextColor.color(255, 127, 64))));
            return;
        }

        if (Boolean.parseBoolean(CONFIG.getProperty("whitelist")) && !(List.of(CONFIG.getProperty("whitelisted-names").split(", |,")).contains(player.getUsername()) || opUuids.contains(player.getUniqueId()))) {
            event.setResult(ResultedEvent.ComponentResult.denied(Component.text("""
                    You are not whitelisted on this server!
                    If you believe that this is a bug, please report it to responsible staff!
                    """, TextColor.color(255, 0, 0))));
        }
    }
}

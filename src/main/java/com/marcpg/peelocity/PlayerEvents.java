package com.marcpg.peelocity;

import com.marcpg.peelocity.Config;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerEvents {
    public static final List<String> ALLOWED_USERS = new ArrayList<>();

    @Subscribe(order = PostOrder.FIRST)
    public void onServerJoin(ServerConnectedEvent event) {
        if (Config.OPERATOR_UUIDS.contains(event.getPlayer().getUniqueId())) {
            // TODO: Give player OP
        }

        if (event.getServer().getServerInfo().getName().startsWith("lobby")) {
            event.getPlayer().sendMessage(Component.text( "Welcome, " + event.getPlayer().getUsername()).color(TextColor.color(0, 255, 0)));
            event.getPlayer().sendMessage(Component.text("\nThis server's code was written by MarcPG1905!").color(TextColor.color(160, 160, 160)));
            event.getPlayer().sendMessage(Component.text("===== https://marcpg.com/ =====").color(TextColor.color(160, 160, 160)));
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPreLogin(LoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        boolean op = Config.OPERATOR_UUIDS.contains(uuid);

        if (Config.CLOSED_TESTING && !(ALLOWED_USERS.contains(event.getPlayer().getUsername()) || op)) {
            event.setResult(ResultedEvent.ComponentResult.denied(Component.text("""
                    This server is currently not available.
                    This is due to closed testing, private-beta or general maintenance.
                    Thanks for your understanding!
                    """, TextColor.color(255, 127, 64))));
            return;
        }

        if (Config.WHITELIST && !(Config.WHITELISTED_UUIDS.contains(uuid) || op)) {
            event.setResult(ResultedEvent.ComponentResult.denied(Component.text("""
                    You are not whitelisted on this server!
                    If you believe that this is a bug, please report it to responsible staff!
                    """, TextColor.color(255, 0, 0))));
        }
    }
}

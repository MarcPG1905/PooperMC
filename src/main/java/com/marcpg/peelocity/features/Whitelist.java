package com.marcpg.peelocity.features;

import com.marcpg.lang.Translation;
import com.marcpg.peelocity.Configuration;
import com.marcpg.peelocity.Peelocity;
import com.marcpg.peelocity.PlayerCache;
import com.marcpg.peelocity.storage.Storage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Whitelist {
    public static Storage<String> STORAGE;

    @Subscribe(order = PostOrder.EARLY)
    public void onLogin(@NotNull LoginEvent event) {
        Player player = event.getPlayer();
        if (!STORAGE.contains(player.getUsername())) {
            event.setResult(ResultedEvent.ComponentResult.denied(Translation.component(player.getEffectiveLocale(), "server.whitelist").color(NamedTextColor.GOLD)));
            Peelocity.LOG.info("Whitelist: " + player.getUsername() + " kicked, because he isn't whitelisted!");
        }
    }

    public static @NotNull BrigadierCommand command() {
        if (STORAGE == null)
            STORAGE = Configuration.storageType.createStorage("whitelist", "username");

        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("whitelist")
                .requires(source -> source.hasPermission("pee.admin"))
                .then(LiteralArgumentBuilder.<CommandSource>literal("add")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    PlayerCache.PLAYERS.values().stream()
                                            .filter(s -> !STORAGE.contains(s))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    CommandSource source = context.getSource();
                                    Locale l = source instanceof Player player ? player.getEffectiveLocale() : new Locale("en", "US");
                                    String target = context.getArgument("player", String.class);

                                    if (STORAGE.contains(target)) {
                                        source.sendMessage(Translation.component(l, "server.whitelist.add.already_whitelisted", target).color(NamedTextColor.YELLOW));
                                    } else {
                                        STORAGE.add(Map.of("username", target));
                                        source.sendMessage(Translation.component(l, "server.whitelist.add.confirm", target).color(NamedTextColor.GREEN));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("remove")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    STORAGE.getAll().forEach(m -> builder.suggest(m.keySet().stream().findFirst().orElse("")));
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    CommandSource source = context.getSource();
                                    Locale l = source instanceof Player player ? player.getEffectiveLocale() : new Locale("en", "US");
                                    String target = context.getArgument("player", String.class);

                                    if (STORAGE.contains(target)) {
                                        STORAGE.remove(target);
                                        source.sendMessage(Translation.component(l, "server.whitelist.remove.confirm", target).color(NamedTextColor.GREEN));
                                    } else {
                                        source.sendMessage(Translation.component(l, "server.whitelist.remove.not_whitelisted", target).color(NamedTextColor.YELLOW));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("list")
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            Locale l = source instanceof Player player ? player.getEffectiveLocale() : new Locale("en", "US");

                            List<String> whitelistedNames = STORAGE.getAll().stream().map(m -> m.keySet().stream().findFirst().orElse("")).toList();
                            if (whitelistedNames.isEmpty()) {
                                source.sendMessage(Translation.component(l, "server.whitelist.empty").color(NamedTextColor.YELLOW));
                            } else {
                                source.sendMessage(Translation.component(l, "server.whitelist.list", STORAGE.getAll().size()).color(NamedTextColor.GREEN));
                                whitelistedNames.forEach(p -> source.sendMessage(Component.text("- " + p, NamedTextColor.GRAY)));
                            }
                            return 1;
                        })
                )
                .build()
        );
    }
}

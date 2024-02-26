package com.marcpg.peelocity.modules;

import com.marcpg.lang.Translation;
import com.marcpg.peelocity.Config;
import com.marcpg.peelocity.Peelocity;
import com.marcpg.peelocity.PlayerCache;
import com.marcpg.peelocity.storage.Storage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
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

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Whitelist {
    public static final Storage<String> STORAGE = Config.STORAGE_TYPE.getStorage("whitelist", "name");

    @Subscribe(order = PostOrder.EARLY)
    public void onLogin(@NotNull LoginEvent event) {
        Player player = event.getPlayer();

        if (Config.WHITELIST_ENABLED && !(STORAGE.contains(player.getUsername()))) {
            event.setResult(ResultedEvent.ComponentResult.denied(Translation.component(player.getEffectiveLocale(), "server.whitelist").color(NamedTextColor.GOLD)));
            Peelocity.LOG.info("Whitelist: " + player.getUsername() + " kicked, because he isn't whitelisted!");
            return;
        }

        PlayerCache.CACHED_USERS.put(player.getUniqueId(), player.getUsername());
    }

    public static @NotNull BrigadierCommand createWhitelistBrigadier() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("peeload")
                .requires(source -> source.hasPermission("pee.admin"))
                .then(LiteralArgumentBuilder.<CommandSource>literal("add")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    PlayerCache.CACHED_USERS.values().stream()
                                            .filter(s -> !STORAGE.contains(s))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    CommandSource source = context.getSource();
                                    Locale locale = source instanceof Player player ? player.getEffectiveLocale() : new Locale("en", "US");

                                    if (STORAGE.contains(context.getArgument("player", String.class))) {
                                        source.sendMessage(Translation.component(locale, "server.whitelist.add.already_whitelisted", context.getArgument("player", String.class)).color(NamedTextColor.YELLOW));
                                    } else {
                                        STORAGE.add(Map.of("name", context.getArgument("player", String.class), "filler_value", false));
                                        source.sendMessage(Translation.component(locale, "server.whitelist.add.confirm", context.getArgument("player", String.class)).color(NamedTextColor.GREEN));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("remove")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    STORAGE.get(m -> true).keySet().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    CommandSource source = context.getSource();
                                    Locale locale = source instanceof Player player ? player.getEffectiveLocale() : new Locale("en", "US");

                                    if (STORAGE.contains(context.getArgument("player", String.class))) {
                                        STORAGE.remove(context.getArgument("player", String.class));
                                        source.sendMessage(Translation.component(locale, "server.whitelist.remove.confirm", context.getArgument("player", String.class)).color(NamedTextColor.GREEN));
                                    } else {
                                        source.sendMessage(Translation.component(locale, "server.whitelist.remove.not_whitelisted", context.getArgument("player", String.class)).color(NamedTextColor.YELLOW));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("list")
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            Locale locale = source instanceof Player player ? player.getEffectiveLocale() : new Locale("en", "US");

                            Set<String> whitelistedNames = STORAGE.get(m -> true).keySet();
                            if (whitelistedNames.isEmpty()) {
                                source.sendMessage(Translation.component(locale, "server.whitelist.empty").color(NamedTextColor.YELLOW));
                            } else {
                                source.sendMessage(Translation.component(locale, "server.whitelist.list", STORAGE.get(m -> true).size()).color(NamedTextColor.GREEN));
                                whitelistedNames.forEach(p -> source.sendMessage(Component.text("- " + p, NamedTextColor.GRAY)));
                            }
                            return 1;
                        })
                )
                .build();
        return new BrigadierCommand(node);
    }
}

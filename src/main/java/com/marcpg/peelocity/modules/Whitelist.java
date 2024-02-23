package com.marcpg.peelocity.modules;

import com.marcpg.lang.Translation;
import com.marcpg.peelocity.Config;
import com.marcpg.peelocity.Peelocity;
import com.marcpg.peelocity.PlayerCache;
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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Whitelist {
    public static final Set<String> WHITELISTED_NAMES = new HashSet<>();

    @Subscribe(order = PostOrder.EARLY)
    public void onLogin(@NotNull LoginEvent event) {
        Player player = event.getPlayer();

        if (Config.WHITELIST && !(WHITELISTED_NAMES.contains(player.getUsername()))) {
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
                                            .filter(s -> !WHITELISTED_NAMES.contains(s))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    if (WHITELISTED_NAMES.add(context.getArgument("player", String.class))) {
                                        Config.CONFIG.set("whitelist.names", WHITELISTED_NAMES);
                                        try {
                                            Config.CONFIG.save();
                                        } catch (IOException e) {
                                            context.getSource().sendMessage(Component.text("Couldn't save the whitelist to config!", NamedTextColor.RED));
                                        } finally {
                                            context.getSource().sendMessage(Component.text("Successfully added " + context.getArgument("player", String.class) + " to the whitelist.", NamedTextColor.GREEN));
                                        }
                                    } else {
                                        context.getSource().sendMessage(Component.text("The player " + context.getArgument("player", String.class) + " is already whitelisted!", NamedTextColor.YELLOW));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("remove")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    WHITELISTED_NAMES.forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    if (WHITELISTED_NAMES.remove(context.getArgument("player", String.class))) {
                                        Config.CONFIG.set("whitelist.names", WHITELISTED_NAMES);
                                        try {
                                            Config.CONFIG.save();
                                        } catch (IOException e) {
                                            context.getSource().sendMessage(Component.text("Couldn't save the whitelist to config!", NamedTextColor.RED));
                                        } finally {
                                            context.getSource().sendMessage(Component.text("Successfully removed " + context.getArgument("player", String.class) + " from the whitelist.", NamedTextColor.YELLOW));
                                        }
                                    } else {
                                        context.getSource().sendMessage(Component.text("The player " + context.getArgument("player", String.class) + " isn't whitelisted, so you can't remove him.", NamedTextColor.GOLD));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("list")
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            if (WHITELISTED_NAMES.isEmpty()) {
                                source.sendMessage(Component.text("The whitelist is empty. Add players using '/whitelist add PlayerName'", NamedTextColor.YELLOW));
                            } else {
                                source.sendMessage(Component.text("Showing all " + WHITELISTED_NAMES.size() + " whitelisted players. ", NamedTextColor.GREEN));
                                WHITELISTED_NAMES.forEach(p -> source.sendMessage(Component.text("- " + p, NamedTextColor.GRAY)));
                            }
                            return 1;
                        })
                )
                .build();
        return new BrigadierCommand(node);
    }
}

package com.marcpg.peelocity;

import com.alessiodp.libby.VelocityLibraryManager;
import com.marcpg.common.Configuration;
import com.marcpg.common.Pooper;
import com.marcpg.common.features.MessageLogging;
import com.marcpg.common.optional.PlayerCache;
import com.marcpg.libpg.lang.Translation;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.marcpg.common.Configuration.doc;

public final class Commands {
    public static @NotNull BrigadierCommand peelocityCommand() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("peelocity")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    Locale l = source instanceof Player player ? player.getEffectiveLocale() : Locale.getDefault();
                    source.sendMessage(Component.text("Peelocity ").decorate(TextDecoration.BOLD).append(Component.text(Pooper.VERSION + "+build." + Pooper.BUILD).decoration(TextDecoration.BOLD, false)).color(NamedTextColor.YELLOW));
                    source.sendMessage(Translation.component(l, "license"));
                    return 1;
                })
                .then(LiteralArgumentBuilder.<CommandSource>literal("reload")
                        .requires(source -> source.hasPermission("poo.admin"))
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            Locale l = source instanceof Player player ? player.getEffectiveLocale() : Locale.getDefault();

                            try {
                                Pooper.INSTANCE.loadBasic(Configuration.serverListFavicons, new VelocityLibraryManager<>(PeelocityPlugin.INSTANCE, (Logger) Pooper.LOG.getNativeLogger(), Pooper.DATA_DIR, PeelocityPlugin.SERVER.getPluginManager()));
                                source.sendMessage(Translation.component(l, "cmd.reload.confirm").color(NamedTextColor.GREEN));
                            } catch (Exception e) {
                                source.sendMessage(Translation.component(l, "cmd.reload.error").color(NamedTextColor.RED));
                            }

                            return 1;
                        })
                )
        );
    }

    public static @NotNull BrigadierCommand configCommand() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("config")
                .requires(source -> source.hasPermission("poo.admin"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("entry", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Configuration.routes.forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(LiteralArgumentBuilder.<CommandSource>literal("get")
                                .executes(context -> {
                                    CommandSource source = context.getSource();
                                    Locale l = source instanceof Player player ? player.getEffectiveLocale() : Locale.getDefault();
                                    String route = context.getArgument("entry", String.class);

                                    if (doc.isList(route)) {
                                        source.sendMessage(Translation.component(l, "cmd.config.get.list", route).color(NamedTextColor.YELLOW));
                                        doc.getList(route).forEach(o -> source.sendMessage(Component.text("- " + o.toString())));
                                    } else if (doc.contains(route)) {
                                        source.sendMessage(Translation.component(l, "cmd.config.get.object", route, doc.getString(route)).color(NamedTextColor.YELLOW));
                                    } else {
                                        source.sendMessage(Translation.component(l, "cmd.config.key_not_existing", route).color(NamedTextColor.RED));
                                    }
                                    return 1;
                                })
                        )
                        .then(LiteralArgumentBuilder.<CommandSource>literal("set")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("value", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> {
                                            if (doc.isBoolean(context.getArgument("entry", String.class))) {
                                                builder.suggest("true");
                                                builder.suggest("false");
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            CommandSource source = context.getSource();
                                            Locale l = source instanceof Player player ? player.getEffectiveLocale() : Locale.getDefault();
                                            String route = context.getArgument("entry", String.class);

                                            if (!doc.contains(route)) {
                                                source.sendMessage(Translation.component(l, "cmd.config.key_not_existing", route).color(NamedTextColor.RED));
                                                return 1;
                                            }
                                            if (doc.isSection(route) || doc.isList(route)) {
                                                source.sendMessage(Translation.component(l, "cmd.config.set.section_list").color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            String value = context.getArgument("value", String.class);

                                            if (doc.isBoolean(route)) {
                                                doc.set(route, Boolean.parseBoolean(value));
                                            } else if (doc.isInt(route)) {
                                                doc.set(route, Integer.parseInt(value));
                                            } else {
                                                doc.set(route, value);
                                            }

                                            try {
                                                doc.save();
                                            } catch (IOException e) {
                                                source.sendMessage(Translation.component(l, "cmd.config.error").color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            source.sendMessage(Translation.component(l, "cmd.config.set.confirm", route, value).color(NamedTextColor.YELLOW));
                                            source.sendMessage(Translation.component(l, "cmd.config.reload_to_apply", "peelocity").color(NamedTextColor.GRAY));

                                            return 1;
                                        })
                                )
                        )
                        .then(LiteralArgumentBuilder.<CommandSource>literal("add")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("value", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            CommandSource source = context.getSource();
                                            Locale l = source instanceof Player player ? player.getEffectiveLocale() : Locale.getDefault();
                                            String route = context.getArgument("entry", String.class);

                                            if (!doc.contains(route)) {
                                                source.sendMessage(Translation.component(l, "cmd.config.key_not_existing", route).color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            List<String> list = doc.getStringList(route);
                                            list.add(context.getArgument("value", String.class));
                                            doc.set(route, list);

                                            try {
                                                doc.save();
                                            } catch (IOException e) {
                                                source.sendMessage(Translation.component(l, "cmd.config.error").color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            source.sendMessage(Translation.component(l, "cmd.config.add.confirm", context.getArgument("value", String.class), route).color(NamedTextColor.YELLOW));
                                            source.sendMessage(Translation.component(l, "cmd.config.reload_to_apply", "peelocity").color(NamedTextColor.GRAY));

                                            return 1;
                                        })
                                )
                        )
                        .then(LiteralArgumentBuilder.<CommandSource>literal("remove")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("value", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            CommandSource source = context.getSource();
                                            Locale l = source instanceof Player player ? player.getEffectiveLocale() : Locale.getDefault();
                                            String route = context.getArgument("entry", String.class);
                                            String arg = context.getArgument("value", String.class);

                                            if (!doc.contains(route)) {
                                                source.sendMessage(Translation.component(l, "cmd.config.key_not_existing", route).color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            List<String> list = doc.getStringList(route);
                                            if (!list.contains(arg)) {
                                                source.sendMessage(Translation.component(l, "cmd.config.remove.not_containing", arg, route).color(NamedTextColor.RED));
                                                return 1;
                                            }
                                            list.remove(context.getArgument("value", String.class));
                                            doc.set(route, list);

                                            try {
                                                doc.save();
                                            } catch (IOException e) {
                                                source.sendMessage(Translation.component(l, "cmd.config.error").color(NamedTextColor.RED));
                                                return 1;
                                            }

                                            source.sendMessage(Translation.component(l, "cmd.config.remove.confirm", arg, route).color(NamedTextColor.YELLOW));
                                            source.sendMessage(Translation.component(l, "cmd.config.reload_to_apply", "peelocity").color(NamedTextColor.GRAY));

                                            return 1;
                                        })
                                )
                        )
                )
                .build()
        );
    }

    public static @NotNull BrigadierCommand msgHistCommand() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("msg-hist")
                .requires(source -> source.hasPermission("poo.msg-hist") && source instanceof Player)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            PlayerCache.PLAYERS.values().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            Player player = (Player) context.getSource();
                            Locale l = player.getEffectiveLocale();

                            String target = context.getArgument("player", String.class);
                            UUID uuid = PlayerCache.getUuid(target);

                            if (uuid == null) {
                                player.sendMessage(Translation.component(l, "cmd.player_not_found", target).color(NamedTextColor.RED));
                                return 1;
                            }
                            if (MessageLogging.noHistory(uuid)) {
                                player.sendMessage(Translation.component(l, "moderation.chat_history.no_history", target).color(NamedTextColor.RED));
                                return 1;
                            }

                            player.sendMessage(Translation.component(l, "moderation.chat_history.title", target).color(NamedTextColor.DARK_GREEN));
                            MessageLogging.getHistory(uuid).forEach(msg -> {
                                String time = "[" + DateTimeFormatter.ofPattern("MMMM d, HH:mm").format(ZonedDateTime.ofInstant(msg.time().toInstant(), ZoneId.of("UTC"))) + " UTC] ";
                                String additional = switch (msg.type()) {
                                    case NORMAL -> "";
                                    case STAFF -> Translation.string(l, "moderation.chat_history.staff") + " ";
                                    case PRIVATE -> Translation.string(l, "moderation.chat_history.private", msg.receiver()) + " ";
                                    case PARTY -> Translation.string(l, "moderation.chat_history.party") + " ";
                                };
                                player.sendMessage(Component.text("| " + time + additional, NamedTextColor.GRAY).append(Component.text(msg.content().strip(), NamedTextColor.WHITE)));
                            });
                            player.sendMessage(Component.text("=========================").color(NamedTextColor.DARK_GREEN));

                            return 1;
                        })
                )
                .build());
    }
}

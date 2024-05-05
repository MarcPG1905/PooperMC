package com.marcpg.ink;

import com.alessiodp.libby.BukkitLibraryManager;
import com.marcpg.common.Configuration;
import com.marcpg.common.Pooper;
import com.marcpg.common.features.MessageLogging;
import com.marcpg.libpg.lang.Translation;
import com.marcpg.libpg.text.Completer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.marcpg.common.Configuration.doc;

public class Commands {
    public static class MsgHistCommand implements TabExecutor {
        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length == 1 && sender.hasPermission("poo.msg-hist")) {
                Locale l = sender instanceof Player player ? player.locale() : Locale.getDefault();

                String target = args[0];
                OfflinePlayer targetPlayer = Bukkit.getOfflinePlayerIfCached(target);
                if (targetPlayer == null) {
                    sender.sendMessage(Translation.component(l, "cmd.player_not_found", target).color(NamedTextColor.RED));
                    return true;
                }
                if (MessageLogging.noHistory(targetPlayer.getUniqueId())) {
                    sender.sendMessage(Translation.component(l, "moderation.chat_history.no_history", target).color(NamedTextColor.RED));
                    return true;
                }

                sender.sendMessage(Translation.component(l, "moderation.chat_history.title", target).color(NamedTextColor.DARK_GREEN));
                MessageLogging.getHistory(targetPlayer.getUniqueId()).forEach(msg -> {
                    String time = "[" + DateTimeFormatter.ofPattern("MMMM d, HH:mm").format(ZonedDateTime.ofInstant(msg.time().toInstant(), ZoneId.of("UTC"))) + " UTC] ";
                    String additional = switch (msg.type()) {
                        case NORMAL -> "";
                        case STAFF -> Translation.string(l, "moderation.chat_history.staff") + " ";
                        case PRIVATE -> Translation.string(l, "moderation.chat_history.private", msg.receiver()) + " ";
                        case PARTY -> Translation.string(l, "moderation.chat_history.party") + " ";
                    };
                    sender.sendMessage(Component.text("| " + time + additional, NamedTextColor.GRAY).append(Component.text(msg.content().strip(), NamedTextColor.WHITE)));
                });
                sender.sendMessage(Component.text("=========================").color(NamedTextColor.DARK_GREEN));

                return true;
            }
            return false;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            return args.length != 1 ? List.of() : Completer.semiSmartComplete(args[0], Arrays.stream(Bukkit.getOfflinePlayers())
                    .filter(sender::equals)
                    .map(OfflinePlayer::getName)
                    .toList());
        }
    }

    public static class InkCommand implements TabExecutor {
        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            Locale l = sender instanceof Player player ? player.locale() : Locale.getDefault();
            if (args.length == 0) {
                sender.sendMessage(Translation.component(l, "license"));
                Pooper.sendInfo(sender);
            } else if (args.length == 1 && args[0].equals("reload")) {
                try {
                    Pooper.INSTANCE.unload();
                    Pooper.INSTANCE.loadBasic(Configuration.serverListFavicons, new BukkitLibraryManager(InkPlugin.getPlugin(InkPlugin.class), Pooper.DATA_DIR.toString()));
                    sender.sendMessage(Translation.component(l, "cmd.reload.confirm").color(NamedTextColor.GREEN));
                } catch (IOException e) {
                    sender.sendMessage(Translation.component(l, "cmd.reload.error").color(NamedTextColor.RED));
                }
            } else {
                return false;
            }
            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
            return args.length == 1 ? List.of("reload") : List.of();
        }
    }

    public static class ConfigCommand implements TabExecutor {
        private static final List<String> OPERATIONS = List.of("get", "set", "add", "remove");
        private static final List<String> BOOLEANS = List.of("true", "false");

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length < 2 || !OPERATIONS.contains(args[0])) return false;

            Locale l = sender instanceof Player player ? player.locale() : Locale.getDefault();

            if (args.length == 2 && args[0].equals("get")) {
                if (doc.isList(args[1])) {
                    sender.sendMessage(Translation.component(l, "cmd.config.get.list", args[1]).color(NamedTextColor.YELLOW));
                    doc.getList(args[1]).forEach(o -> sender.sendMessage(Component.text("- " + o.toString())));
                } else if (doc.contains(args[1])) {
                    sender.sendMessage(Translation.component(l, "cmd.config.get.object", args[1], doc.getString(args[1])).color(NamedTextColor.YELLOW));
                } else {
                    sender.sendMessage(Translation.component(l, "cmd.config.key_not_existing", args[1]).color(NamedTextColor.RED));
                }
            } else if (args.length == 3) {
                switch (args[0]) {
                    case "set" -> {
                        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

                        if (!doc.contains(args[1])) {
                            sender.sendMessage(Translation.component(l, "cmd.config.key_not_existing", args[1]).color(NamedTextColor.RED));
                            return true;
                        }
                        if (doc.isSection(args[1]) || doc.isList(args[1])) {
                            sender.sendMessage(Translation.component(l, "cmd.config.set.section_list").color(NamedTextColor.RED));
                            return true;
                        }

                        if (doc.isBoolean(args[1])) {
                            doc.set(args[1], Boolean.parseBoolean(value));
                        } else if (doc.isInt(args[1])) {
                            doc.set(args[1], Integer.parseInt(value));
                        } else {
                            doc.set(args[1], value);
                        }

                        try {
                            doc.save();
                        } catch (IOException e) {
                            sender.sendMessage(Translation.component(l, "cmd.config.error").color(NamedTextColor.RED));
                            return true;
                        }

                        sender.sendMessage(Translation.component(l, "cmd.config.set.confirm", args[1], value).color(NamedTextColor.YELLOW));
                        sender.sendMessage(Translation.component(l, "cmd.config.reload_to_apply", "ink").color(NamedTextColor.GRAY));
                    }
                    case "add" -> {
                        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

                        if (!doc.contains(args[1])) {
                            sender.sendMessage(Translation.component(l, "cmd.config.key_not_existing", args[1]).color(NamedTextColor.RED));
                            return true;
                        }

                        List<String> list = doc.getStringList(args[1]);
                        list.add(value);
                        doc.set(args[1], list);

                        try {
                            doc.save();
                        } catch (IOException e) {
                            sender.sendMessage(Translation.component(l, "cmd.config.error").color(NamedTextColor.RED));
                            return true;
                        }

                        sender.sendMessage(Translation.component(l, "cmd.config.add.confirm", value, args[1]).color(NamedTextColor.YELLOW));
                        sender.sendMessage(Translation.component(l, "cmd.config.reload_to_apply", "ink").color(NamedTextColor.GRAY));
                    }
                    case "remove" -> {
                        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

                        if (!doc.contains(args[1])) {
                            sender.sendMessage(Translation.component(l, "cmd.config.key_not_existing", args[1]).color(NamedTextColor.RED));
                            return true;
                        }

                        List<String> list = doc.getStringList(args[1]);
                        if (!list.contains(value)) {
                            sender.sendMessage(Translation.component(l, "cmd.config.remove.not_containing", value, args[1]).color(NamedTextColor.RED));
                            return true;
                        }
                        list.remove(value);
                        doc.set(args[1], list);

                        try {
                            doc.save();
                        } catch (IOException e) {
                            sender.sendMessage(Translation.component(l, "cmd.config.error").color(NamedTextColor.RED));
                            return true;
                        }

                        sender.sendMessage(Translation.component(l, "cmd.config.remove.confirm", value, args[1]).color(NamedTextColor.YELLOW));
                        sender.sendMessage(Translation.component(l, "cmd.config.reload_to_apply", "ink").color(NamedTextColor.GRAY));
                    }
                    case "get" -> { return false; }
                }
            }
            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
            if (args.length == 1) {
                return Completer.startComplete(args[0], OPERATIONS);
            } else if (args.length == 2) {
                return Completer.containComplete(args[1], Configuration.routes);
            } else if (args.length == 3 && !args[1].equals("get") && doc.isBoolean(args[1])) {
                return BOOLEANS;
            }
            return List.of();
        }
    }
}

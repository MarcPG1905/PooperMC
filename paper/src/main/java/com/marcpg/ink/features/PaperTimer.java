package com.marcpg.ink.features;

import com.marcpg.common.Pooper;
import com.marcpg.common.features.PooperTimer;
import com.marcpg.libpg.data.time.Time;
import com.marcpg.libpg.lang.Translation;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class PaperTimer implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Locale l = Pooper.INSTANCE.getLocale(sender);

        if (args.length == 2 && List.of("stop", "info", "pause", "resume").contains(args[0])) {
            PooperTimer timer = PooperTimer.getTimer(args[1]);
            if (timer == null) {
                sender.sendMessage(Translation.component(l, "timer.not_found").color(NamedTextColor.RED));
                return true;
            }

            switch (args[0]) {
                case "stop" -> timer.stop();
                case "info" -> {
                    sender.sendMessage(Translation.component(l, "timer.info.title", timer.id).decorate(TextDecoration.BOLD));
                    sender.sendMessage(Translation.component(l, "timer.info.paused", timer.isPaused()));
                    sender.sendMessage(Translation.component(l, "timer.info.time_done", timer.getDone()));
                    sender.sendMessage(Translation.component(l, "timer.info.time_left", timer.getLeft()));
                }
                case "pause" -> {
                    if (timer.pause()) {
                        sender.sendMessage(Translation.component(l, "timer.pause.success", timer.id).color(NamedTextColor.YELLOW));
                    } else {
                        sender.sendMessage(Translation.component(l, "timer.pause.already", timer.id).color(NamedTextColor.GOLD));
                    }
                }
                case "resume" -> {
                    if (timer.resume()) {
                        sender.sendMessage(Translation.component(l, "timer.resume.success", timer.id).color(NamedTextColor.GREEN));
                    } else {
                        sender.sendMessage(Translation.component(l, "timer.resume.already", timer.id).color(NamedTextColor.YELLOW));
                    }
                }
            }
            return true;
        }

        if (args.length >= 5 && args[0].equals("start")) {
            Time time = Time.parse(args[2]);
            if (time.get() <= 0) {
                sender.sendMessage(Translation.component(l, "moderation.time.invalid", args[2]));
                return true;
            }

            try {
                new PooperTimer(args[1], time, PooperTimer.Renderer.valueOf(args[3].toUpperCase()), Pooper.INSTANCE.parseAudience(Arrays.copyOfRange(args, 4, args.length), sender)).start();
            } catch (IllegalArgumentException e) {
                sender.sendMessage(Translation.component(l, "cmd.player_not_found", e.getMessage()));
            }
            return true;
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1)
            return List.of("start", "stop", "info", "pause", "resume");

        if (args.length == 2) {
            return switch (args[0]) {
                case "stop", "info" -> PooperTimer.RUNNING_TIMERS.stream().map(t -> t.id).toList();
                case "pause" -> PooperTimer.RUNNING_TIMERS.stream().filter(t -> !t.isPaused()).map(t -> t.id).toList();
                case "resume" -> PooperTimer.RUNNING_TIMERS.stream().filter(PooperTimer::isPaused).map(t -> t.id).toList();
                default -> List.of();
            };
        }

        if (args.length == 3 && args[0].equals("start"))
            return Stream.of("s", "min", "h").map(unit -> args[1].replaceAll("[^-\\d.]+", "") + unit).toList();
        if (args.length == 4 && args[0].equals("start"))
            return Arrays.stream(PooperTimer.Renderer.values()).map(r -> r.name().toLowerCase()).toList();
        if (args.length >= 5 && args[0].equals("start"))
            return Stream.concat(Bukkit.getOnlinePlayers().stream().map(Player::getName), Pooper.SUPPORTED_AUDIENCES.stream()).toList();

        return List.of();
    }
}

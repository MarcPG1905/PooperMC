package com.marcpg.peelocity.features;

import com.marcpg.common.Pooper;
import com.marcpg.common.features.PooperTimer;
import com.marcpg.libpg.data.time.Time;
import com.marcpg.libpg.lang.Translation;
import com.marcpg.peelocity.PeelocityPlugin;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.Locale;

public class VelocityTimer {
    public static BrigadierCommand command() {
        return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("timer")
                .then(LiteralArgumentBuilder.<CommandSource>literal("start")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("id", StringArgumentType.word())
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("time", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            String input = context.getArguments().size() == 2 ? builder.getInput().split(" ")[0] : "";
                                            List.of("s", "min", "h").forEach(string -> builder.suggest(input.replaceAll("[^-\\d.]+", "") + string));
                                            return builder.buildFuture();
                                        })
                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("renderer", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    for (PooperTimer.Renderer r : PooperTimer.Renderer.values()) {
                                                        builder.suggest(r.name().toLowerCase());
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("audience", StringArgumentType.greedyString())
                                                        .suggests((context, builder) -> {
                                                            PeelocityPlugin.SERVER.getAllPlayers().forEach(p -> builder.suggest(p.getUsername()));
                                                            Pooper.SUPPORTED_AUDIENCES.forEach(builder::suggest);
                                                            return builder.buildFuture();
                                                        })
                                                        .executes(context -> {
                                                            String timeArg = context.getArgument("time", String.class);
                                                            Time time = Time.parse(timeArg);
                                                            if (time.get() <= 0) {
                                                                context.getSource().sendMessage(Translation.component(Pooper.INSTANCE.getLocale(context.getSource()), "moderation.time.invalid", timeArg));
                                                                return 1;
                                                            }

                                                            try {
                                                                new PooperTimer(
                                                                        context.getArgument("id", String.class),
                                                                        time,
                                                                        PooperTimer.Renderer.valueOf(context.getArgument("renderer", String.class).toUpperCase()),
                                                                        Pooper.INSTANCE.parseAudience(context.getArgument("audience", String.class).split(" "), context.getSource())
                                                                ).start();
                                                            } catch (IllegalArgumentException e) {
                                                                context.getSource().sendMessage(Translation.component(Pooper.INSTANCE.getLocale(context.getSource()), "cmd.player_not_found", e.getMessage()));
                                                            }
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("stop")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    PooperTimer.RUNNING_TIMERS.forEach(t -> builder.suggest(t.id));
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    PooperTimer timer = PooperTimer.getTimer(context.getArgument("id", String.class));
                                    if (timer == null) {
                                        context.getSource().sendMessage(Translation.component(Pooper.INSTANCE.getLocale(context.getSource()), "timer.not_found").color(NamedTextColor.RED));
                                        return 1;
                                    }
                                    timer.stop();
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("info")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    PooperTimer.RUNNING_TIMERS.forEach(t -> builder.suggest(t.id));
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Locale l = Pooper.INSTANCE.getLocale(context.getSource());

                                    PooperTimer timer = PooperTimer.getTimer(context.getArgument("id", String.class));
                                    if (timer == null) {
                                        context.getSource().sendMessage(Translation.component(l, "timer.not_found").color(NamedTextColor.RED));
                                        return 1;
                                    }

                                    context.getSource().sendMessage(Translation.component(l, "timer.info.title", timer.id).decorate(TextDecoration.BOLD));
                                    context.getSource().sendMessage(Translation.component(l, "timer.info.paused", timer.isPaused()));
                                    context.getSource().sendMessage(Translation.component(l, "timer.info.time_done", timer.getDone()));
                                    context.getSource().sendMessage(Translation.component(l, "timer.info.time_left", timer.getLeft()));

                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("pause")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    PooperTimer.RUNNING_TIMERS.forEach(t -> builder.suggest(t.id));
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Locale l = Pooper.INSTANCE.getLocale(context.getSource());

                                    PooperTimer timer = PooperTimer.getTimer(context.getArgument("id", String.class));
                                    if (timer == null) {
                                        context.getSource().sendMessage(Translation.component(Pooper.INSTANCE.getLocale(context.getSource()), "timer.not_found").color(NamedTextColor.RED));
                                        return 1;
                                    }

                                    if (timer.pause()) {
                                        context.getSource().sendMessage(Translation.component(l, "timer.pause.success", timer.id).color(NamedTextColor.YELLOW));
                                    } else {
                                        context.getSource().sendMessage(Translation.component(l, "timer.pause.already", timer.id).color(NamedTextColor.GOLD));
                                    }

                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("resume")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    PooperTimer.RUNNING_TIMERS.forEach(t -> builder.suggest(t.id));
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Locale l = Pooper.INSTANCE.getLocale(context.getSource());

                                    PooperTimer timer = PooperTimer.getTimer(context.getArgument("id", String.class));
                                    if (timer == null) {
                                        context.getSource().sendMessage(Translation.component(Pooper.INSTANCE.getLocale(context.getSource()), "timer.not_found").color(NamedTextColor.RED));
                                        return 1;
                                    }

                                    if (timer.resume()) {
                                        context.getSource().sendMessage(Translation.component(l, "timer.resume.success", timer.id).color(NamedTextColor.GREEN));
                                    } else {
                                        context.getSource().sendMessage(Translation.component(l, "timer.resume.already", timer.id).color(NamedTextColor.YELLOW));
                                    }

                                    return 1;
                                })
                        )
                )
                .build()
        );
    }
}

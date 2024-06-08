package com.marcpg.common.features;

import com.marcpg.common.Pooper;
import com.marcpg.libpg.data.time.Time;
import com.marcpg.libpg.data.time.Timer;
import com.marcpg.libpg.lang.Translation;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PooperTimer extends Timer {
    public static final Set<PooperTimer> RUNNING_TIMERS = new HashSet<>();
    public static final List<Long> NOTIFICATIONS = List.of(600L, 300L, 180L, 60L, 30L, 10L, 5L, 3L, 2L, 1L, 0L);

    public final String id;
    public final Renderer renderer;
    public final Audience audience;
    protected Object renderingObject;
    protected boolean paused = false;
    protected boolean stopped = false;

    public PooperTimer(String id, Time time, Renderer renderer, Audience audience) {
        super(time);
        this.id = id;
        this.renderer = renderer;
        this.audience = audience;

        if (renderer == Renderer.BOSSBAR) {
            renderingObject = BossBar.bossBar(Component.text(time.getPreciselyFormatted()).decorate(TextDecoration.BOLD), 1.0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
            ((BossBar) renderingObject).addViewer(audience);
        }
    }

    @Override
    public void start() {
        RUNNING_TIMERS.add(this);
        Pooper.SCHEDULER.delayed(() -> {
            if (!paused) {
                timer.decrement();
                render();
            }
            if (timer.get() > 0 && !stopped) {
                start();
            } else {
                RUNNING_TIMERS.remove(this);
            }
        }, new Time(1));
    }

    @Override
    public void stop() {
        stopped = true;
        RUNNING_TIMERS.remove(this);
    }

    @Override
    public boolean pause() {
        if (paused) return false;
        paused = true;
        return true;
    }

    public boolean isPaused() {
        return paused;
    }

    @Override
    public boolean resume() {
        if (!paused) return false;
        paused = false;
        return true;
    }

    public void render() {
        switch (renderer) {
            case ACTIONBAR -> audience.sendActionBar(Component.text(timer.getPreciselyFormatted()));
            case BOSSBAR -> {
                ((BossBar) renderingObject).name(Component.text(timer.getPreciselyFormatted()).decorate(TextDecoration.BOLD));
                ((BossBar) renderingObject).progress((float) timer.get() / initialTime.get());
            }
            case CHAT_REMINDERS -> {
                long t = timer.get();
                long iT = initialTime.get();
                if (t == iT || t == iT / 2 || t == iT / 4 || NOTIFICATIONS.contains(t))
                    audience.forEachAudience(a -> audience.sendMessage(Translation.component(Pooper.INSTANCE.getLocale(a), "timer.chat_reminders", timer.getPreciselyFormatted()).decorate(TextDecoration.BOLD)));
            }
        }
    }

    public static @Nullable PooperTimer getTimer(String id) {
        for (PooperTimer timer : RUNNING_TIMERS) {
            if (timer.id.equals(id)) return timer;
        }
        return null;
    }

    public enum Renderer { ACTIONBAR, BOSSBAR, CHAT_REMINDERS, BACKGROUND }
}

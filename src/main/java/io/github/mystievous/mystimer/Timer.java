package io.github.mystievous.mystimer;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Range;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

public class Timer extends BukkitRunnable implements Listener {

    public static String formatDuration(Duration duration) {
        return String.format("%d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
    }

    public enum State {
        PAUSED,
        RUNNING,
        ENDED
    }

    public static final String START_ID = "start";
    public static final String END_ID = "end";

    private final String id;

    private final Plugin plugin;

    private Duration duration;
    private Duration timeLeft;
    private LocalDateTime endTime;
    private LocalDateTime lastPause;

    private State state;
    private final BossBar bossBar;
    private boolean bossBarShown;

    private final Collection<TimeTrigger> triggers;

    private boolean started;
    private Runnable onStart;
    private Runnable onResume;
    private Runnable onPause;
    private Runnable onEnd;

    public Timer(Plugin plugin, String id, Duration duration) {
        this.plugin = plugin;
        this.id = id;
        LocalDateTime now = LocalDateTime.now();
        this.endTime = now.plus(duration);
        this.lastPause = now;
        this.duration = duration;
        this.timeLeft = this.duration;
        this.state = State.PAUSED;
        this.bossBar = BossBar.bossBar(
                Component.text("Time Left: ")
                        .append(Component.text(formatDuration(timeLeft), NamedTextColor.BLUE)),
                1.0f, BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10);
        this.bossBarShown = false;
        this.triggers = new ArrayList<>();
        this.onEnd = () -> pause(false);
        this.started = false;
        runTaskTimer(plugin, 0, 10);
    }

    public Duration getTimeLeft() {
        return timeLeft;
    }

    public void addTrigger(TimeTrigger trigger) {
        triggers.add(trigger);
    }

    public void setOnStart(Runnable onStart) {
        this.onStart = onStart;
    }

    public void setOnResume(Runnable onResume) {
        this.onResume = onResume;
    }

    public void setOnPause(Runnable onPause) {
        this.onPause = onPause;
    }

    public void setOnEnd(Runnable onEnd) {
        this.onEnd = onEnd;
    }

    public void start(boolean runTrigger) throws TimerUnsetException {
        if (state.equals(State.ENDED)) {
            throw new TimerUnsetException();
        }

        if (!state.equals(State.RUNNING)) {
            LocalDateTime now = LocalDateTime.now();
            endTime = endTime.plus(Duration.between(lastPause, now));
            state = State.RUNNING;
            if (runTrigger) {
                if (onStart != null && !started) {
                    started = true;
                    onStart.run();
                } else if (onResume != null) {
                    onResume.run();
                }
            }
        }
    }

    public void pause(boolean runTrigger) {
        if (state.equals(State.ENDED)) {
            return;
        }

        if (!state.equals(State.PAUSED)) {
            lastPause = LocalDateTime.now();
            state = State.PAUSED;
            if (runTrigger && onPause != null) {
                onPause.run();
            }
        }
    }

    public void setTimeLeft(Duration timeLeft) throws IllegalArgumentException {
        if (timeLeft.compareTo(duration) > 0) {
            throw new IllegalArgumentException("Time is outside the duration of timer.");
        }
        this.timeLeft = timeLeft;
        LocalDateTime now = LocalDateTime.now();
        endTime = now.plus(timeLeft);
        lastPause = now;

        pause(false);

        for (TimeTrigger trigger : triggers) {
            if (trigger.getTime().compareTo(timeLeft) < 0) {
                trigger.reset();
            }
        }

        updateTime();
    }

    public void reset() {
        setTimeLeft(duration);
        started = false;
    }

    public void updateTime() {
        timeLeft = Duration.between(LocalDateTime.now(), endTime);
        Component name = Component.text("Time left: ")
                .append(Component.text(formatDuration(timeLeft), NamedTextColor.BLUE));
        bossBar.name(name);
        float progress = timeLeft.dividedBy(duration);
        bossBar.progress(1-Math.min(Math.max(progress, 0), 1));
    }

    @Override
    public void run() {

        if (state.equals(State.PAUSED) || state.equals(State.ENDED)) {
            return;
        }

        for (TimeTrigger trigger : triggers) {
            trigger.check(this);
        }

        if (state != State.ENDED && timeLeft.compareTo(Duration.ZERO) <= 0) {
            TimeTriggerEvent event = new TimeTriggerEvent(this, END_ID);
            Bukkit.getPluginManager().callEvent(event);
            pause(false);
            state = State.ENDED;
            onEnd.run();
        }

        if (state.equals(State.ENDED) || state.equals(State.PAUSED)) {
            return;
        }

        updateTime();

    }

    public void showBossBar() {
        bossBarShown = true;
        Bukkit.getServer().showBossBar(bossBar);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void hideBossBar() {
        bossBarShown = false;
        Bukkit.getServer().hideBossBar(bossBar);
        HandlerList.unregisterAll(this);
    }

    public void setBossBarName(Component name) {
        bossBar.name(name);
    }

    @EventHandler
    public void onPlayerLogin(PlayerJoinEvent event) {
        if (bossBarShown) {
            event.getPlayer().showBossBar(bossBar);
        }
    }

}

package io.github.mystievous.mystimer;

import io.github.mystievous.mystimer.event.TimerScheduledActionEvent;
import io.github.mystievous.mystimer.event.TimerTimeChangeEvent;
import io.github.mystievous.mystimer.exception.TimerUnsetException;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

public class Timer extends BukkitRunnable implements Listener {

    public static String formatDuration(Duration duration) {
        Duration displayDuration = duration;
        if (duration.compareTo(Duration.ZERO) <= 0) {
            displayDuration = Duration.ZERO;
        }
        return String.format("%02d:%02d:%02d", displayDuration.toHours(), displayDuration.toMinutesPart(), displayDuration.toSecondsPart());
    }

    public enum State {
        PAUSED,
        RUNNING,
        ENDED
    }

    public enum UpdateCause {
        MANUAL_TIME_SET,
        TIMER_RUNNING_UPDATE;
    }

    private final Plugin plugin;

    private Component barMessage;

    private final Duration duration;
    private Duration previousTimeLeft;
    private Duration timeLeft;
    private LocalDateTime endTime;
    private LocalDateTime lastPause;

    private State state;
    private final BossBar bossBar;
    private boolean bossBarShown;

    private final Map<Duration, Collection<ScheduledAction>> scheduledActions;
    private final Collection<ScheduledPausingAction> resumeActions;

    public Timer(Plugin plugin, Duration duration) {
        this.plugin = plugin;
        LocalDateTime now = LocalDateTime.now();
        this.endTime = now.plus(duration);
        this.lastPause = now;
        this.duration = duration;
        this.timeLeft = this.duration;
        this.previousTimeLeft = this.timeLeft;
        this.state = State.PAUSED;
        this.barMessage = Component.text("Time Left: ");
        this.bossBar = BossBar.bossBar(
                barMessage.append(Component.text(formatDuration(timeLeft), NamedTextColor.BLUE)),
                1.0f, BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10);
        this.bossBarShown = false;
        this.scheduledActions = new HashMap<>();
        this.resumeActions = new ArrayList<>();

        registerEndAction(timer -> {
            pauseTimer();
            state = State.ENDED;
        });

        runTaskTimer(plugin, 0, 5);
    }

    public Duration getTimeLeft() {
        return timeLeft;
    }

    public void setBarMessage(Component barMessage) {
        this.barMessage = barMessage;
    }

    public void registerScheduledAction(ScheduledAction scheduledAction) {
        Duration time = scheduledAction.getTime();
        Collection<ScheduledAction> actions = scheduledActions.getOrDefault(time, new ArrayList<>());
        actions.add(scheduledAction);
        scheduledActions.put(time, actions);
    }

    public void registerStartAction(Consumer<Timer> startAction) {
        registerScheduledAction(new ScheduledAction(duration, startAction));
    }

    public void registerEndAction(Consumer<Timer> endAction) {
        registerScheduledAction(new ScheduledAction(Duration.ZERO, endAction));
    }

    public void startTimer() throws TimerUnsetException {
        if (state.equals(State.ENDED)) {
            throw new TimerUnsetException();
        }

        if (!state.equals(State.RUNNING)) {
            LocalDateTime now = LocalDateTime.now();
            endTime = endTime.plus(Duration.between(lastPause, now));
            state = State.RUNNING;
        }
    }

    public void pauseTimer() {
        if (state.equals(State.ENDED)) {
            return;
        }

        if (!state.equals(State.PAUSED)) {
            lastPause = LocalDateTime.now();
            state = State.PAUSED;
        }
    }

    public void setTimeLeft(Duration timeLeft) throws IllegalArgumentException {
        if (timeLeft.compareTo(duration) > 0) {
            throw new IllegalArgumentException("Time is outside the duration of timer.");
        }

        TimerTimeChangeEvent event = new TimerTimeChangeEvent(this, UpdateCause.MANUAL_TIME_SET, this.timeLeft, timeLeft);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        resumeActions.clear();
        this.timeLeft = event.getNewTime();
        this.previousTimeLeft = this.timeLeft;
        LocalDateTime now = LocalDateTime.now();
        endTime = now.plus(event.getNewTime());

        lastPause = now;

        pauseTimer();
        state = State.PAUSED;

        updateTitle();
    }

    public void reset() {
        setTimeLeft(duration);
    }

    private void updateTitle() {
        Component name = barMessage.append(Component.text(formatDuration(timeLeft), NamedTextColor.BLUE));
        setBossBarName(name);
        float progress = (float) timeLeft.getSeconds() / duration.getSeconds();
//        float barProgress = 1 - Math.min(Math.max(progress, 0), 1);
        bossBar.progress(progress);
//        Bukkit.getServer().sendMessage(Component.text(String.format("timeLeft: %s, duration: %s, progress: %.2f, bossbar: %.2f", formatDuration(timeLeft), formatDuration(duration), progress, barProgress)));
    }

    @Override
    public void run() {

        if (state.equals(State.PAUSED) || state.equals(State.ENDED)) {
            return;
        }

        boolean actionDidPause = !resumeActions.isEmpty();

        if (actionDidPause) {
            for (ScheduledPausingAction action : resumeActions) {
                action.runResumeAction(this);
            }
            resumeActions.clear();
        } else {
            List<Duration> actionKeys = scheduledActions.keySet().stream()
                    .filter(actionTime -> actionTime.compareTo(previousTimeLeft) <= 0 && actionTime.compareTo(timeLeft) > 0)
                    .toList();

            for (Duration key : actionKeys) {
                Collection<ScheduledAction> actions = scheduledActions.get(key);
                for (ScheduledAction action : actions) {
                    TimerScheduledActionEvent actionEvent = new TimerScheduledActionEvent(this, action);
                    Bukkit.getPluginManager().callEvent(actionEvent);
                    if (actionEvent.isCancelled()) {
                        continue;
                    }
                    action.runTimeAction(this);
                    if (action instanceof ScheduledPausingAction pausingAction) {
                        resumeActions.add(pausingAction);
                    }
                }
            }
        }


        if (state == State.ENDED || state.equals(State.PAUSED)) {
            return;
        }

        Duration newTimeLeft = Duration.between(LocalDateTime.now(), endTime);

        TimerTimeChangeEvent event = new TimerTimeChangeEvent(this, UpdateCause.TIMER_RUNNING_UPDATE, this.timeLeft, newTimeLeft);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        newTimeLeft = event.getNewTime();

        previousTimeLeft = timeLeft;
        timeLeft = newTimeLeft;

        updateTitle();

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

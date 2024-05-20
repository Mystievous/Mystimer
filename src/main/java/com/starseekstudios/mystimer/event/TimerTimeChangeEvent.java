package com.starseekstudios.mystimer.event;

import com.starseekstudios.mystimer.Timer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class TimerTimeChangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    private final Timer timer;
    private final Timer.UpdateCause cause;
    private final Duration oldTime;
    private Duration newTime;
    private boolean cancelled;

    public TimerTimeChangeEvent(Timer timer, Timer.UpdateCause cause, Duration oldTime, Duration newTime) {
        this.timer = timer;
        this.cause = cause;
        this.oldTime = oldTime;
        this.newTime = newTime;
        this.cancelled = false;
    }

    public Timer getTimer() {
        return timer;
    }

    public Timer.UpdateCause getCause() {
        return cause;
    }

    public Duration getOldTime() {
        return oldTime;
    }

    public Duration getNewTime() {
        return newTime;
    }

    public void setNewTime(Duration newTime) {
        this.newTime = newTime;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
